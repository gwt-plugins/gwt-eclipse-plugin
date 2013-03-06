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
package com.google.gwt.eclipse.core.speedtracer;

import com.google.gdt.eclipse.core.ResourceUtils;
import com.google.gdt.eclipse.core.StatusUtilities;
import com.google.gwt.eclipse.core.GWTPlugin;
import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.runtime.CoreException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates the trampoline HTML file required to auto-start Speed Tracer.
 * 
 * Speed Tracer cannot see the command-line for the Chrome launch. It can,
 * however, see the contents of HTML pages. To launch Chrome and start Speed
 * Tracer automatically, we create this trampoline HTML file and point Chrome to
 * it. Speed Tracer sees the magic inside the HTML, and starts up.
 */
public class SpeedTracerTrampolineFileGenerator {

  private static String computeContents(String redirectUrl) throws CoreException {
    String finalRedirectUrl = redirectUrl;
    try {
      // Some boilerplate required to HTML encode the URL (need to use the
      // multi-arg constructor of URI in order for it to escape)
      URL url = new URL(redirectUrl);
      finalRedirectUrl = new URI(url.getProtocol(), url.getUserInfo(),
          url.getHost(), url.getPort(), url.getPath(), url.getQuery(), null).toURL().toString();
    } catch (Exception e) {
      GWTPluginLog.logWarning(e, "Could not encode URL");
    }

    Map<String, String> replacements = new HashMap<String, String>();
    replacements.put("@RedirectUrl@", finalRedirectUrl);
    String contents = ResourceUtils.getResourceAsStringAndFilterContents(
        SpeedTracerTrampolineFileGenerator.class, replacements,
        "speedtracer-trampoline-file.template");
    if (contents == null) {
      throw new CoreException(StatusUtilities.newErrorStatus(
          "Could not get the contents of the Speed Tracer trampoline file.",
          GWTPlugin.PLUGIN_ID));
    } else {
      return contents;
    }
  }

  private final String contents;

  public SpeedTracerTrampolineFileGenerator(String redirectUrl)
      throws CoreException {
    contents = computeContents(redirectUrl);
  }

  public File generate() throws IOException, CoreException {
    File tempFile = File.createTempFile("speedtracer-", ".html");
    FileWriter fw = new FileWriter(tempFile);
    try {
      fw.write(contents);
    } finally {
      fw.close();
    }
    return tempFile;
  }
}
