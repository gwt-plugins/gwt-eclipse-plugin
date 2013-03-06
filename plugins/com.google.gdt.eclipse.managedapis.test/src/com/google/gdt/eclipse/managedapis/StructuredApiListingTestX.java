/*******************************************************************************
 * Copyright 2011 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.gdt.eclipse.managedapis;

//
// import com.google.gdt.eclipse.managedapis.directory.StructuredApiCollection;
// import
// com.google.gdt.eclipse.managedapis.impl.LatestVersionOnlyStructuredApiCollection;
// import com.google.gdt.googleapi.core.ApiDirectoryItem;
// import com.google.gdt.googleapi.core.ApiInfoImpl;
//
// import org.junit.Before;
// import org.junit.Test;
//
// import static org.junit.Assert.assertEquals;

/**
 * TODO:
 * 
 * This test has been disabled; hence the X after its name. The code has also
 * been commented out because it has a dependency on JUnit4, which is not the
 * default on Eclipse 3.5 (which ends up causing build breaks). We need to
 * re-enable it after dropping support for Eclipse 3.5
 * 
 * Also, it's not clear that this test actually matters anymore, as the
 * implementation of LatestVersionOnlyStructuredApiCollection seems to show all
 * versions of a given API now. LatestVersionOnlyStructuredApiCollection needs
 * to be renamed, and this test needs to be re-worked to test a different
 * aspect of the class. 
 */
public class StructuredApiListingTestX {
  //
  // ApiInfoImpl info1_1rel = new ApiInfoImpl("t1:v1");
  // ApiInfoImpl info1_2labs = new ApiInfoImpl("t1:v2");
  // ApiInfoImpl info1_3labs = new ApiInfoImpl("t1:v3");
  // ApiInfoImpl info2_1rel = new ApiInfoImpl("t2:v1");
  // ApiDirectoryItem entry1_1rel;
  // ApiDirectoryItem entry1_3labs;
  // ApiDirectoryItem entry2_1rel;
  //
  // @Test
  // public void directoryListingTest() {
  // StructuredApiCollection sal1 = new
  // LatestVersionOnlyStructuredApiCollection();
  // sal1.add(entry1_1rel);
  // sal1.add(entry1_3labs);
  // sal1.add(entry2_1rel);
  //
  // assertEquals("3 api listing entries should produce 0 installed listings",
  // 0, sal1.getInstalledEntries().getEntries().size());
  // assertEquals(
  // "3 tagged api listing entries should produce 2 listings (2 different APIs)",
  // 2, sal1.getListing().getEntries().size());
  // }
  //
  // @Before
  // public void setUp() {
  // info1_1rel.setName("t1");
  // info1_3labs.setName("t1");
  // info1_2labs.setName("t1");
  // info2_1rel.setName("t2");
  //
  // // installedApi1_1rel = new ManagedApi(info1_1rel, null);
  // // installedApi1_2labs = new ManagedApi(info1_2labs, null);
  // // installedApi2_1rel = new ManagedApi(info2_1rel, null);
  // entry1_1rel = new ApiDirectoryItem(info1_1rel);
  // entry1_3labs = new ApiDirectoryItem(info1_3labs);
  // entry2_1rel = new ApiDirectoryItem(info2_1rel);
  // }
}
