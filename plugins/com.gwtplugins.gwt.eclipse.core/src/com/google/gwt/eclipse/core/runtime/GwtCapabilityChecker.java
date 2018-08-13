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
package com.google.gwt.eclipse.core.runtime;

import com.google.gdt.eclipse.core.sdk.Sdk.SdkException;
import com.google.gwt.eclipse.core.GWTPluginLog;

import java.net.MalformedURLException;
import java.net.URLClassLoader;

/**
 * Checks GWT SDKs for specific capabilities.
 */
public class GwtCapabilityChecker {

  /**
   * Creates new instances of the {@link GwtCapabilityChecker}.
   */
  public static class Factory {
    public GwtCapabilityChecker newGwtCapabilityChecker(GwtSdk sdk) {
      return new GwtCapabilityChecker(sdk);
    }
  }

  /**
   * @return whether the fully-qualified class name exists in the GWT SDK
   */
  private static boolean doesGwtClassExist(GwtSdk gwtRuntime, String qualifiedClassName) {
    try {
      URLClassLoader cl = gwtRuntime.createClassLoader();
      cl.loadClass(qualifiedClassName);
      return true;
    } catch (ClassNotFoundException e) {
      /*
       * No need for logging an exception here; we're just checking to see if this runtime contains the
       * given class or not.
       */
    } catch (MalformedURLException e) {
      GWTPluginLog.logError(e);
    } catch (SdkException e) {
      GWTPluginLog.logError(e);
    }

    return false;
  }

  private final GwtSdk sdk;

  public GwtCapabilityChecker(GwtSdk sdk) {
    this.sdk = sdk;
  }

  /**
   * @return true if the GWT compiler allows multiple modules to be passed in.
   */
  public boolean doesCompilerAllowMultipleModules() {
    // This new Compiler allows multiple modules to be passed in
    return doesGwtClassExist(sdk, "com.google.gwt.dev.Compiler");
  }

}
