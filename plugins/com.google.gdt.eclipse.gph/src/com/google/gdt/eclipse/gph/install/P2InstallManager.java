/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.google.gdt.eclipse.gph.install;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfile;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.query.IQuery;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.ServiceReference;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A manager used to install required new software into Eclipse.
 */
public class P2InstallManager {
  private static final String FEATURE_GROUP = ".feature.group";

  private IInstallableUnit[] computedInstallableUnits;
  private InstallOperation installOperation;
  private List<P2InstallationUnit> installationUnits;
  private final ProvisioningUI provisioningUI;
  private Set<URI> repositoryLocations;

  /**
   * Creates a new P2InstallManager instance.
   */
  public P2InstallManager(List<P2InstallationUnit> installationUnits) {
    this.provisioningUI = ProvisioningUI.getDefaultUI();
    this.installationUnits = new ArrayList<>(installationUnits);
    if (installationUnits == null || installationUnits.isEmpty()) {
      throw new IllegalArgumentException("Must pass a non-empty list of installation units.");
    }
  }

  /**
   * Determines whether the desired features are installed locally. Invoke this method before
   * invoking either {@link #resolveP2Information(IProgressMonitor)} or {@link #runP2Install()}..
   */
  public List<P2InstallationFeature> resolveInstalledStatus() {
    List<P2InstallationFeature> results = new ArrayList<P2InstallationFeature>();

    for (P2InstallationUnit u : installationUnits) {
      for (P2InstallationFeature f : u.getFeatures()) {
        P2InstallationFeature feature =
            new P2InstallationFeature(f.getFeatureLabel(), f.getFeatureId());
        feature.setInstalled(isFeatureInstalled(feature.getFeatureId()));
        results.add(feature);
      }
    }
    return results;
  }

  /**
   * Parses the P2 update site info for the given installation units, resolve dependencies, and
   * returns a success or failure status. Invoke this method before invoking {@link #runP2Install()}
   * .
   */
  public IStatus resolveP2Information(IProgressMonitor progressMonitor) {
    try {
      String statusText = "Installing " + installationUnits.get(0).getInstallationUnitName();

      if (installationUnits.size() > 1) {
        statusText += "s";
      }

      SubMonitor monitor = SubMonitor.convert(progressMonitor, statusText, 100);

      try {
        computedInstallableUnits = computeInstallableUnits(monitor.newChild(50));

        checkCancelled(monitor);

        installOperation =
            resolve(monitor.newChild(50), computedInstallableUnits,
                repositoryLocations.toArray(new URI[0]));

        checkCancelled(monitor);

        return Status.OK_STATUS;
      } finally {
        monitor.done();
      }
    } catch (CoreException e) {
      return e.getStatus();
    } catch (Exception e) {
      return ProjectHostingUIPlugin.createErrorStatus(e);
    }
  }

  /**
   * Installs the desired features. Only call this if
   * {@link #resolveP2Information(IProgressMonitor)} returns without errors.
   */
  public void runP2Install() {
    Display.getDefault().asyncExec(new Runnable() {
      @Override
      public void run() {
        provisioningUI.openInstallWizard(Arrays.asList(computedInstallableUnits), installOperation,
            null);
      }
    });
  }

  private List<IMetadataRepository> addRepositories(SubMonitor monitor) throws ProvisionException {
    // tell p2 that it's okay to use these repositories
    ProvisioningSession session = ProvisioningUI.getDefaultUI().getSession();
    RepositoryTracker repositoryTracker = ProvisioningUI.getDefaultUI().getRepositoryTracker();
    repositoryLocations = new HashSet<URI>();
    monitor.setWorkRemaining(installationUnits.size() * 5);
    for (P2InstallationUnit descriptor : installationUnits) {
      URI uri = descriptor.getUpdateSiteURI();
      if (repositoryLocations.add(uri)) {
        checkCancelled(monitor);
        repositoryTracker.addRepository(uri, null, session);
      }
      monitor.worked(1);
    }

    // fetch meta-data for these repositories
    ArrayList<IMetadataRepository> repositories = new ArrayList<IMetadataRepository>();
    monitor.setWorkRemaining(repositories.size());
    IMetadataRepositoryManager manager =
        (IMetadataRepositoryManager) session.getProvisioningAgent().getService(
            IMetadataRepositoryManager.SERVICE_NAME);
    for (URI uri : repositoryLocations) {
      checkCancelled(monitor);
      IMetadataRepository repository = manager.loadRepository(uri, monitor.newChild(1));
      repositories.add(repository);
    }
    return repositories;
  }

  private void checkCancelled(IProgressMonitor monitor) {
    if (monitor.isCanceled()) {
      throw new OperationCanceledException();
    }
  }

  /**
   * Verifies that we found what we were looking for: it's possible that we have connector
   * descriptors that are no longer available on their respective sites. In that case we must inform
   * the user. Unfortunately this is the earliest point at which we can know.
   */
  private void checkForUnavailable(final List<IInstallableUnit> installableUnits)
      throws CoreException {
    // at least one selected connector could not be found in a repository
    Set<String> foundIds = new HashSet<String>();
    for (IInstallableUnit unit : installableUnits) {
      foundIds.add(unit.getId());
    }

    String message = ""; //$NON-NLS-1$
    String detailedMessage = ""; //$NON-NLS-1$
    for (P2InstallationUnit descriptor : installationUnits) {
      StringBuilder unavailableIds = null;
      for (String id : getFeatureGroupIds(descriptor)) {
        if (!foundIds.contains(id)) {
          if (unavailableIds == null) {
            unavailableIds = new StringBuilder();
          } else {
            unavailableIds.append(",");
          }
          unavailableIds.append(id);
        }
      }
      if (unavailableIds != null) {
        if (message.length() > 0) {
          message += ",";
        }
        message += descriptor.getInstallationUnitName();

        if (detailedMessage.length() > 0) {
          detailedMessage += ",";
        }
        detailedMessage +=
            NLS.bind("{0} (id={1}, site={2})", new Object[] {descriptor.getInstallationUnitName(),
                unavailableIds.toString(), descriptor.getUpdateSite()});
      }
    }

    if (message.length() > 0) {
      // instead of aborting here we ask the user if they wish to proceed
      // anyways
      final boolean[] okayToProceed = new boolean[1];
      final String finalMessage = message;
      Display.getDefault().syncExec(new Runnable() {
        @Override
        public void run() {
          okayToProceed[0] =
              MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
                  "Proceed With Installation?",
                  NLS.bind("The following features are not available: "
                      + "{0}\nProceed with the installation anyway?", new Object[] {finalMessage}));
        }
      });
      if (!okayToProceed[0]) {
        throw new CoreException(ProjectHostingUIPlugin.createErrorStatus(NLS.bind(
            "The following features are not available: {0}", detailedMessage)));
      }
    }
  }

  private IInstallableUnit[] computeInstallableUnits(SubMonitor monitor) throws CoreException {
    monitor.setWorkRemaining(100);

    // add repository urls and load meta data
    List<IMetadataRepository> repositories = addRepositories(monitor.newChild(50));
    final List<IInstallableUnit> installableUnits =
        queryInstallableUnits(monitor.newChild(50), repositories);
    removeOldVersions(installableUnits);
    checkForUnavailable(installableUnits);
    return installableUnits.toArray(new IInstallableUnit[installableUnits.size()]);
  }

  private IProfile getCurrentProfile() {
    // get the agent
    ServiceReference<?> sr =
        ProjectHostingUIPlugin.getBundleContext().getServiceReference(
            IProvisioningAgentProvider.SERVICE_NAME);

    if (sr == null) {
      return null;
    }

    IProvisioningAgentProvider agentProvider =
        (IProvisioningAgentProvider) ProjectHostingUIPlugin.getBundleContext().getService(sr);

    try {
      // null == the current Eclipse installation
      IProvisioningAgent agent = agentProvider.createAgent(null);

      IProfileRegistry profileRegistry =
          (IProfileRegistry) agent.getService(IProfileRegistry.SERVICE_NAME);

      return profileRegistry.getProfile(IProfileRegistry.SELF);
    } catch (ProvisionException e) {
      return null;
    }
  }

  private Set<String> getDescriptorIds(final IMetadataRepository repository) {
    final Set<String> installableUnitIdsThisRepository = new HashSet<String>();

    // determine all installable units for this repository
    for (P2InstallationUnit descriptor : installationUnits) {
      if (repository.getLocation().equals(descriptor.getUpdateSiteURI())) {
        installableUnitIdsThisRepository.addAll(getFeatureGroupIds(descriptor));
      }
    }

    return installableUnitIdsThisRepository;
  }

  private List<String> getFeatureGroupIds(P2InstallationUnit unit) {
    // ".feature.group"

    List<String> features = new ArrayList<String>();

    for (P2InstallationFeature p2Feature : unit.getFeatures()) {
      features.add(p2Feature.getFeatureId() + FEATURE_GROUP);
    }

    return features;
  }

  private boolean isFeatureInstalled(String featureId) {
    IProfile profile = getCurrentProfile();

    if (profile != null) {
      IQueryResult<IInstallableUnit> results =
          profile.available(QueryUtil.createIUQuery(featureId + FEATURE_GROUP),
              new NullProgressMonitor());

      return !results.isEmpty();
    }

    return false;
  }

  /**
   * Perform a query to get the installable units. This causes p2 to determine what features are
   * available in each repository. We select installable units by matching both the feature id and
   * the repository; it is possible though unlikely that the same feature id is available from more
   * than one of the selected repositories, and we must ensure that the user gets the one that they
   * asked for.
   */
  private List<IInstallableUnit> queryInstallableUnits(SubMonitor monitor,
      List<IMetadataRepository> repositories) {
    final List<IInstallableUnit> installableUnits = new ArrayList<IInstallableUnit>();

    monitor.setWorkRemaining(repositories.size());
    for (final IMetadataRepository repository : repositories) {
      checkCancelled(monitor);
      final Set<String> installableUnitIdsThisRepository = getDescriptorIds(repository);
      IQuery<IInstallableUnit> query = QueryUtil.createIUGroupQuery();
      IQueryResult<IInstallableUnit> result = repository.query(query, monitor.newChild(1));
      for (Iterator<IInstallableUnit> iter = result.iterator(); iter.hasNext();) {
        IInstallableUnit iu = iter.next();
        String id = iu.getId();
        if (installableUnitIdsThisRepository.contains(id)) {
          installableUnits.add(iu);
        }
      }
    }
    return installableUnits;
  }

  /**
   * Filters those installable units that have a duplicate in the list with a higher version number.
   * it's possible that some repositories will host multiple versions of a particular feature. we
   * assume that the user wants the highest version.
   */
  private void removeOldVersions(final List<IInstallableUnit> installableUnits) {
    Map<String, Version> symbolicNameToVersion = new HashMap<String, Version>();
    for (IInstallableUnit unit : installableUnits) {
      Version version = symbolicNameToVersion.get(unit.getId());
      if (version == null || version.compareTo(unit.getVersion()) < 0) {
        symbolicNameToVersion.put(unit.getId(), unit.getVersion());
      }
    }
    if (symbolicNameToVersion.size() != installableUnits.size()) {
      for (IInstallableUnit unit : new ArrayList<IInstallableUnit>(installableUnits)) {
        Version version = symbolicNameToVersion.get(unit.getId());
        if (!version.equals(unit.getVersion())) {
          installableUnits.remove(unit);
        }
      }
    }
  }

  private InstallOperation resolve(IProgressMonitor monitor, final IInstallableUnit[] ius,
      URI[] repositories) throws CoreException {
    final InstallOperation operation =
        provisioningUI.getInstallOperation(Arrays.asList(ius), repositories);
    IStatus operationStatus =
        operation.resolveModal(new SubProgressMonitor(monitor, installationUnits.size()));
    if (operationStatus.getSeverity() > IStatus.WARNING) {
      throw new CoreException(operationStatus);
    }
    return operation;
  }
}
