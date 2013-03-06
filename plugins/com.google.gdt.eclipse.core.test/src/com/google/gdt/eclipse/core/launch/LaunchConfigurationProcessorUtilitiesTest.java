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
package com.google.gdt.eclipse.core.launch;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the {@link LaunchConfigurationProcessorUtilities}.
 */
public class LaunchConfigurationProcessorUtilitiesTest extends TestCase {

  public void testCreateArgsString() {
    assertEquals("",
        LaunchConfigurationProcessorUtilities.createArgsString(Arrays.<String> asList()));
    assertEquals("",
        LaunchConfigurationProcessorUtilities.createArgsString(null));
    assertEquals(
        "",
        LaunchConfigurationProcessorUtilities.createArgsString(Arrays.asList("")));
    assertEquals(
        "oneArg",
        LaunchConfigurationProcessorUtilities.createArgsString(Arrays.asList("oneArg")));
    assertEquals("two Args",
        LaunchConfigurationProcessorUtilities.createArgsString(Arrays.asList(
            "two", "Args")));
    assertEquals("\"/tmp/Some Dir\" dummy",
        LaunchConfigurationProcessorUtilities.createArgsString(Arrays.asList(
            "/tmp/Some Dir", "dummy")));
    assertEquals("\"arg1\\\" With Quote\" arg2",
        LaunchConfigurationProcessorUtilities.createArgsString(Arrays.asList(
            "arg1\" With Quote", "arg2")));
  }

  public void testGetArgValue() {
    List<String> args = Arrays.asList("-noserver", "-war", "/some/dir");
    assertEquals(null, LaunchConfigurationProcessorUtilities.getArgValue(args,
        -1));
    assertEquals(null, LaunchConfigurationProcessorUtilities.getArgValue(args,
        args.size()));
    // This method only returns args which are values, so "-blah" is not treated
    // as a value
    assertEquals(null, LaunchConfigurationProcessorUtilities.getArgValue(args,
        0));
    assertEquals("/some/dir",
        LaunchConfigurationProcessorUtilities.getArgValue(args, 2));
  }

  public void testGetArgValueFromUpperCaseChoices() {
    String[] VALUES = {"PRETTY", "OBFUSCATED"};
    final String defaultValue = "OBFUSCATED";
    List<String> args = Arrays.asList("-noserver", "-style", "pretty");
    List<String> alreadyUpperCaseArgs = Arrays.asList("-noserver", "-style",
        "PRETTY");

    assertEquals("PRETTY",
        LaunchConfigurationProcessorUtilities.getArgValueFromUpperCaseChoices(
            args, "-style", VALUES, defaultValue));
    assertEquals("PRETTY",
        LaunchConfigurationProcessorUtilities.getArgValueFromUpperCaseChoices(
            alreadyUpperCaseArgs, "-style", VALUES, defaultValue));
    assertEquals("OBFUSCATED",
        LaunchConfigurationProcessorUtilities.getArgValueFromUpperCaseChoices(
            args, "-missingArg", VALUES, defaultValue));
  }

  public void testParseArgs() {
    assertEquals(Arrays.<String> asList(),
        LaunchConfigurationProcessorUtilities.parseArgs(null));
    assertEquals(Arrays.<String> asList(),
        LaunchConfigurationProcessorUtilities.parseArgs(""));
    assertEquals(Arrays.<String> asList(),
        LaunchConfigurationProcessorUtilities.parseArgs("   "));
    assertEquals(Arrays.asList("-one"),
        LaunchConfigurationProcessorUtilities.parseArgs("-one"));
    assertEquals(Arrays.asList("-one", "-two"),
        LaunchConfigurationProcessorUtilities.parseArgs("-one -two"));
    assertEquals(Arrays.asList("-one", "value"),
        LaunchConfigurationProcessorUtilities.parseArgs("-one value"));
    assertEquals(
        Arrays.asList("-one", "value with spaces"),
        LaunchConfigurationProcessorUtilities.parseArgs("-one \"value with spaces\""));
    assertEquals(
        Arrays.asList("-one", "test InnerQuotes"),
        LaunchConfigurationProcessorUtilities.parseArgs("-one test\" InnerQuotes\""));

    // NOTE: this needs to be run as a plug-in JUnit test because of this
    // assertion -- otherwise something deep in Eclipse NPEs
    assertEquals(Arrays.asList("\""),
        LaunchConfigurationProcessorUtilities.parseArgs("\\\""));
  }

  public void testRemoveArgsAndReturnInsertionIndex() {
    List<String> originalList = Arrays.asList("-war", "/tmp/war", "-noserver",
        "-d32");
    List<String> list;
    
    // OOB
    list = new ArrayList<String>(originalList);
    assertEquals(
        0,
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
            list, -1, true));
    assertEquals(originalList, list);
    
    list = new ArrayList<String>(originalList);
    assertEquals(
        0,
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
            list, list.size(), true));
    assertEquals(originalList, list);

    // Arg and its value removed
    list = new ArrayList<String>(originalList);
    assertEquals(
        0,
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
            list, 0, true));
    assertEquals(Arrays.asList("-noserver", "-d32"), list);

    // Arg alone removed
    list = new ArrayList<String>(originalList);
    assertEquals(
        2,
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
            list, 2, false));
    assertEquals(Arrays.asList("-war", "/tmp/war", "-d32"), list);

    // Arg that should have value but does not
    list = new ArrayList<String>(originalList);
    assertEquals(
        2,
        LaunchConfigurationProcessorUtilities.removeArgsAndReturnInsertionIndex(
            list, 2, true));
    assertEquals(Arrays.asList("-war", "/tmp/war", "-d32"), list);
  }
}
