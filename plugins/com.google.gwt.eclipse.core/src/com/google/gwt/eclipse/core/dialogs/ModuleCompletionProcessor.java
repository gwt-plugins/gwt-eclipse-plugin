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
package com.google.gwt.eclipse.core.dialogs;

import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;
import com.google.gwt.eclipse.core.modules.IModule;
import com.google.gwt.eclipse.core.modules.ModuleUtils;
import com.google.gwt.eclipse.core.resources.GWTImages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides auto-completion for {@link ModuleSelectionDialogButtonField}.
 * 
 * This class needs to implement the deprecated
 * ISubjectControlContentAssistProcessor interface because of an instanceof
 * check inside the ContentAssistant.computeCompletionProposals method. If that
 * check fails, no completion proposals are returned. The JDT completion
 * processors for package name, package fragment root, etc. all implement the
 * same deprecated interface as well.
 * 
 */
@SuppressWarnings("deprecation")
public class ModuleCompletionProcessor implements IContentAssistProcessor,
    ISubjectControlContentAssistProcessor {

  private static final int SIMPLE_NAME_PROPOSAL_RELEVANCE = 1;

  private static final int QUALIFIED_NAME_PROPOSAL_RELEVANCE = 2;

  private static final int PACKAGE_PROPOSAL_RELEVANCE = 3;

  /**
   * The fully-qualified module name completions only show up after the user has
   * entered the complete package name, so we need to provide custom matching
   * rules.
   */
  private static class QualifiedModuleCompletion extends
      ModuleCompletionProposal {

    public QualifiedModuleCompletion(String replacementString,
        int replacementOffset, int replacementLength, Image image,
        String displayString, String prefixCompareString, int relevance) {
      super(replacementString, replacementOffset, replacementLength, image,
          displayString, prefixCompareString, relevance);
    }

    @Override
    public boolean matches(String input) {
      String packageName = Signature.getQualifier(this.prefixCompareString);
      if (!input.toLowerCase().startsWith(packageName.toLowerCase())) {
        return false;
      }
      return super.matches(input);
    }
  }

  // TODO: consider moving to module utility class, since the module selection
  // dialog has identical logic
  private static String simpleNameDisplayString(IModule module) {
    return module.getSimpleName() + " - " + module.getPackageName();
  }

  private List<ModuleCompletionProposal> allProposals;

  private final JavaElementLabelProvider javaLabelProvider;

  private IJavaProject javaProject;

  public ModuleCompletionProcessor() {
    javaLabelProvider = new JavaElementLabelProvider(
        JavaElementLabelProvider.SHOW_SMALL_ICONS);
    allProposals = new ArrayList<ModuleCompletionProposal>();
  }

  public ICompletionProposal[] computeCompletionProposals(
      IContentAssistSubjectControl contentAssistSubjectControl,
      int documentOffset) {
    String input = contentAssistSubjectControl.getDocument().get();
    return createProposals(documentOffset, input);
  }

  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer,
      int offset) {
    Assert.isTrue(false, "ITextViewer not supported");
    return null;
  }

  public IContextInformation[] computeContextInformation(
      IContentAssistSubjectControl contentAssistSubjectControl,
      int documentOffset) {
    return null;
  }

  public IContextInformation[] computeContextInformation(ITextViewer viewer,
      int offset) {
    Assert.isTrue(false, "ITextViewer not supported");
    return null;
  }

  public char[] getCompletionProposalAutoActivationCharacters() {
    return new char[] {'.'};
  }

  public char[] getContextInformationAutoActivationCharacters() {
    return null;
  }

  public IContextInformationValidator getContextInformationValidator() {
    return null;
  }

  public String getErrorMessage() {
    return null;
  }

  public void setProject(IJavaProject javaProject) {
    if (javaProject == null) {
      // Clear the list if we don't have a project
      this.javaProject = null;
      this.allProposals = new ArrayList<ModuleCompletionProposal>();
    } else if (!javaProject.equals(this.javaProject)) {
      // Regenerate the list of proposals when we switch projects (note that
      // the javaProject field must be set before calling createProposals).
      this.javaProject = javaProject;
      this.allProposals = createProposals();
    }
  }

  private void addPackageProposal(List<ModuleCompletionProposal> proposals,
      IModule module) {
    // Don't add a proposal for the default package
    String modulePackageName = module.getPackageName();
    if (modulePackageName.length() == 0) {
      return;
    }

    // Don't add duplicate proposals
    for (ModuleCompletionProposal proposal : proposals) {
      if (proposal.getDisplayString().equals(modulePackageName)) {
        return;
      }
    }

    try {
      for (IPackageFragment packageFragment : javaProject.getPackageFragments()) {
        if (packageFragment.getElementName().equals(modulePackageName)) {
          // Delegate to the Java label provider for the text + icon
          Image image = javaLabelProvider.getImage(packageFragment);
          String displayString = packageFragment.getElementName();
          ModuleCompletionProposal proposal = new ModuleCompletionProposal(
              modulePackageName, 0, 0, image, displayString, modulePackageName,
              PACKAGE_PROPOSAL_RELEVANCE);
          proposals.add(proposal);
          break;
        }
      }
    } catch (JavaModelException e) {
      GWTPluginLog.logError(e);
    }
  }

  private void addQualifiedNameProposal(
      List<ModuleCompletionProposal> proposals, IModule module) {
    ModuleCompletionProposal proposal = new QualifiedModuleCompletion(
        module.getQualifiedName(), 0, 0, GWTPlugin.getDefault().getImage(
            GWTImages.MODULE_ICON), simpleNameDisplayString(module),
        module.getQualifiedName(), QUALIFIED_NAME_PROPOSAL_RELEVANCE);
    proposals.add(proposal);
  }

  private void addSimpleNameProposal(List<ModuleCompletionProposal> proposals,
      IModule module) {
    ModuleCompletionProposal proposal = new ModuleCompletionProposal(
        module.getQualifiedName(), 0, 0, GWTPlugin.getDefault().getImage(
            GWTImages.MODULE_ICON), simpleNameDisplayString(module),
        module.getSimpleName(), SIMPLE_NAME_PROPOSAL_RELEVANCE);
    proposals.add(proposal);
  }

  private List<ModuleCompletionProposal> createProposals() {
    List<ModuleCompletionProposal> proposals = new ArrayList<ModuleCompletionProposal>();

    // Do not include modules in JARS for right now
    IModule[] modules = ModuleUtils.findAllModules(javaProject, false);
    for (IModule module : modules) {
      addSimpleNameProposal(proposals, module);
      addQualifiedNameProposal(proposals, module);
      addPackageProposal(proposals, module);
    }

    return proposals;
  }

  private ICompletionProposal[] createProposals(int documentOffset, String input) {
    assert (this.allProposals != null);

    String prefix = input.substring(0, documentOffset);
    List<ModuleCompletionProposal> proposals = new ArrayList<ModuleCompletionProposal>();

    // Compile a list of proposals matching the user input
    for (ModuleCompletionProposal proposal : this.allProposals) {
      if (proposal.matches(prefix)) {
        proposal.setReplacementLength(prefix.length());
        proposals.add(proposal);
      }
    }

    Collections.sort(proposals);

    return proposals.toArray(new ICompletionProposal[0]);
  }

}
