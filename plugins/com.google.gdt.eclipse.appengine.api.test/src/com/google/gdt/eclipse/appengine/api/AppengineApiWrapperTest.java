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
package com.google.gdt.eclipse.appengine.api;

import com.google.api.services.appengine.Appengine;
import com.google.api.services.appengine.Appengine.Apps;
import com.google.api.services.appengine.Appengine.Apps.Insert;
import com.google.api.services.appengine.model.App;
import com.google.api.services.appengine.model.InsertAppRequest;
import com.google.api.services.appengine.model.InsertAppResponse;
import com.google.api.services.appengine.model.ListAppResponse;

import junit.framework.TestCase;

import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

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
  private class AppengineApiWrapperMock extends AppengineApiWrapper {

    /**
     * The constructor
     */
    public AppengineApiWrapperMock() {
      appengine = mockAppengine;
      inTestMode = true;
    }
  }

  private class IsInsertAppRequest extends ArgumentMatcher<InsertAppRequest> {
    public boolean matches(Object obj) {
      if (obj instanceof InsertAppRequest) {
        return true;
      } else {
        return false;
      }
    }
  }

  @Mock
  private Apps mockApps;

  @Mock
  private Appengine.Apps.List mockAppsDotList;

  @Mock
  private Appengine mockAppengine;

  @Mock
  private Insert mockInsert;

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

  /**
   * Tests that applications added using
   * {@link AppengineApiWrapper#insertNewApplication(String, boolean)} can be retrieved using
   * {@link AppengineApiWrapper#getApplications(boolean)}.
   * 
   * NOTE: This test now verifies that an IllegalStateException is thrown whenever
   * insertNewApplication is called (as the inserting functionality no longer exists/works in the
   * App Engine API.
   */
  public void testInsertNewApplication() {
    try {
      mockAppengineApiWrapper.insertNewApplication("App4", true);
    } catch (IllegalStateException e) {
      Assert.assertEquals(e.getMessage(), "The ability to insert apps no longer exists.");
      return;
    } catch (IOException e) {
      // Should never happen, since we're expecting an IllegalStateException to be thrown
      fail(e.getMessage());
    }

    fail("Expected IllegalStateException to be thrown.");
  }

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mockAppengineApiWrapper = new AppengineApiWrapperMock();
    ListAppResponse listAppResponse = new ListAppResponse();
    appsList = new ArrayList<App>();
    listAppResponse.setApps(appsList);
    InsertAppResponse insertAppResponse = new InsertAppResponse();

    // Mocks for listApps();
    when(mockAppengine.apps()).thenReturn(mockApps);
    when(mockApps.list()).thenReturn(mockAppsDotList);
    when(mockAppsDotList.execute()).thenReturn(listAppResponse);
    // Mocks for insertApps()
    when(mockInsert.execute()).thenReturn(insertAppResponse);
    when(mockApps.insert(argThat(new IsInsertAppRequest()))).thenAnswer(new Answer<Insert>() {
      public Insert answer(InvocationOnMock invocation) {
        Object[] args = invocation.getArguments();
        InsertAppRequest insertAppRequest = (InsertAppRequest) args[0];
        appsList.add(insertAppRequest.getApp());
        return mockInsert;
      }
    });

  }

  @Override
  protected void tearDown() throws Exception {
    Mockito.validateMockitoUsage();
  }
}
