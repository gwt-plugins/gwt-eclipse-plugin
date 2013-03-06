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
package com.google.appengine.eclipse.core.markers.quickfixes;

import com.google.appengine.eclipse.core.AppEngineCorePluginLog;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.gdt.eclipse.core.BuilderUtilities;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.JavaPluginImages;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.osgi.service.prefs.BackingStoreException;

import java.text.MessageFormat;
import java.util.List;

/**
 * Excludes a class or package from App Engine JRE whitelist validation.
 */
@SuppressWarnings("restriction")
public class ExcludeFromValidationProposal implements IJavaCompletionProposal {

  private final IJavaElement javaElement;

  private final int relevance;

  public ExcludeFromValidationProposal(IJavaElement javaElement, int relevance) {
    this.javaElement = javaElement;
    this.relevance = relevance;
  }

  public void apply(IDocument document) {
    IProject project = javaElement.getJavaProject().getProject();
    List<IPath> exclusions = GaeProjectProperties.getValidationExclusionPatterns(project);
    IResource resource = javaElement.getResource();
    IPath pattern = resource.getProjectRelativePath();
    if (resource instanceof IContainer) {
      pattern = pattern.addTrailingSeparator();
    }
    exclusions.add(pattern);

    try {
      GaeProjectProperties.setValidationExclusionPatterns(project, exclusions);
      BuilderUtilities.scheduleRebuild(project);
    } catch (BackingStoreException e) {
      AppEngineCorePluginLog.logError(e);
      MessageDialog.openError(null, "Quick Fix Failed",
          "Failed to update validation exclusion patterns.  Try refreshing your project.");
    }
  }

  public String getAdditionalProposalInfo() {
    return null;
  }

  public IContextInformation getContextInformation() {
    return null;
  }

  public String getDisplayString() {
    return MessageFormat.format("Exclude ''{0}'' from App Engine validation",
        javaElement.getElementName());
  }

  public Image getImage() {
    return JavaPlugin.getImageDescriptorRegistry().get(
        JavaPluginImages.DESC_OBJS_EXCLUSION_FILTER_ATTRIB);
  }

  public int getRelevance() {
    return relevance;
  }

  public Point getSelection(IDocument document) {
    return null;
  }

}