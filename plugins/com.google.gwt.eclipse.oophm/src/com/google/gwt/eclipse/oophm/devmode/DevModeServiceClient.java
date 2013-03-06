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
package com.google.gwt.eclipse.oophm.devmode;

import com.google.gwt.dev.shell.remoteui.MessageTransport;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Request.DevModeRequest.RequestType;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.DevModeResponse;
import com.google.gwt.dev.shell.remoteui.RemoteMessageProto.Message.Response.DevModeResponse.CapabilityExchange.Capability;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A client that interacts with the Development Mode Service.
 */
public class DevModeServiceClient {

  /**
   * Given a list of capabilities and a particular capability, see if the
   * capability exists in the list of capabilities.
   * 
   * @return true if the given capability exists in the list, false otherwise
   */
  public static boolean checkCapability(
      List<Capability> devModeCapaibilityList, RequestType capabilityWeNeed) {
    for (Capability c : devModeCapaibilityList) {
      if (c.getCapability() == capabilityWeNeed) {
        return true;
      }
    }
    return false;
  }

  private final MessageTransport transport;

  /**
   * Create a new instance bound to the given transport.
   */
  public DevModeServiceClient(MessageTransport transport) {
    this.transport = transport;
  }

  /**
   * Determine the capabilities of the Development Mode Service.
   * 
   * @return a list of capabilities that the Development Mode Service has
   */
  public List<Capability> checkCapabilities() {
    DevModeRequest.CapabilityExchange.Builder capabilityExchangeBuilder = DevModeRequest.CapabilityExchange.newBuilder();
    DevModeRequest.Builder viewerRequestBuilder = DevModeRequest.newBuilder();
    viewerRequestBuilder.setRequestType(DevModeRequest.RequestType.CAPABILITY_EXCHANGE);
    viewerRequestBuilder.setCapabilityExchange(capabilityExchangeBuilder);

    Request.Builder request = buildRequestMessageFromDevModeRequest(viewerRequestBuilder);

    Future<Response> responseFuture = transport.executeRequestAsync(request.build());
    Response response = waitForResponse(responseFuture);

    DevModeResponse.CapabilityExchange capabilityExchangeResponse = response.getDevModeResponse().getCapabilityExchange();
    return capabilityExchangeResponse.getCapabilitiesList();
  }

  /**
   * Request that the web server be restarted. Assumes that the Development Mode
   * Service supports this capability.
   */
  public void restartWebServer() {
    DevModeRequest.RestartWebServer.Builder restartWebServerBuilder = DevModeRequest.RestartWebServer.newBuilder();
    DevModeRequest.Builder devModeRequestBuilder = DevModeRequest.newBuilder();
    devModeRequestBuilder.setRequestType(DevModeRequest.RequestType.RESTART_WEB_SERVER);
    devModeRequestBuilder.setRestartWebServer(restartWebServerBuilder);

    Request.Builder request = buildRequestMessageFromDevModeRequest(devModeRequestBuilder);

    Future<Response> responseFuture = transport.executeRequestAsync(request.build());
    waitForResponse(responseFuture);
  }

  private Request.Builder buildRequestMessageFromDevModeRequest(
      DevModeRequest.Builder devModeRequestBuilder) {
    return Request.newBuilder().setServiceType(Request.ServiceType.DEV_MODE).setDevModeRequest(
        devModeRequestBuilder);
  }

  private Response waitForResponse(Future<Response> future) {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
