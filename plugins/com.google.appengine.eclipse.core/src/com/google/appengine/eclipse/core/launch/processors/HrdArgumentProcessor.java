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
package com.google.appengine.eclipse.core.launch.processors;

import com.google.appengine.eclipse.core.launch.AppEngineLaunchConfiguration;
import com.google.appengine.eclipse.core.nature.GaeNature;
import com.google.appengine.eclipse.core.properties.GaeProjectProperties;
import com.google.gdt.eclipse.core.StringUtilities;
import com.google.gdt.eclipse.core.launch.ILaunchConfigurationProcessor;
import com.google.gdt.eclipse.core.launch.LaunchConfigurationProcessorUtilities;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;

import java.util.List;

/**
 * Processes the "-Ddatastore.default_high_rep_job_policy_unapplied_job_pct"
 * argument for App Engine launches when using the high replication datastore.
 */
public class HrdArgumentProcessor implements ILaunchConfigurationProcessor {
  private static final String ARG_UNAPPLIED_JOB_PCT_PREFIX = "-Ddatastore.default_high_rep_job_policy_unapplied_job_pct=";

  public void update(ILaunchConfigurationWorkingCopy launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs)
      throws CoreException {

    int argIndex = StringUtilities.indexOfThatStartsWith(vmArgs,
        ARG_UNAPPLIED_JOB_PCT_PREFIX, 0);
    int insertIndex = LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
        vmArgs, argIndex, false);

    boolean usingHrd = GaeProjectProperties.getGaeHrdEnabled(javaProject.getProject());

    if (usingHrd && GaeNature.isGaeProject(javaProject.getProject())) {
      vmArgs.add(insertIndex, ARG_UNAPPLIED_JOB_PCT_PREFIX
          + AppEngineLaunchConfiguration.getUnappliedJobPct(launchConfig));
    }
  }

  public String validate(ILaunchConfiguration launchConfig,
      IJavaProject javaProject, List<String> programArgs, List<String> vmArgs) {
    return null;
  }
}
