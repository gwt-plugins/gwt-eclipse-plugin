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
package com.google.gwt.eclipse.oophm.launch;

import com.google.gwt.dev.shell.remoteui.MessageTransport;
import com.google.gwt.dev.shell.remoteui.MessageTransport.TerminationCallback;
import com.google.gwt.eclipse.oophm.devmode.ViewerServiceServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listens for and accepts a single connection from a remote server wanting to
 * dispatch UI events and receive commands.
 */
public class RemoteUIServer {
  private static RemoteUIServer INSTANCE;

  public synchronized static RemoteUIServer getInstance() throws IOException {
    if (INSTANCE == null) {
      INSTANCE = new RemoteUIServer();
      INSTANCE.start();
    }

    return INSTANCE;
  }

  private final ServerSocket serverSocket;
  private Thread connectionAcceptThread = null;
  private Object privateInstanceLock = new Object();

  private AtomicBoolean isStopped = new AtomicBoolean(false);

  /**
   * Create a new instance for the given launch configuration object. The
   * communicator will listen for a connection on <code>requestedPort</code>. If
   * <code>requestedPort</code> is set to <code>0</code>, then a port will
   * automatically chosen. The automatically-chosen port can be queried via
   * {@link #getPort()}.
   * 
   * @throws IOException if there was a problem while attempting to set up the
   *           server socket
   */
  private RemoteUIServer() throws IOException {
    this.serverSocket = new ServerSocket(0);
    this.serverSocket.setSoTimeout(2000);
  }

  /**
   * Get the port on which the communicator is listening for a connection.
   */
  public int getPort() {
    return serverSocket.getLocalPort();
  }

  /**
   * Stops the communicator. All open connections to the remote server will be
   * terminated. Once this method has been called, the communicator cannot be
   * restarted.
   */
  public void stop() {
    synchronized (privateInstanceLock) {
      if (connectionAcceptThread == null) {
        return;
      }
    }

    isStopped.set(true);

    try {
      connectionAcceptThread.interrupt();
      connectionAcceptThread.join();
    } catch (InterruptedException e) {
      // Ignore
    }

    try {
      serverSocket.close();
    } catch (IOException e) {
      // Ignore this exception
    }
  }

  /**
   * Start the communicator. Invoking this method causes the communicator to
   * start listening on the port given by {@link #getPort()} for a connection
   * from the remote server. Once a connection has been made, the listening port
   * will be closed (i.e. only one connection is allowed).
   * 
   * @throws IllegalStateException if the {@link RemoteUIServer} has already
   *           been started, or if {@link #stop()} has been called
   */
  private void start() {
    // Check to see if we've already stopped the communicator
    if (isStopped.get()) {
      throw new IllegalStateException(getClass().getName() + " on port "
          + serverSocket.getLocalPort() + " has already been stopped.");
    }

    synchronized (privateInstanceLock) {
      // Check to see if the communicator has already been started
      if (connectionAcceptThread != null) {
        throw new IllegalStateException(getClass().getName() + " on port "
            + serverSocket.getLocalPort() + " has already been started.");
      }

      // Create the connection acceptance thread
      connectionAcceptThread = new Thread() {
        @Override
        public void run() {
          /*
           * Wait 2 seconds for a connection request. If no request comes in by
           * that time, throw a SocketTimeoutException, and check and see if the
           * communicator has been terminated. If not, repeat the process.
           */
          while (!isStopped.get()) {
            try {
              final Socket acceptSocket = serverSocket.accept();
              if (acceptSocket != null) {
                // Do something here...
                ViewerServiceServer viewerServiceServer = new ViewerServiceServer();
                MessageTransport transport = new MessageTransport(
                    acceptSocket.getInputStream(),
                    acceptSocket.getOutputStream(), viewerServiceServer,
                    new TerminationCallback() {
                      public void onTermination(Exception e) {
                        try {
                          acceptSocket.close();
                        } catch (IOException ioe) {
                          // Ignore
                        }
                      }
                    });
                viewerServiceServer.setTransport(transport);
                transport.start();
              }
            } catch (SocketTimeoutException e) {
              // Ignore and try again
            } catch (IOException e) {
              // Terminate the thread
              isStopped.set(true);
              return;
            }
          }
        }
      };
    } // end synchronized(privateInstanceLock)

    // Start the connection acceptance thread
    connectionAcceptThread.start();
  }
}
