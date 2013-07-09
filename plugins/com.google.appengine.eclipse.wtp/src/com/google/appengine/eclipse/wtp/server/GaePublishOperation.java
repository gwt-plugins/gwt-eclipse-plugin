package com.google.appengine.eclipse.wtp.server;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.appengine.eclipse.wtp.utils.ProjectUtils;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.StatusUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jst.server.core.IJ2EEModule;
import org.eclipse.jst.server.core.IWebModule;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.IModuleResource;
import org.eclipse.wst.server.core.model.IModuleResourceDelta;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.ServerBehaviourDelegate;
import org.eclipse.wst.server.core.util.PublishHelper;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * GAE publish helper.
 */
public final class GaePublishOperation extends PublishOperation {
  /**
   * Throws new {@link CoreException} if status list is not empty.
   */
  private static void checkStatuses(List<IStatus> statusList) throws CoreException {
    if (statusList == null || statusList.size() == 0) {
      return;
    }
    if (statusList.size() == 1) {
      throw new CoreException(statusList.get(0));
    }
    IStatus[] children = statusList.toArray(new IStatus[statusList.size()]);
    throw new CoreException(new MultiStatus(AppEnginePlugin.PLUGIN_ID, 0, children,
        "Error during publish operation", null));
  }

  private GaeServerBehaviour server;
  private IModule[] module;
  private int kind;
  private int deltaKind;
  private PublishHelper helper;

  /**
   * Construct the operation object to publish the specified module(s) to the specified server.
   */
  public GaePublishOperation(GaeServerBehaviour server, int kind, IModule[] module, int deltaKind) {
    super("Publish to server", "Publish a module to App Engine Server");
    this.server = server;
    this.module = module;
    this.kind = kind;
    this.deltaKind = deltaKind;
    IPath base = server.getRuntimeBaseDirectory();
    helper = new PublishHelper(base.toFile());
  }

  @Override
  public void execute(IProgressMonitor monitor, IAdaptable info) throws CoreException {
    // TODO: use more advanced key to store module publish locations? Because a dependent java
    // project (added as child module and published as jar) cannot present in more than one parent
    // module.
    List<IStatus> statusList = Lists.newArrayList();
    IPath deployPath = server.getModuleDeployDirectory(module[0]);
    if (module.length == 1) {
      // root module
      publishDir(deployPath, statusList, monitor);
    } else {
      for (int i = 0; i < module.length - 1; i++) {
        IWebModule webModule = (IWebModule) module[i].loadAdapter(IWebModule.class, monitor);
        if (webModule == null) {
          statusList.add(StatusUtilities.newErrorStatus("Not a Web module: " + module[i].getName(),
              AppEnginePlugin.PLUGIN_ID));
          return;
        }
        String uri = webModule.getURI(module[i + 1]);
        if (uri != null) {
          deployPath = deployPath.append(uri);
        } else {
          // no uri is OK for removed modules
          if (deltaKind != ServerBehaviourDelegate.REMOVED) {
            statusList.add(StatusUtilities.newErrorStatus("Cannot get URI for module: "
                + module[i + 1].getName(), AppEnginePlugin.PLUGIN_ID));
            return;
          }
        }
      }
      // modules given as parent-child chain
      // get last one, the prior modules should already be published
      IModule childModule = module[module.length - 1];
      Properties moduleUrls = server.loadModulePublishLocations();
      // get as j2ee
      IJ2EEModule childJ2EEModule = (IJ2EEModule) childModule.loadAdapter(IJ2EEModule.class,
          monitor);
      if (childJ2EEModule != null && childJ2EEModule.isBinary()) {
        publishArchiveModule(deployPath, moduleUrls, statusList, monitor, childModule);
      } else {
        if (ProjectUtils.isGaeProject(childModule.getProject())) {
          // GAE WTP projects should be published as a directory
          publishDir(deployPath, moduleUrls, statusList, monitor, childModule);
        } else {
          publishJar(deployPath, moduleUrls, statusList, monitor, childModule);
        }
      }
      server.saveModulePublishLocations(moduleUrls);
    }
    checkStatuses(statusList);
    server.setModulePublishState2(module, IServer.PUBLISH_STATE_NONE);
  }

  @Override
  public int getKind() {
    return REQUIRED;
  }

  @Override
  public int getOrder() {
    return 0;
  }

  /**
   * Publish as binary module.
   */
  private void publishArchiveModule(IPath path, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor, IModule childModule) {
    boolean isMoving = false;
    // check older publish
    String oldURI = (String) mapping.get(childModule.getId());
    String jarURI = path.toOSString();
    if (oldURI != null && jarURI != null) {
      isMoving = !oldURI.equals(jarURI);
    }
    // setup target
    IPath jarPath = (IPath) path.clone();
    IPath deployPath = jarPath.removeLastSegments(1);
    // remove if requested or if previously published and are now serving without publishing
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      if (oldURI != null) {
        File file = new File(oldURI);
        if (file.exists()) {
          file.delete();
        }
      }
      mapping.remove(childModule.getId());
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // check for changes
    if (!isMoving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);
      if (delta == null || delta.length == 0) {
        return;
      }
    }
    // ensure target directory
    if (!deployPath.toFile().exists()) {
      deployPath.toFile().mkdirs();
    }
    // do publish
    IModuleResource[] resources = server.getResources(module);
    IStatus[] publishStatus = helper.publishToPath(resources, jarPath, monitor);
    statusList.addAll(Arrays.asList(publishStatus));
    // store into mapping
    mapping.put(childModule.getId(), jarURI);
  }

  /**
   * Publish module as directory.
   */
  private void publishDir(IPath path, List<IStatus> statusList, IProgressMonitor monitor)
      throws CoreException {
    // delete if needed
    if (kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.toFile();
      if (file.exists()) {
        IStatus[] status = PublishHelper.deleteDirectory(file, monitor);
        statusList.addAll(Arrays.asList(status));
      }
      // request for remove
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // republish or publish fully
    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] resources = server.getResources(module);
      IStatus[] publishStatus = helper.publishFull(resources, path, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
      return;
    }
    // publish changes only
    IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(module);
    for (IModuleResourceDelta delta : deltas) {
      IStatus[] publishStatus = helper.publishDelta(delta, path, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
    }
  }

  /**
   * Publish child module as directory if not binary.
   */
  private void publishDir(IPath path, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor, IModule childModule) throws CoreException {
    boolean isMoving = false;
    // check older publish
    String oldURI = (String) mapping.get(childModule.getId());
    String dirURI = path.toOSString();
    if (oldURI != null && dirURI != null) {
      isMoving = !oldURI.equals(dirURI);
    }
    // setup target
    IPath dirPath = (IPath) path.clone();
    // remove if needed
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      if (oldURI != null) {
        File file = new File(oldURI);
        if (file.exists()) {
          IStatus[] status = PublishHelper.deleteDirectory(file, monitor);
          statusList.addAll(Arrays.asList(status));
        }
      }
      mapping.remove(childModule.getId());
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // check for changes
    if (!isMoving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);
      if (delta == null || delta.length == 0) {
        return;
      }
    }
    // ensure directory exists
    if (!dirPath.toFile().exists()) {
      dirPath.toFile().mkdirs();
    }
    // do publish resources
    // republish or publish fully
    if (kind == IServer.PUBLISH_CLEAN || kind == IServer.PUBLISH_FULL) {
      IModuleResource[] resources = server.getResources(module);
      IStatus[] publishStatus = helper.publishFull(resources, dirPath, monitor);
      statusList.addAll(Arrays.asList(publishStatus));
    } else {
      // publish changes only
      IModuleResourceDelta[] deltas = server.getPublishedResourceDelta(module);
      for (IModuleResourceDelta delta : deltas) {
        IStatus[] publishStatus = helper.publishDelta(delta, dirPath, monitor);
        statusList.addAll(Arrays.asList(publishStatus));
      }
    }
    // store into mapping
    mapping.put(childModule.getId(), dirURI);
  }

  /**
   * Publish module by zipping it into JAR file.
   */
  private void publishJar(IPath path, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor, IModule childModule) throws CoreException {
    boolean isMoving = false;
    // check older publish
    String oldURI = (String) mapping.get(childModule.getId());
    String jarURI = path.toOSString();
    if (oldURI != null && jarURI != null) {
      isMoving = !oldURI.equals(jarURI);
    }
    // setup target
    IPath jarPath = (IPath) path.clone();
    IPath deployDirectory = jarPath.removeLastSegments(1);
    // remove if needed
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      if (oldURI != null) {
        File file = new File(oldURI);
        if (file.exists()) {
          file.delete();
        }
      }
      mapping.remove(childModule.getId());
      if (deltaKind == ServerBehaviourDelegate.REMOVED) {
        return;
      }
    }
    // check for changes
    if (!isMoving && kind != IServer.PUBLISH_CLEAN && kind != IServer.PUBLISH_FULL) {
      IModuleResourceDelta[] delta = server.getPublishedResourceDelta(module);
      if (delta == null || delta.length == 0) {
        return;
      }
    }
    // ensure directory exists
    if (!deployDirectory.toFile().exists()) {
      deployDirectory.toFile().mkdirs();
    }
    // zip resources
    IModuleResource[] resources = server.getResources(module);
    IStatus[] status = helper.publishZip(resources, jarPath, monitor);
    statusList.addAll(Arrays.asList(status));
    // store into mapping
    mapping.put(childModule.getId(), jarURI);
  }
}
