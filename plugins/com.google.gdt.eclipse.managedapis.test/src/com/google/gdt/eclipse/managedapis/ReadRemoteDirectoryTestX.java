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
package com.google.gdt.eclipse.managedapis;

//import com.google.gdt.eclipse.managedapis.directory.ApiDirectory;
//import com.google.gdt.eclipse.managedapis.directory.ApiDirectoryFactory;
//import com.google.gdt.eclipse.managedapis.impl.RemoteApiDirectory;
//
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.NullProgressMonitor;
//import org.eclipse.core.runtime.Status;
//import org.junit.Assert;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import java.io.IOException;

/**
 * This test has been disabled; hence the X after its name. The code has also
 * been commented out because it has a dependency on JUnit4, which is not the
 * default on Eclipse 3.5 (which ends up causing build breaks). We need to
 * re-enable it after dropping support for Eclipse 3.5
 */
public class ReadRemoteDirectoryTestX {

  // TODO (aiuto): Fix this test.
//  @Ignore("Test failing: Need to start server before running this test.")
//  @Test
//  public void readRemoteDirectory() throws IOException {
//    ApiDirectoryFactory factory = new ApiDirectoryFactory() {
//      public ApiDirectory buildApiDirectory() {
//        RemoteApiDirectory directory = new RemoteApiDirectory();
//        directory.setDirectoryLink("http://localhost:8888/5935");
//        return directory;
//      }
//    };
//
//    IStatus jobStatus = Status.OK_STATUS;
//
//    ApiDirectory source = factory.buildApiDirectory();
//    jobStatus = source.run(new NullProgressMonitor());
//
//    Assert.assertEquals(Status.OK_STATUS, jobStatus);
//    Assert.assertNotNull(source.getApiDirectoryListing());
//  }

}
