/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.ssh;

import com.google.gdt.eclipse.core.LogUtils;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.xfer.FileSystemFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * A class of static convinience methods for interacting with sshj
 */
public final class SSHUtils {

  /**
   * The stream gobbler is an asynchronous stream reader. It will route lines
   * from one stream to another, use with ssh functionality here to clear
   * buffers. Will redirect data to an output stream
   * 
   */
  private static class StreamGobbler implements Runnable {
    private final InputStream is;
    private final String prefix;
    private final OutputStream os;

    public StreamGobbler(InputStream is, String prefix, OutputStream dest) {
      this.is = is;
      this.prefix = prefix;
      this.os = dest;
    }

    public void run() {
      try {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = br.readLine()) != null) {
          os.write((prefix + line + "\n").getBytes());
        }
      } catch (IOException ioe) {
        LogUtils.logError(SSHPlugin.getInstance().getLog(), ioe);
      }
    }
  }

  /**
   * Helper for client authorization
   * 
   * @param client
   * @param username
   * @param password
   * @throws IOException
   * @throws UserAuthException
   */
  public static void authClient(SSHClient client, String username, char[] password)
      throws IOException, UserAuthException {
    client.authPassword(username, password);
  }

  /**
   * Helper for client authorization
   * 
   * @param client
   * @param username
   * @param pFinder
   * @throws IOException
   * @throws UserAuthException
   */
  public static void authClient(SSHClient client, String username, PasswordFinder pFinder)
      throws IOException, UserAuthException {
    client.authPassword(username, pFinder);
  }

  /**
   * Helper for client authorization
   * 
   * @param client
   * @param username
   * @param idFileLoc is a path to a private key file
   * @throws IOException
   * @throws UserAuthException
   */
  public static void authClient(SSHClient client, String username, String idFileLoc)
      throws IOException, UserAuthException {
    client.authPublickey(username, client.loadKeys(idFileLoc));
  }

  /**
   * Helper for client authorization
   * 
   * @param client
   * @param username
   * @param idFileLoc is a path to a private key file
   * @param passphrase
   * @throws IOException
   * @throws UserAuthException
   */
  public static void authClient(SSHClient client, String username, String idFileLoc,
      char[] passphrase) throws IOException, UserAuthException {
    client.authPublickey(username, client.loadKeys(idFileLoc, passphrase));
  }

  /**
   * Helper for client authorization
   * 
   * @param client
   * @param username
   * @param idFileLoc is a path to a private key file
   * @param finder is a password finder to obtain the passphrase from the user
   * @throws IOException
   * @throws UserAuthException
   */
  public static void authClient(SSHClient client, String username, String idFileLoc,
      PasswordFinder finder) throws IOException, UserAuthException {
    client.authPublickey(username, client.loadKeys(idFileLoc, finder));
  }

  /**
   * Helper to close an ssh client
   * 
   * @param ssh
   * @throws IOException
   */
  public static void closeClient(SSHClient ssh) throws IOException {
    if (ssh != null) {
      try {
        if (ssh.isConnected()) {
          ssh.disconnect();
        }
      } finally {
        ssh.close();
      }
    }
  }

  /**
   * Helper to create an ssh client, it WILL ignore hostkey fingerprinting
   * 
   * @param host
   * @return
   * @throws IOException
   */
  public static SSHClient createClient(String host) throws IOException {
    // TODO(appu): Don't just accept all hosts, possible security lapse, we need
    // to either add support to ECDSA to sshj or just ignore ECDSA requests only
    SSHClient ssh = new SSHClient();
    ssh.addHostKeyVerifier(new PromiscuousVerifier());
    ssh.connect(host);
    return ssh;
  }

  /**
   * Run a command remotely
   * 
   * To reduce what is being sent over the network, redirect to /dev/null for
   * input/output when those can be ignored, use & for background jobs, ssh will
   * return while command continues
   * 
   * @param ssh
   * @param command
   * @param outputDestination the output stream where stdout should be
   *          redirected
   * @param errDestination the output stream where stderr should be redirected
   * @return
   * @throws ConnectionException
   * @throws TransportException
   * @throws IOException
   */
  public static Integer runRemoteCommand(SSHClient ssh, String command,
      OutputStream outputDestination, OutputStream errDestination) throws ConnectionException,
      TransportException, IOException {
    final Session session = ssh.startSession();
    final Command cmd = session.exec(command);
    try {

      StreamGobbler inGobbler = new StreamGobbler(cmd.getInputStream(), "out: ", outputDestination);
      StreamGobbler errGobbler = new StreamGobbler(cmd.getErrorStream(), 
          "err: ", outputDestination);
      Thread inThread = new Thread(inGobbler);
      Thread errThread = new Thread(errGobbler);
      inThread.start();
      errThread.start();

      // wait for the stream gobbler threads to end
      // TODO(appu): what if they never end? put some kind of interrupting
      // timeout here, really look into making this make sense.
      try {
        inThread.join();
      } catch (InterruptedException ie) {
        // pass
      }
      try {
        errThread.join();
      } catch (InterruptedException ie) {
        // pass
      }
      cmd.join(10, TimeUnit.SECONDS); // this is a very silly timeout
      return cmd.getExitStatus();
    } finally {
      session.close();
    }
  }

  /**
   * SCP wrapper, will enable compression on the stream as a side effect.
   * 
   * @param ssh
   * @param src a path to the file on the local host
   * @param dest the path to the destination file on the remote host
   * @throws TransportException
   * @throws IOException
   */
  public static void scp(SSHClient ssh, String src, String dest) throws TransportException,
      IOException {
    ssh.useCompression();
    ssh.newSCPFileTransfer().upload(new FileSystemFile(src), dest);
  }

}
