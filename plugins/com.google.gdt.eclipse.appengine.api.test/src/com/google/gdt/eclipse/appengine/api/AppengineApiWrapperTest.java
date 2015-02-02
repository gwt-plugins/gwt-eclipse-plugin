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
package com.google.gdt.eclipse.appengine.api;

import static org.mockito.Mockito.when;

import com.google.api.services.appengine.v1beta2.Appengine;
import com.google.api.services.appengine.v1beta2.Appengine.Apps;
import com.google.api.services.appengine.v1beta2.model.App;
import com.google.api.services.appengine.v1beta2.model.AppsListResponse;

import junit.framework.TestCase;

import org.junit.Assert;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link AppengineApiWrapper}.
 */
public class AppengineApiWrapperTest extends TestCase {
  /**
   * Mock class for AppengineApiWrapper.
   */
  public class AppengineApiWrapperMock extends AppengineApiWrapper {
    /**
     * The constructor
     */
    public AppengineApiWrapperMock() {
      appengine = mockAppengine;
      inTestMode = true;
    }
  }

  @Mock
  private Apps mockApps;

  @Mock
  private Appengine.Apps.List mockAppsDotList;

  @Mock
  private Appengine mockAppengine;

  @Mock
  private AppengineApiWrapperMock mockAppengineApiWrapper;

  private List<App> appsList;

  /**
   * Tests that {@link AppengineApiWrapper#getApplications(boolean)} returns all applications.
   */
  public void testGetApplications_appsAvailable() {
    String[] actuals = null;

    // Create apps
    App app1 = new App();
    app1.setAppId("App1");

    App app2 = new App();
    app2.setAppId("App2");

    App app3 = new App();
    app3.setAppId("App3");

    // Add apps
    appsList.add(app1);
    appsList.add(app2);
    appsList.add(app3);

    // Call getApplications()
    try {
      actuals = mockAppengineApiWrapper.getApplications(true);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    String[] expected = {"App1", "App2", "App3"};
    Assert.assertArrayEquals(expected, actuals);

  }

  /**
   * Tests that {@link AppengineApiWrapper#getApplications(boolean)} returns an empty array when
   * there are no applications.
   */
  public void testGetApplications_noApps() {
    String[] actuals = null;

    try {
      actuals = mockAppengineApiWrapper.getApplications(true);
    } catch (IOException e) {
      fail(e.getMessage());
    }
    String[] expected = {};
    Assert.assertArrayEquals(expected, actuals);
  }

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockAppengineApiWrapper = new AppengineApiWrapperMock();
    AppsListResponse listAppResponse = new AppsListResponse();
    appsList = new ArrayList<App>();
    listAppResponse.setApps(appsList);

    // Mocks for listApps();
    when(mockAppengine.apps()).thenReturn(mockApps);
    when(mockApps.list()).thenReturn(mockAppsDotList);
    when(mockAppsDotList.execute()).thenReturn(listAppResponse);
  }

  @Override
  protected void tearDown() throws Exception {
    Mockito.validateMockitoUsage();
  }
}
