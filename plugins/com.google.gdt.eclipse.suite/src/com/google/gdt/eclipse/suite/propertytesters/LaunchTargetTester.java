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
package com.google.gdt.eclipse.suite.propertytesters;

import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.gdt.eclipse.core.AdapterUtilities;
import com.google.gdt.eclipse.core.CorePluginLog;
import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.WebAppUtilities;
import com.google.gwt.eclipse.core.launch.GWTJUnitPropertyTester;
import com.google.gwt.eclipse.core.nature.GWTNature;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;

/**
 * A PropertyTester applied to resources to determine if they should have a Web
 * Application launch shortcut applied to them.
 * 
 * This is the case when a selection is in a project that has the webapp nature
 * and any of the following apply: the selection is the project itself, it's
 * <WAR>/WEB-INF/{web.xml,appengine-web.xml}, it's an html or jsp file somewhere
 * under the war directory, or it's a .gwt.xml file and the containing project
 * has GWT nature.
 * 
 * For legacy GWT projects, launchable resources include: the project itself, a
 * GWT module (.gwt.xml file), and any .html files.
 * 
 * FIXME: Unit tests for this. We need a com.google.gdt.eclipse.suite.test
 * plugin first, though.
 */
public class LaunchTargetTester extends PropertyTester {

  public boolean test(Object receiver, String property, Object[] args,
      Object expectedValue) {

    assert (receiver != null);
    IResource resource = AdapterUtilities.getAdapter(receiver, IResource.class);

    if (resource == null) {
      // Unexpected case; we were asked to test against something that's
      // not a resource.
      return false;
    }

    // Resolve to the actual resource (if it is linked)
    resource = ResourceUtils.resolveTargetResource(resource);

    try {
      return (isGaeOrGwtProject(resource) && (resourceIsProject(resource)
          || receiverIsJavaButNotTestCase(receiver)
          || resourceIsDeploymentDescriptor(resource)
          || resourceIsHostPage(resource) || resourceIsGwtXmlAndInGwt(resource)));
    } catch (CoreException ce) {
      CorePluginLog.logError(ce);
      return false;
    }
  }

  private boolean isGaeOrGwtProject(IResource resource) {
    return GWTNature.isGWTProject(resource.getProject())
        || GaeNature.isGaeProject(resource.getProject());
  }

  /**
   * @param receiver
   * @return whether the receiver meets the conditions (e.g. java, not test)
   */
  private boolean receiverIsJavaButNotTestCase(Object receiver) {
    // Ensure it is a Java element
    if (AdapterUtilities.getAdapter(receiver, IJavaElement.class) == null) {
      return false;
    }
    // Ensure it is not a test case
    GWTJUnitPropertyTester tester = new GWTJUnitPropertyTester();
    return !tester.test(receiver, GWTJUnitPropertyTester.PROPERTY_IS_GWT_TEST,
        null, null);
  }

  /**
   * Returns true if the resource is a web.xml or appengine-web.xml in the
   * canonical location.
   * 
   * @param resource
   * @return whether the resource is a web.xml or appengine-web.xml file as expected.
   */
  private boolean resourceIsDeploymentDescriptor(IResource resource) {
    IProject project = resource.getProject();

    if (WebAppUtilities.isWebApp(project)) {
      IFolder webInf = WebAppUtilities.getWebInfSrc(project);
      if (webInf.exists()) {
        if (resource.getParent().equals(webInf)) {
          String name = resource.getName();
          return name.equals("web.xml") || name.equals("appengine-web.xml");
        }
      }
    }
    return false;
  }

  /**
   * If the resource is a .gwt.xml file and we're in a gwt-enabled project,
   * return true.
   * 
   * @throws CoreException
   */
  private boolean resourceIsGwtXmlAndInGwt(IResource resource)
      throws CoreException {
    return GWTNature.isGWTProject(resource.getProject())
        && resource.getName().endsWith(".gwt.xml");
  }

  /**
   * WAR projects: Is this resource an html or jsp page under the war directory
   * or one of its subdirectories? Legacy GWT projects: Is this resource an html
   * file?
   * 
   * @param resource
   * @return whether the resource matches the specified conditions (a servable html/jsp page).
   */
  private boolean resourceIsHostPage(IResource resource) {
    IProject project = resource.getProject();

    if (WebAppUtilities.isWebApp(project)) {
      IFolder war = WebAppUtilities.getWarSrc(project);

      if (war != null) {
        if (war.getFullPath().isPrefixOf(resource.getFullPath())) {
          return ResourceUtils.hasJspOrHtmlExtension(resource);
        }
      }
    } else {
      // Legacy GWT project
      return "html".equalsIgnoreCase(resource.getFileExtension());
    }

    return false;
  }

  /**
   * Is this resource a project?
   * 
   * @param resource
   * @return true iff the resource is a project.
   */
  private boolean resourceIsProject(IResource resource) {
    if (resource == null) {
      return false;
    }

    IProject proj = resource.getProject();
    boolean out = (proj == resource);
    return out;
  }
}
