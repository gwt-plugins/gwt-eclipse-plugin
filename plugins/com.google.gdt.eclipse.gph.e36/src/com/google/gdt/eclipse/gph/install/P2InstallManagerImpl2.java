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

// TODO: this P2 installation manager attempts to do inline installation 
// and hot-patching of plugins. This has some issues.

/**
 * An implementation of the P2InstallManager. This implementation and plugin
 * isolates the dependency on P2.
 */
public class P2InstallManagerImpl2 /*implements P2InstallManager*/ {
//  private static final String PLUGIN_ID = "com.google.gdt.eclipse.gph.e36";
//
//  private static Object getService(String name) {
//    BundleContext context = P2InstallManagerPlugin.getBundleContext();
//    ServiceReference reference = context.getServiceReference(name);
//
//    if (reference == null) {
//      return null;
//    }
//
//    Object result = context.getService(reference);
//
//    context.ungetService(reference);
//
//    return result;
//  }
//
//  private IEngine engine;
//
//  private List<P2InstallationUnit> installationUnits = new ArrayList<P2InstallationUnit>();
//
//  private IPlanner planner;
//
//  private IProfile profile;
//
//  private IProvisioningAgent provisioningAgent;
//
//  private ProvisioningContext provisioningContext;
//
//  private IProvisioningPlan provisioningPlan;
//
//  private List<IInstallableUnit> unitsToInstall = new ArrayList<IInstallableUnit>();
//
//  /**
//   * Create a new instance of P2InstallManagerImpl.
//   */
//  public P2InstallManagerImpl2() {
//
//  }
//
//  @SuppressWarnings("restriction")
//  public IStatus applyProfileChanges() {
//    try {
//      Configurator configurator = (Configurator) getService(Configurator.class.getName());
//
//      configurator.applyConfiguration();
//    } catch (IOException e) {
//      return new Status(IStatus.ERROR, PLUGIN_ID, 0, "Cannot apply changes", e);
//    } catch (IllegalStateException e) {
//      return new Status(IStatus.ERROR, PLUGIN_ID, 0, "Cannot apply changes", e);
//    }
//
//    return Status.OK_STATUS;
//  }
//
//  public List<P2LicenseInfo> calculateRequiredUnits(
//      IProgressMonitor progressMonitor) throws CoreException {
//    SubMonitor monitor = SubMonitor.convert(progressMonitor,
//        10 * (installationUnits.size() + 1));
//
//    List<IInstallableUnit> units = new ArrayList<IInstallableUnit>();
//
//    for (P2InstallationUnit installUnit : installationUnits) {
//      if (progressMonitor.isCanceled()) {
//        throw new OperationCanceledException();
//      }
//
//      URI updateSiteURI = installUnit.getUpdateSiteURI();
//
//      if (updateSiteURI != null) {
//        monitor.setTaskName("Retrieving installation information for "
//            + installUnit.getInstallationUnitName());
//
//        // Get the repository managers and define our repositories.
//        IMetadataRepositoryManager manager = (IMetadataRepositoryManager) getProvisioningAgent().getService(
//            IMetadataRepositoryManager.SERVICE_NAME);
//        IArtifactRepositoryManager artifactManager = (IArtifactRepositoryManager) getProvisioningAgent().getService(
//            IArtifactRepositoryManager.SERVICE_NAME);
//
//        manager.addRepository(updateSiteURI);
//        artifactManager.addRepository(updateSiteURI);
//
//        // Load and query the metadata.
//        IMetadataRepository metadataRepo = manager.loadRepository(
//            updateSiteURI, monitor.newChild(10));
//
//        for (String featureId : installUnit.getFeatures()) {
//          if (progressMonitor.isCanceled()) {
//            throw new OperationCanceledException();
//          }
//
//          // necessary magic
//          featureId += ".feature.group";
//
//          Collection<IInstallableUnit> featureResults = metadataRepo.query(
//              QueryUtil.createLatestQuery(QueryUtil.createIUQuery(featureId)),
//              new NullProgressMonitor()).toUnmodifiableSet();
//
//          units.addAll(featureResults);
//        }
//      }
//    }
//
//    // TODO: go back to using install operations?
//    // ProvisioningUI
//    // * InstallOperation op = new InstallOperation(session, new
//    // IInstallableUnit [] { myIU });
//    // * IStatus result = op.resolveModal(monitor);
//    // * if (result.isOK()) {
//    // * op.getProvisioningJob(monitor).schedule();
//    // * }
//
//    provisioningContext = new ProvisioningContext(getProvisioningAgent());
//
//    IProfileChangeRequest request = getPlanner().createChangeRequest(
//        getProfile());
//
//    request.addAll(units);
//
//    provisioningPlan = getPlanner().getProvisioningPlan(request,
//        provisioningContext, monitor.newChild(10));
//
//    IStatus planStatus = provisioningPlan.getStatus();
//
//    if (!planStatus.isOK()) {
//      throw new CoreException(planStatus);
//    }
//
//    IQueryResult<IInstallableUnit> result = provisioningPlan.getAdditions().query(
//        QueryUtil.createIUAnyQuery(), null);
//
//    unitsToInstall = new ArrayList<IInstallableUnit>(result.toUnmodifiableSet());
//
//    return createLicenseList(unitsToInstall);
//  }
//
//  // public static boolean isFeatureInstalled(String featureId) {
//  // IProfile profile = getCurrentProfile();
//  //
//  // if (profile == null) {
//  // return false;
//  // }
//  //
//  // IQueryResult<IInstallableUnit> results = profile.available(
//  // QueryUtil.createIUQuery(featureId + ".feature.group"),
//  // new NullProgressMonitor());
//  //
//  // return !results.isEmpty();
//  // }
//  //
//  // public static boolean isInstalled(P2InstallationUnit installationUnit) {
//  // for (String featureId : installationUnit.getFeatures()) {
//  // if (!isFeatureInstalled(featureId)) {
//  // return false;
//  // }
//  // }
//  //
//  // return true;
//  // }
//
//  @SuppressWarnings("restriction")
//  public IStatus installFeaturesInline(IProgressMonitor monitor)
//      throws CoreException {
//    // TODO: inline this internal class
//
//    return PlanExecutionHelper.executePlan(provisioningPlan, getEngine(),
//        provisioningContext, monitor);
//  }
//
//  public void setInstallationUnits(List<P2InstallationUnit> installationUnits) {
//    this.installationUnits = new ArrayList<P2InstallationUnit>(
//        installationUnits);
//  }
//
//  protected IEngine getEngine() {
//    if (engine == null) {
//      engine = (IEngine) getProvisioningAgent().getService(IEngine.SERVICE_NAME);
//    }
//
//    return engine;
//  }
//
//  protected IPlanner getPlanner() {
//    if (planner == null) {
//      planner = (IPlanner) getProvisioningAgent().getService(
//          IPlanner.SERVICE_NAME);
//    }
//
//    return planner;
//  }
//
//  protected IProfile getProfile() {
//    if (profile == null) {
//      IProfileRegistry profileRegistry = (IProfileRegistry) getProvisioningAgent().getService(
//          IProfileRegistry.SERVICE_NAME);
//
//      profile = profileRegistry.getProfile(IProfileRegistry.SELF);
//    }
//
//    return profile;
//  }
//
//  // PlatformUI.getWorkbench().restart();
//
//  protected IProvisioningAgent getProvisioningAgent() {
//    if (provisioningAgent == null) {
//      ServiceReference serviceReference = P2InstallManagerPlugin.getBundleContext().getServiceReference(
//          IProvisioningAgentProvider.SERVICE_NAME);
//
//      if (serviceReference != null) {
//        IProvisioningAgentProvider agentProvider = (IProvisioningAgentProvider) P2InstallManagerPlugin.getBundleContext().getService(
//            serviceReference);
//
//        try {
//          // null == the current Eclipse installation
//          provisioningAgent = agentProvider.createAgent(null);
//        } catch (ProvisionException e) {
//          P2InstallManagerPlugin.logError(e);
//        }
//      }
//    }
//
//    return provisioningAgent;
//  }
//
//  private List<P2LicenseInfo> createLicenseList(List<IInstallableUnit> units) {
//    List<P2LicenseInfo> licenseInfos = new ArrayList<P2LicenseInfo>();
//
//    for (IInstallableUnit unit : units) {
//      for (ILicense license : unit.getLicenses(null)) {
//        String name = unit.getProperty(IInstallableUnit.PROP_NAME, null);
//        String versionText = null;
//
//        Version version = unit.getVersion();
//
//        if (version != null) {
//          org.osgi.framework.Version v = org.osgi.framework.Version.parseVersion(version.getOriginal());
//
//          versionText = " [" + v.getMajor() + "." + v.getMinor() + "."
//              + v.getMicro() + "]";
//        }
//
//        P2LicenseInfo licenseInfo = new P2LicenseInfo(name, versionText,
//            license.getBody(), license.getUUID());
//
//        // TODO: is this the right way to eliminate duplicate features?
//        // we get one copy from the site meta repo and one from the jar content
//        // repo
//        if (!licenseInfos.contains(licenseInfo)) {
//          licenseInfos.add(licenseInfo);
//        }
//      }
//    }
//
//    return licenseInfos;
//  }

}
