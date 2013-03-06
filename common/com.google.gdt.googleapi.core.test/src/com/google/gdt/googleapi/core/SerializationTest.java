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
package com.google.gdt.googleapi.core;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * Test serialization of types as required for caching.
 */
public class SerializationTest {
  @Test
  public void testApiDirectoryItem() throws IOException {
    ApiInfoImpl aii = new ApiInfoImpl("aaa-v2");
    aii.setDescription("test description");
    aii.setName("name-test");

    ApiDirectoryItem adi = new ApiDirectoryItem(aii);
    ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
    oos.writeObject(adi);
  }

  @Test
  public void testApiDirectoryListing() throws IOException {
    ApiInfoImpl aii1 = new ApiInfoImpl("aaa-v1");
    aii1.setDescription("test description");
    aii1.setName("name-test");
    aii1.setVersion("v1");
    aii1.setDownloadLink(new URL("http://www.google.com"));
    aii1.setPreferred(true);
    aii1.addLabel("bling");
    aii1.putIconLink("x16", new URL("http://www.google.com"));
    ApiDirectoryItem adi1 = new ApiDirectoryItem(aii1);
    ApiDirectoryListing adl = new ApiDirectoryListing();
    adl.setItems(Arrays.asList(new ApiDirectoryItem[] {adi1}));

    ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
    oos.writeObject(adl);
  }

  @Test
  public void testApiInfoImpl() throws IOException {
    PartialApiInfo pai = new PartialApiInfo();
    pai.setDescription("test description");
    pai.setName("name-test");
    ObjectOutputStream oos = new ObjectOutputStream(
        new ByteArrayOutputStream());
    oos.writeObject(pai);
  }

  @Test
  public void testSerializePartialApiInfo() throws IOException {
    ApiInfoImpl aii = new ApiInfoImpl("aaa-v2");
    aii.setDescription("test description");
    aii.setName("name-test");
    ObjectOutputStream oos = new ObjectOutputStream(
        new ByteArrayOutputStream());
    oos.writeObject(aii);
  }

}
