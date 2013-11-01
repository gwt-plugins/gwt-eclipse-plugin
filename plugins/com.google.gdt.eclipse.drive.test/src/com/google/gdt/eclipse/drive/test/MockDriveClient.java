/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.drive.test;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;

/**
 * Mock implementation of a subset of the Drive API, with {@code execute} methods that do nothing.
 */
public class MockDriveClient extends Drive {

  public MockDriveClient() {
    super(new NetHttpTransport(), new JacksonFactory(), null);
    // HttpTransport, JsonFactory, HttpRequestInitializer
  }

  @Override
  public Files files() {
    return new MockFiles();
  }
  
  /**
   * Mock implementation of {@link Drive.Files}.
   */
  private class MockFiles extends Files {

    @Override
    public Get get(String arg0) throws IOException {
      return new MockGet();
    }

    @Override
    public List list() throws IOException {
      return new MockList();
    }

    @Override
    public Update update(String arg0, File arg1, AbstractInputStreamContent arg2)
        throws IOException {
      return new MockUpdate(arg0, arg1, arg2);
    }

    /**
     * Mock implementation of {@link Drive.Files.List}.
     */
    private class MockList extends List {

      @Override public FileList execute() throws IOException {
        return null;
      }
      
    }
    
    /**
     * Mock implementation of {@link Drive.Files.Get}.
     */
    private class MockGet extends Get {

      public MockGet() {
        super("");
      }

      @Override
      public File execute() throws IOException {
        return null;
      }
      
    }
    
    /**
     * Mock implementation of {@link Drive.Files.Update}.
     */
    private class MockUpdate extends Update {

      public MockUpdate(String arg0, File arg1, AbstractInputStreamContent arg2) {
        super(arg0, arg1, arg2);
      }

      @Override
      public File execute() throws IOException {
        return null;
      }
      
    }
    
  }

}
