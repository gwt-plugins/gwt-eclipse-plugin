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
package com.google.gdt.eclipse.maven.builders;

import com.google.appengine.eclipse.core.nature.GaeNature;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resource change listener that examines pre-build events where a new project
 * has been added.
 * 
 * If this project has the ORM enhancement builder, we need to move it to the
 * end of the build spec. It needs to be placed after any builders that create
 * .class files. In particular, it needs to be placed after the AspectJ builder
 * and the Maven 2 builder. Normally, we'd do this in an m2Eclipse configurator,
 * but m2Eclipse adds the Maven Builder AFTER running all the configurators, so
 * we have to resort to this hack to get things to work as we want them to.
 */
public class EnsureEnhancerBuilderLast implements IResourceChangeListener {

  public void resourceChanged(IResourceChangeEvent event) {
    if (event.getType() == IResourceChangeEvent.PRE_BUILD) {
      IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
        public boolean visit(IResourceDelta delta) throws CoreException {
          if (delta.getResource().getType() == IResource.PROJECT
              && delta.getKind() == IResourceDelta.ADDED) {
            IProject project = (IProject) delta.getResource();
            if (GaeNature.isGaeProject(project)) {
              // Only examine GAE projects
              ensureEnhancerBuilderLast(project);
            }
            return false;
          }
          return true;
        }
      };
      try {
        event.getDelta().accept(visitor);
      } catch (CoreException e) {
      }
    }
  }

  private void ensureEnhancerBuilderLast(IProject project) throws CoreException {
    IProjectDescription description = project.getDescription();
    List<ICommand> builders = new ArrayList<ICommand>(
        Arrays.asList(description.getBuildSpec()));

    if (builders.size() > 0
        && builders.get(builders.size() - 1).getBuilderName().equals(
            GaeNature.CLASS_ENHANCER_BUILDER)) {
      // Enhancer builder is already last; nothing to do.
      return;
    }

    for (int i = 0, size = builders.size(); i < size; i++) {
      if (builders.get(i).getBuilderName().equals(
          GaeNature.CLASS_ENHANCER_BUILDER)) {
        ICommand enhancerBuilder = builders.remove(i);
        builders.add(enhancerBuilder);
        description.setBuildSpec(builders.toArray(new ICommand[builders.size()]));
        project.setDescription(description, new NullProgressMonitor());
        return;
      }
    }
  }

}
