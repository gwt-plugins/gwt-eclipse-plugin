package com.google.appengine.eclipse.wtp.server;

import com.google.appengine.eclipse.wtp.AppEnginePlugin;
import com.google.common.collect.Lists;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.jst.j2ee.internal.J2EEConstants;
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
@SuppressWarnings("restriction")
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
    List<IStatus> statusList = Lists.newArrayList();
    if (module.length == 1) {
      // root module
      publishDir(module[0], statusList, monitor);
    } else {
      // child module, given as root-child pair
      Properties moduleUrls = server.loadModulePublishLocations();
      // try to determine the URI for the child module
      IWebModule webModule = (IWebModule) module[0].loadAdapter(IWebModule.class, monitor);
      String childURI = null;
      if (webModule != null) {
        childURI = webModule.getURI(module[1]);
      }
      // get child
      IJ2EEModule childModule = (IJ2EEModule) module[1].loadAdapter(IJ2EEModule.class, monitor);
      if (childModule != null && childModule.isBinary()) {
        publishArchiveModule(childURI, moduleUrls, statusList, monitor);
      } else {
        publishJar(childURI, moduleUrls, statusList, monitor);
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
  private void publishArchiveModule(String jarURI, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor) {
    IPath path = server.getModuleDeployDirectory(module[0]);
    boolean isMoving = false;
    // check older publish
    String oldURI = (String) mapping.get(module[1].getId());
    if (oldURI != null && jarURI != null) {
      isMoving = !oldURI.equals(jarURI);
    }
    // create uri if needed
    if (jarURI == null) {
      jarURI = J2EEConstants.WEB_INF_LIB + "/" + module[1].getName();
    }
    // setup target
    IPath jarPath = path.append(jarURI);
    IPath deployPath = jarPath.removeLastSegments(1);
    // remove if requested or if previously published and are now serving without publishing
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.append(oldURI).toFile();
      if (file.exists()) {
        file.delete();
      }
      mapping.remove(module[1].getId());

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
    mapping.put(module[1].getId(), jarURI);
  }

  /**
   * Publish module as directory.
   */
  private void publishDir(IModule publishModule, List<IStatus> statusList, IProgressMonitor monitor)
      throws CoreException {
    IPath path = server.getModuleDeployDirectory(publishModule);
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
   * Publish module by zipping it into JAR file.
   */
  private void publishJar(String jarURI, Properties mapping, List<IStatus> statusList,
      IProgressMonitor monitor) throws CoreException {
    IPath path = server.getModuleDeployDirectory(module[0]);
    boolean isMoving = false;
    // check older publish
    String oldURI = (String) mapping.get(module[1].getId());
    if (oldURI != null && jarURI != null) {
      isMoving = !oldURI.equals(jarURI);
    }
    // create uri if needed
    if (jarURI == null) {
      jarURI = J2EEConstants.WEB_INF_LIB + "/" + module[1].getName() + ".jar";
    }
    // setup target
    IPath jarPath = path.append(jarURI);
    IPath deployDirectory = jarPath.removeLastSegments(1);
    // remove if needed
    if (isMoving || kind == IServer.PUBLISH_CLEAN || deltaKind == ServerBehaviourDelegate.REMOVED) {
      File file = path.append(oldURI).toFile();
      if (file.exists()) {
        file.delete();
      }
      mapping.remove(module[1].getId());
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
    mapping.put(module[1].getId(), jarURI);
  }
}
