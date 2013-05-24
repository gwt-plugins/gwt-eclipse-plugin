/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.appengine.eclipse.wtp.runtime;

import com.google.appengine.eclipse.core.preferences.GaePreferences;
import com.google.appengine.eclipse.core.sdk.GaeSdk;
import com.google.common.collect.Lists;
import com.google.gdt.eclipse.core.sdk.SdkUtils;

import org.eclipse.core.runtime.Platform;

import java.io.File;
import java.util.List;

/**
 * Various utility methods for GaeRuntime.
 */
public final class RuntimeUtils {
  private static final String ARG_XMX512M = "-Xmx512m";
  private static final String ARG_XSTART_ON_FIRST_THREAD = "-XstartOnFirstThread";
  private static final String BOOTCLASSPATH_PREFIX = "-Xbootclasspath/p:";
  private static final String ARG_JAVAAGENT_PREFIX = "-javaagent:";
  /**
   * Minimum version of App Engine SDK that requires a VM argument to specify the KickStart Java
   * agent.
   */
  private static final String MIN_SDK_VERSION_USING_KICKSTART_AGENT = "1.2.6";

  /**
   * @return GaeSdk bound for given <code>runtime</code>. If sdk cannot be found, returns default
   *         sdk if it exists. Otherwise returns <code>null</code>.
   */
  public static GaeSdk getRuntimeSdk(GaeRuntime runtime) {
    if (runtime == null) {
      // fallback to default
      return GaePreferences.getDefaultSdk();
    } else {
      String sdkName = runtime.getGaeSdkName();
      if (sdkName == null || sdkName.trim().isEmpty()) {
        return null;
      }
      GaeSdk sdk = GaePreferences.getSdkManager().getSdks().findSdk(sdkName);
      if (sdk == null) {
        // fallback to default
        return GaePreferences.getDefaultSdk();
      }
      return sdk;
    }
  }

  /**
   * @return GaeSdk bound for given <code>runtime</code>. If sdk is absent or cannot be found,
   *         returns <code>null</code>.
   */
  public static GaeSdk getRuntimeSdkNoFallback(GaeRuntime runtime) {
    if (runtime == null) {
      return null;
    }
    String sdkName = runtime.getGaeSdkName();
    if (sdkName == null || sdkName.trim().isEmpty()) {
      return null;
    }
    return GaePreferences.getSdkManager().getSdks().findSdk(sdkName);
  }

  /**
   * @return the list of VM arguments required by runtime.
   */
  public static List<String> getRuntimeVMArguments(GaeRuntime runtime) {
    List<String> args = Lists.newArrayList();
    if (runtime == null) {
      return args;
    }

    args.add(getJavaAgentVMArgument(runtime));
    args.add(ARG_XMX512M);
    args.add(getXBootclassPathVMArgument(runtime));
    if (Platform.OS_MACOSX.equals(Platform.getOS())) {
      args.add(ARG_XSTART_ON_FIRST_THREAD);
    }

    return args;
  }

  /**
   * @return VM argument representing javaagent.
   */
  private static String getJavaAgentVMArgument(GaeRuntime runtime) {
    GaeSdk gaeSdk = getRuntimeSdk(runtime);
    if (gaeSdk == null) {
      return null;
    }
    if (SdkUtils.compareVersionStrings(gaeSdk.getVersion(), MIN_SDK_VERSION_USING_KICKSTART_AGENT) >= 0) {
      String agentPath = gaeSdk.getInstallationPath().append("lib/agent/appengine-agent.jar").toOSString();
      return ARG_JAVAAGENT_PREFIX + agentPath;
    }
    return null;
  }

  /**
   * Adds a workaround to fix an issue with the jvm on Mac. This will add the -Xbootclasspath/p vm
   * argument and provide it with a jar in the gae sdk that will override a broken class.
   */
  private static String getXBootclassPathVMArgument(GaeRuntime runtime) {
    // only affects macs
    if (!Platform.OS_MACOSX.equals(Platform.getOS())) {
      return null;
    }
    GaeSdk gaeSdk = getRuntimeSdk(runtime);
    if (gaeSdk == null) {
      return null;
    }
    String jarPath = gaeSdk.getInstallationPath().append(
        "lib/override/appengine-dev-jdk-overrides.jar").toOSString();
    if (new File(jarPath).exists()) {
      return BOOTCLASSPATH_PREFIX + jarPath;
    }
    return null;
  }

  /**
   * private ctor
   */
  private RuntimeUtils() {
  }
}
