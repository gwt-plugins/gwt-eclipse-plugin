/******************************************************************************
 * Copyright 2023.
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
package com.google.gdt.eclipse.maven.configurators;

import org.apache.maven.project.MavenProject;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.configurator.ProjectConfigurationRequest;

import java.lang.reflect.Method;

/**
 * @author Luca Piccarreta
 *
 * Adaptation layer for m2e 1.x / 2.x API
 */
public class M2eCompat {

  private M2eCompat() {
    // Hide constructor of static only method
  }

  private static class M2eMethods {
    Method getMavenProject = null;
    Method getMavenProjectFacade = null;
  }

  private static M2eMethods handles = null;

  /**
   * Extract maven project from an instance of  {@link ProjectConfigurationRequest}, i.e.
   * - request.getMavenProject for m2e 1.x
   * - request.mavenProject for m2e 2.x
   *
   * @param request a non-null instance of {@link ProjectConfigurationRequest}
   *
   * @return the MavenProject associated to the input request
   */
  public static MavenProject getMavenProject(ProjectConfigurationRequest request) {
    try {
      return (MavenProject)invoke(ensureM2eMethods(request).getMavenProject, request);
    } catch (Throwable e) {
      throw new IllegalStateException("Failed getting MavenProject from ProjectConfigurationRequest", e);
    }
  }

  /**
   * Extract maven project facade from an instance of  {@link ProjectConfigurationRequest}, i.e.
   * - request.getMavenProjectFacade for m2e 1.x
   * - request.mavenProjectFacade for m2e 2.x
   *
   * @param request a non-null instance of {@link ProjectConfigurationRequest}
   *
   * @return the MavenProject associated to the input request
   */
  public static IMavenProjectFacade getMavenProjectFacade(ProjectConfigurationRequest request) {
    try {
      return (IMavenProjectFacade)invoke(ensureM2eMethods(request).getMavenProjectFacade, request);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed getting IMavenProjectFacade from ProjectConfigurationRequest", e);
    }
  }


  /*
   * Return (and cache) a structure which holds java.lang.reflect.Method's for m2e methods which have a different signature
   * in m2e 1.0 and m2e 2.0
   *
   * @param request
   * @return
   * @throws NoSuchMethodException
   */
  private synchronized static final M2eMethods ensureM2eMethods(ProjectConfigurationRequest request) throws NoSuchMethodException {
    if(handles == null) {
      M2eMethods h = new M2eMethods();
      h.getMavenProject = findAvailableMethod(request, "mavenProject", "getMavenProject");
      h.getMavenProjectFacade = findAvailableMethod(request, "mavenProjectFacade", "getMavenProjectFacade");
      M2eCompat.handles = h;
    }
    return M2eCompat.handles;
  }

  /**
   * Find the first method with a given signature in a list of names
   *
   * Making this method generic makes the compiler happy even if the object is a record (which is the case in m2e 2.0)
   *
   * @param <T>
   *
   * @param obj an instance of the class in which the method has to be searched for
   * @param methodNames an array of possible names
   *
   * @return a non-null {@link Method}
   * @throws NoSuchMethodException if no method id found
   */
  private final static <T> Method findAvailableMethod(T obj, String... methodNames) throws NoSuchMethodException {
    for(String methodName : methodNames) {
      try {
        return obj.getClass().getMethod(methodName);
      } catch(ReflectiveOperationException e) {
        // intentionally swallowing exception
      }
    }
    throw new NoSuchMethodException("Can't find in " + obj.getClass() + ". Tried " + String.join(",", methodNames));
  }

  /**
  /*
   * Invoke a method on an object.
   *
   * The only reason for such a method is because generic parameters seem to disable class compatibility checks
   *
   * @param <T>
   *
   * @param method the method to invoke
   * @param obj an object on which to invoke
   * @return invocation result
   * @throws ReflectiveOperationException
   */
  private final static <T> Object invoke (Method method, T obj) throws ReflectiveOperationException {
    try {
      return method.invoke(obj);
    } catch(Throwable t) {
      // restore interrupt status if necessary
      if(t instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      throw new ReflectiveOperationException(t);
    }
  }
}
