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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.stream.JsonReader;

import org.joda.time.LocalDate;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A coder/decoder for API types including ApiInfo, PartialApiInfo
 * ApiDirectoryListing and PartialApiDirectoryListing types. Codes Java types
 * into/from JSON strings.
 */
public class ApiDirectoryListingJsonCodec {
  class ApiDirectoryListingTypeAdaptor implements
      JsonDeserializer<ApiDirectoryListing>,
      JsonSerializer<ApiDirectoryListing> {

    public ApiDirectoryListing deserialize(JsonElement element,
        Type typeOfElement, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject root = element.getAsJsonObject();
      ApiDirectoryListing listing = new ApiDirectoryListing();
      List<ApiDirectoryItem> entries = new LinkedList<ApiDirectoryItem>();
      if (root.has("kind")
          && DIRECTORY_LIST_TYPE_KEY.equals(root.get("kind").getAsString())
          && root.has("items")) {
        JsonArray items = root.getAsJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
          JsonElement item = items.get(i);
          ApiInfoImpl apiInfo = context.deserialize(item, ApiInfoImpl.class);
          if (apiInfo != null) {
            entries.add(new ApiDirectoryItem(apiInfo));
          }
        }
      }
      listing.setItems(entries);
      return listing;
    }

    public JsonElement serialize(ApiDirectoryListing src, Type typeOfSrc,
        JsonSerializationContext context) {
      JsonArray items = new JsonArray();
      for (ApiDirectoryItem entry : src.getItems()) {
        items.add(context.serialize(entry, ApiInfo.class));
      }

      JsonObject entry = new JsonObject();
      entry.addProperty("kind", DIRECTORY_LIST_TYPE_KEY);
      entry.add("items", items);
      return entry;
    }
  }
  class ApiInfoImplTypeAdaptor implements JsonDeserializer<ApiInfoImpl>,
      InstanceCreator<ApiInfoImpl> {

    public ApiInfoImpl createInstance(Type arg0) {
      return new ApiInfoImpl(null);
    }

    public ApiInfoImpl deserialize(JsonElement element, Type typeOfElement,
        JsonDeserializationContext context) throws JsonParseException {
      URL baseURL = baseURLThreadLocal.get();
      JsonObject object = element.getAsJsonObject();
      ApiInfoImpl info = null;
      if (object.has("kind")
          && DIRECTORY_ITEM_TYPE_KEY.equals(object.get("kind").getAsString())
          && object.has("id")) {
        info = new ApiInfoImpl(object.get("id").getAsString());
        populateApiInfoFromJson(baseURL, object, info);
        if (object.has("preferred")) {
          info.setPreferred(object.get("preferred").getAsBoolean());
        }
      }
      return info;
    }
  }
  class ApiInfoTypeAdaptor implements JsonSerializer<ApiInfo> {

    public JsonElement serialize(ApiInfo src, Type typeOfSrc,
        JsonSerializationContext context) {
      URL baseURL = baseURLThreadLocal.get();
      String baseHref = baseURL == null ? null : baseURL.toExternalForm();
      JsonObject entry = new JsonObject();
      entry.addProperty("kind", DIRECTORY_ITEM_TYPE_KEY);
      entry.addProperty("id", src.getIdentifier());
      populateJsonFromApiInfo(src, entry, baseHref);
      entry.addProperty("preferred", src.isPreferred());
      return entry;
    }
  }
  class IconCacheIndexAdaptor implements JsonDeserializer<Map<String, String>>,
      JsonSerializer<Map<String, String>> {

    public Map<String, String> deserialize(JsonElement element,
        Type typeOfElement, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject object = element.getAsJsonObject();

      Map<String, String> index = new HashMap<String, String>();
      for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
        index.put(entry.getKey(), entry.getValue().getAsString());
      }
      return index;
    }

    public JsonElement serialize(Map<String, String> mapping,
        Type typeOfElement, JsonSerializationContext context) {
      JsonObject index = new JsonObject();
      for (Map.Entry<String, String> entry : mapping.entrySet()) {
        index.addProperty(entry.getKey(), entry.getValue());
      }
      return index;
    }
  }

  class PartialApiInfoListingTypeAdaptor implements
      JsonDeserializer<PartialApiInfoListing> {

    public PartialApiInfoListing deserialize(JsonElement element,
        Type typeOfElement, JsonDeserializationContext context)
        throws JsonParseException {
      JsonObject root = element.getAsJsonObject();
      PartialApiInfoListing listing = new PartialApiInfoListing();
      List<PartialApiInfo> entries = new LinkedList<PartialApiInfo>();
      if (root.has("kind")
          && DIRECTORY_LIST_PARTIAL_TYPE_KEY.equals(root.get("kind").getAsString())
          && root.has("items")) {
        JsonArray items = root.getAsJsonArray("items");
        for (int i = 0; i < items.size(); i++) {
          JsonElement item = items.get(i);
          if (item.isJsonObject()) {
            JsonObject object = item.getAsJsonObject();
            if (DIRECTORY_ITEM_PARTIAL_TYPE_KEY.equals(object.get("kind").getAsString())) {
              PartialApiInfo apiInfo = context.deserialize(item,
                  PartialApiInfo.class);
              if (apiInfo != null) {
                entries.add(apiInfo);
              }
            }
          }
        }
      }
      listing.setItems(entries);
      return listing;
    }
  }

  class PartialApiInfoTypeAdaptor implements JsonDeserializer<PartialApiInfo> {

    public PartialApiInfo deserialize(JsonElement element, Type typeOfElement,
        JsonDeserializationContext context) throws JsonParseException {
      URL baseURL = baseURLThreadLocal.get();
      JsonObject object = element.getAsJsonObject();

      PartialApiInfo info = null;
      if (object.has("kind")
          && DIRECTORY_ITEM_PARTIAL_TYPE_KEY.equals(object.get("kind").getAsString())) {
        info = new PartialApiInfo();
        populateApiInfoFromJson(baseURL, object, info);
      }
      if (info.hasName()) {
        return info;
      } else {
        return null;
      }
    }
  }

  public static final String DIRECTORY_ITEM_PARTIAL_TYPE_KEY = "discovery#directoryItemPartial";

  public static final String DIRECTORY_ITEM_TYPE_KEY = "discovery#directoryItem";

  public static final String DIRECTORY_LIST_PARTIAL_TYPE_KEY = "discovery#directoryListPartial";

  public static final String DIRECTORY_LIST_TYPE_KEY = "discovery#directoryList";

  private ThreadLocal<URL> baseURLThreadLocal = new ThreadLocal<URL>();

  private Gson gson;

  public ApiDirectoryListingJsonCodec() {
    gson = new GsonBuilder().//
    registerTypeAdapter(ApiDirectoryListing.class,
        new ApiDirectoryListingTypeAdaptor()).//
    registerTypeAdapter(ApiInfo.class, new ApiInfoTypeAdaptor()).//
    registerTypeAdapter(ApiInfoImpl.class, new ApiInfoImplTypeAdaptor()).//
    registerTypeAdapter(PartialApiInfoListing.class,
        new PartialApiInfoListingTypeAdaptor()).//
    registerTypeAdapter(PartialApiInfo.class, new PartialApiInfoTypeAdaptor()).//
    registerTypeAdapter(Map.class, new IconCacheIndexAdaptor()).//
    create();
  }

  public ApiDirectoryListing toApiDirectoryListing(InputStream in, URL baseURL)
      throws UnsupportedEncodingException {
    ApiDirectoryListing retval = null;
    try {
      JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
      baseURLThreadLocal.set(baseURL);
      retval = gson.fromJson(reader, ApiDirectoryListing.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  public ApiDirectoryListing toApiDirectoryListing(String json, URL baseURL) {
    ApiDirectoryListing retval = null;
    try {
      baseURLThreadLocal.set(baseURL);
      retval = gson.fromJson(json, ApiDirectoryListing.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  public ApiInfoImpl toApiInfo(InputStream in, URL baseURL)
      throws UnsupportedEncodingException {
    ApiInfoImpl retval = null;
    try {
      JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
      baseURLThreadLocal.set(baseURL);
      retval = gson.fromJson(reader, ApiInfoImpl.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  public ApiInfoImpl toApiInfo(String json, URL baseURL) {
    ApiInfoImpl retval = null;
    try {
      baseURLThreadLocal.set(baseURL);
      retval = gson.fromJson(json, ApiInfoImpl.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  public Map<String, String> toIconCacheMap(InputStream in) {
    try {
      JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
      return gson.fromJson(reader, Map.class);
    } catch (UnsupportedEncodingException e) {
      return Collections.emptyMap();
    }
  }

  public String toJson(ApiDirectoryListing listing, URL baseURL) {
    String retval = null;
    try {
      baseURLThreadLocal.set(baseURL);
      retval = gson.toJson(listing, ApiDirectoryListing.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  public String toJson(ApiInfo entry, URL baseURL) {
    String retval = null;
    try {
      baseURLThreadLocal.set(baseURL);
      retval = gson.toJson(entry, ApiInfo.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  public String toJson(Map<String, String> map) {
    return gson.toJson(map, Map.class);
  }

  public PartialApiInfoListing toPartialApiInfoListing(String json, URL baseURL) {
    PartialApiInfoListing retval = null;
    try {
      baseURLThreadLocal.set(baseURL);
      retval = gson.fromJson(json, PartialApiInfoListing.class);
    } finally {
      baseURLThreadLocal.set(null);
    }
    return retval;
  }

  protected void populateApiInfoFromJson(URL baseURL, JsonObject object,
      MutableApiInfo info) {
    if (object.has("name")) {
      info.setName(object.get("name").getAsString());
    }
    if (object.has("version")) {
      info.setVersion(object.get("version").getAsString());
    }
    if (object.has("title")) {
      info.setDisplayName(object.get("title").getAsString());
    }
    if (object.has("publisher")) {
      info.setPublisher(object.get("publisher").getAsString());
    }
    if (object.has("description")) {
      info.setDescription(object.get("description").getAsString());
    }
    if (object.has("icons")) {
      JsonObject iconLinks = object.getAsJsonObject("icons");
      Set<Entry<String, JsonElement>> iconLinksEntrySet = iconLinks.entrySet();
      for (Entry<String, JsonElement> entry : iconLinksEntrySet) {
        try {
          info.putIconLink(entry.getKey(), new URL(baseURL,
              entry.getValue().getAsString()));
        } catch (MalformedURLException e) {
          // TODO Add logging warn
        }
      }
    }
    if (object.has("labels")) {
      JsonArray labelsJsonArray = object.getAsJsonArray("labels");

      for (JsonElement labelElement : labelsJsonArray) {
        info.addLabel(labelElement.getAsString());
      }
    }
    if (object.has("releaseDate")) {
      try {
        LocalDate date = new LocalDate(object.get("releaseDate").getAsString());
        info.setReleaseDate(date);
      } catch (IllegalArgumentException e) {
        throw new JsonParseException(e);
      }
    }
    if (object.has("releaseNotesLink")) {
      try {
        info.setReleaseNotesLink(new URL(baseURL,
            object.get("releaseNotesLink").getAsString()));
      } catch (MalformedURLException e) {
        // TODO Add logging warn
      }
    }
    if (object.has("ranking")) {
      info.setRanking(object.get("ranking").getAsInt());
    }
    if (object.has("discoveryLink")) {
      try {
        info.setDiscoveryLink(new URL(baseURL,
            object.get("discoveryLink").getAsString()));
      } catch (MalformedURLException e) {
        // TODO Add logging warn
      }
    }
    if (object.has("documentationLink")) {
      try {
        info.setDocumentationLink(new URL(baseURL, object.get(
            "documentationLink").getAsString()));
      } catch (MalformedURLException e) {
        // TODO Add logging warn
      }
    }
    if (object.has("downloadLink")) {
      try {
        info.setDownloadLink(new URL(baseURL,
            object.get("downloadLink").getAsString()));
      } catch (MalformedURLException e) {
        // TODO Add logging warn
      }
    }
    if (object.has("tosLink")) {
      try {
        info.setTosLink(new URL(baseURL, object.get("tosLink").getAsString()));
      } catch (MalformedURLException e) {
        // TODO Add logging warn
      }
    }
  }

  protected void populateJsonFromApiInfo(ReadableApiInfo src, JsonObject entry,
      String baseHref) {
    if (null != src.getName()) {
      entry.addProperty("name", src.getName());
    }
    if (null != src.getVersion()) {
      entry.addProperty("version", src.getVersion());
    }
    if (null != src.getDisplayName()) {
      entry.addProperty("title", src.getDisplayName());
    }
    if (null != src.getPublisher()) {
      entry.addProperty("publisher", src.getPublisher());
    }
    String[] labels = src.getLabels();
    if (labels.length > 0) {
      JsonArray jsonLabels = new JsonArray();
      for (String label : labels) {
        jsonLabels.add(new JsonPrimitive(label));
      }
      entry.add("labels", jsonLabels);
    }
    entry.addProperty("ranking", src.getRanking());
    if (null != src.getDescription()) {
      entry.addProperty("description", src.getDescription());
    }
    Set<String> iconLinkRefs = src.getIconLinkKeys();
    if (!iconLinkRefs.isEmpty()) {
      JsonObject iconLinks = new JsonObject();
      for (String ref : iconLinkRefs) {
        URL link = src.getIconLink(ref);
        if (link == null) {
          break;
        }
        String linkStr = renderLink(link, baseHref);
        if (linkStr == null) {
          break;
        }
        iconLinks.addProperty(ref, linkStr);
      }
      entry.add("icons", iconLinks);
    }
    if (null != src.getTosLink()) {
      entry.addProperty("tosLink", src.getTosLink().toExternalForm());
    }
    if (null != src.getReleaseDate()) {
      entry.addProperty("releaseDate", src.getReleaseDate().toString());
    }
    if (null != src.getReleaseNotesLink()) {
      entry.addProperty("releaseNotesLink",
          src.getReleaseNotesLink().toExternalForm());
    }
    if (null != src.getDiscoveryLink()) {
      entry.addProperty("discoveryLink",
          src.getDiscoveryLink().toExternalForm());
    }
    if (null != src.getDocumentationLink()) {
      entry.addProperty("documentationLink",
          src.getDocumentationLink().toExternalForm());
    }
    if (null != src.getDownloadLink()) {
      entry.addProperty("downloadLink", src.getDownloadLink().toExternalForm());
    }
  }

  private String renderLink(URL link, String baseHref) {
    String linkStr = link.toExternalForm();
    if (baseHref != null && linkStr.startsWith(baseHref)) {
      linkStr = linkStr.substring(baseHref.length());
      // in some JVMs, the baseHREF ends with a '/' where others don't. This
      // hack addresses the variation.
      if (linkStr.length() > 0 && linkStr.charAt(0) == '/') {
        linkStr = linkStr.substring(1);
      }
    }
    return linkStr;
  }
}
