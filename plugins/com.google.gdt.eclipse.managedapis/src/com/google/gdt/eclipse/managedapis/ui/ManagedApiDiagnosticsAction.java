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
package com.google.gdt.eclipse.managedapis.ui;

import com.google.gdt.eclipse.core.ActiveProjectFinder;
import com.google.gdt.eclipse.managedapis.ManagedApi;
import com.google.gdt.eclipse.managedapis.ManagedApiPlugin;
import com.google.gdt.eclipse.managedapis.ManagedApiProject;
import com.google.gdt.eclipse.managedapis.impl.ManagedApiProjectImpl;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;

import java.text.MessageFormat;

/**
 * TODO: doc me.
 */
public class ManagedApiDiagnosticsAction extends
    AbstractHandler {

  public Object execute(ExecutionEvent event) throws ExecutionException {
  
    IProject project = ActiveProjectFinder.getInstance().getProject();
    try {
      ManagedApiProject managedApiProject = ManagedApiProjectImpl.getManagedApiProject(JavaCore.create(project));
      if (managedApiProject != null) {
        managedApiProject.hasManagedApis();
        System.out.println("ManagedApi diagnostics");
        System.out.println("======================");
        System.out.println(MessageFormat.format("Project name: {0}",
            project.getName()));
        System.out.println(MessageFormat.format("ManagedApi project: {0}",
            new Object[] {managedApiProject.hasManagedApis() ? "true"
                : "false"}));
        String flag = managedApiProject.getJavaProject().getProject().getPersistentProperty(
            ManagedApiPlugin.MANAGED_API_FLAG_QNAME);
        System.out.println(MessageFormat.format("              flag: {0}",
            new Object[] {flag == null ? "null" : flag}));
        if (managedApiProject.hasManagedApis()) {
          System.out.println(MessageFormat.format(
              "ManagedApi root folder: {0}",
              new Object[] {(managedApiProject.getManagedApiRootFolder() != null
                  ? managedApiProject.getManagedApiRootFolder().toString()
                  : "N/A")}));
          System.out.println("Managed apis:");
          for (ManagedApi managedApi : managedApiProject.getManagedApis()) {
            System.out.println(MessageFormat.format("ManagedApi: {0}",
                new Object[] {managedApi.getName()}));
            System.out.println(MessageFormat.format("            {0}",
                new Object[] {managedApi.getVersion()}));
            System.out.println(MessageFormat.format("            {0}",
                new Object[] {managedApi.toString()}));
          }
        }
      } else {
        System.out.println("Can not access project (closed?)");
      }
    } catch (CoreException e) {
      System.out.println("Caught CoreException");
      e.printStackTrace();
    }
    return null;
  }
}
