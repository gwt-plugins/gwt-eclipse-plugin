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

import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * TODO: provide javadocs
 */
public class JsonCodecTests {

  private static String readFileIntoString(File source)
      throws FileNotFoundException {
    assert source.exists() : "File not found";
    assert source.canRead() : "Can not read file";

    return new Scanner(source).useDelimiter("\\Z").next();
  }

  ApiDirectoryListingJsonCodec codec = new ApiDirectoryListingJsonCodec();

  private ApiInfoImpl ai_1 = null;

  @Before
  public void setUp() {
    try {
      ai_1 = new ApiInfoImpl("zoo:v2").withName("zoo").withVersion("v2").withDisplayName(
          "Zoo").withDescription("Zoo API used for Google API testing").withDocumentationLink(
          "http://link/to/docs").withPublisher("Not Google").withTosLink(
          "http://link/to/tos").withIconLink("32x32",
          "http://link/to/icon32x32.png").withReleaseDate(
          new LocalDate(2010, 11, 29)).withReleaseNotesLink(
          "http://link/to/release/notes").withLabel("labs").withDiscoveryLink(
          "http://link/to/discovery").withPreferred(true);
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testDeserializeEntry() throws FileNotFoundException,
      MalformedURLException {
    String jsonSampleData1 = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_Single_API_1.json"));
    ReadableApiInfo deserialized1 = codec.toApiInfo(jsonSampleData1, null);
    Assert.assertNotNull(deserialized1);
    Assert.assertEquals("buzz", deserialized1.getName());
    Assert.assertEquals("v1", deserialized1.getVersion());
    Assert.assertEquals("Google, Inc.", deserialized1.getPublisher());
    Assert.assertEquals("Google Buzz API", deserialized1.getDescription());
    Assert.assertEquals("pretty name", deserialized1.getDisplayName());
    Assert.assertEquals(new LocalDate(2010, 11, 20),
        deserialized1.getReleaseDate());
    Assert.assertTrue(deserialized1.getIconLinkKeys().contains("32x32"));
    Assert.assertEquals(new URL("http://icons.google.com/someicon.png"),
        deserialized1.getIconLink("32x32"));

    String jsonSampleData2 = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_Single_API_2.json"));
    ReadableApiInfo deserialized2 = codec.toApiInfo(jsonSampleData2, new URL(
        "http://baseurl.com"));
    Assert.assertNotNull(deserialized2);
    Assert.assertEquals("http://icons.google.com/someicon.png",
        deserialized2.getIconLink("32x32").toExternalForm());
  }

  @Test
  public void testDeserializeEntryWithBaseURL() throws FileNotFoundException,
      MalformedURLException {
    String jsonSampleData1 = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_Single_API_3.json"));
    ApiInfo deserialized1 = codec.toApiInfo(jsonSampleData1, new URL(
        "https://www-googleapis-test.sandbox.google.com/discovery/v0.3beta1/"));
    Assert.assertEquals("appsmarket:v2sandbox", deserialized1.getIdentifier());
    Assert.assertEquals("appsmarket", deserialized1.getName());
    Assert.assertEquals("v2sandbox", deserialized1.getVersion());
    Assert.assertEquals("Google Apps Marketplace API",
        deserialized1.getDescription());
    Assert.assertEquals("appsmarket", deserialized1.getDisplayName());
    Assert.assertEquals(
        new URL(
            "https://www-googleapis-test.sandbox.google.com/discovery/v0.3beta1/describe/appsmarket/v2sandbox"),
        deserialized1.getDiscoveryLink());
    Assert.assertTrue(deserialized1.hasLabel("labs"));
    Assert.assertTrue(!deserialized1.hasLabel("black_labs"));
    Assert.assertTrue(deserialized1.isPreferred());

    Assert.assertEquals("labs", deserialized1.getLabels()[0]);
  }

  @Test
  public void testDeserializeListing() throws FileNotFoundException,
      MalformedURLException {
    String jsonSampleData1 = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_API_List_1.json"));
    ApiDirectoryListing deserialized1 = codec.toApiDirectoryListing(
        jsonSampleData1, null);
    Assert.assertEquals(2, deserialized1.getItems().length);
    Assert.assertEquals("buzz", deserialized1.get(0).getName());
    Assert.assertEquals("v2beta1", deserialized1.get(0).getVersion());
    Assert.assertEquals("Google, Inc.", deserialized1.get(1).getPublisher());
    Assert.assertEquals("Google Buzz API",
        deserialized1.get(0).getDescription());
    Assert.assertEquals("pretty name", deserialized1.get(1).getDisplayName());
    Assert.assertEquals(new URL("http://link/to/tos"),
        deserialized1.get(1).getTosLink());
    Assert.assertEquals(new URL("http://link/to/release/notes"),
        deserialized1.get(1).getReleaseNotesLink());
    Assert.assertEquals(new URL("http://link/to/docs"),
        deserialized1.get(0).getDocumentationLink());
    Assert.assertEquals(new LocalDate(2010, 1, 14),
        deserialized1.get(0).getReleaseDate());
    Assert.assertTrue(deserialized1.get(1).getIconLinkKeys().contains("32x32"));
    Assert.assertEquals(new URL("http://icons.google.com/someicon.png"),
        deserialized1.get(1).getIconLink("32x32"));

    String jsonSampleData2 = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_API_List_2.json"));
    ApiDirectoryListing deserialized2 = codec.toApiDirectoryListing(
        jsonSampleData2, null);
    Assert.assertEquals(3, deserialized2.getItems().length);

    String jsonSampleData3 = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_API_List_3.json"));
    ApiDirectoryListing deserialized3 = codec.toApiDirectoryListing(
        jsonSampleData3, null);
    Assert.assertEquals(21, deserialized3.getItems().length);
  }

  @Test
  public void testSerializeEntry() {
    String json = codec.toJson(ai_1, null);
    Assert.assertTrue(json.contains("\"kind\":\""
        + ApiDirectoryListingJsonCodec.DIRECTORY_ITEM_TYPE_KEY + "\""));
    Assert.assertTrue(json.contains("\"id\":\"zoo:v2\""));
    Assert.assertTrue(json.contains("\"name\":\"zoo\""));
    Assert.assertTrue(json.contains("\"version\":\"v2\""));
    Assert.assertTrue(json.contains("\"title\":\"Zoo\""));
    Assert.assertTrue(json.contains("\"publisher\":\"Not Google\""));
    Assert.assertTrue(json.contains("\"ranking\":0"));
    Assert.assertTrue(json.contains("\"description\":\"Zoo API used for Google API testing\""));
    Assert.assertTrue(json.contains("\"tosLink\":\"http://link/to/tos\""));
    Assert.assertTrue(json.contains("\"releaseDate\":\"2010-11-29\""));
    Assert.assertTrue(json.contains("\"releaseNotesLink\":\"http://link/to/release/notes\""));
    Assert.assertTrue(json.contains("\"discoveryLink\":\"http://link/to/discovery\""));
    Assert.assertTrue(json.contains("\"documentationLink\":\"http://link/to/docs\""));
    Assert.assertTrue(json.contains("\"icons\":{\"32x32\":\"http://link/to/icon32x32.png\"}"));
    Assert.assertTrue(json.contains("\"labels\":[\"labs\"]"));
    Assert.assertTrue(json.contains("\"preferred\":true"));
  }

  @Test
  public void testSerializeList() throws FileNotFoundException {
    String jsonSampleData = readFileIntoString(new File(
        "src/com/google/gdt/googleapi/core/Sample_API_List_1.json"));
    ApiDirectoryListing deserialized = codec.toApiDirectoryListing(
        jsonSampleData, null);
    String json = codec.toJson(deserialized, null);
    Assert.assertTrue(json.contains("\"version\":\"v1\""));
    Assert.assertTrue(json.contains("\"version\":\"v2beta1\""));
  }

}
