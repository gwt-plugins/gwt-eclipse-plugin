package com.google.gdt.eclipse.suite.launch;

import com.google.gdt.eclipse.core.NetworkUtilities;
import com.google.gdt.eclipse.core.launch.WebAppLaunchAttributes;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swtbot.swt.finder.SWTBot;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Test for WebAppLaunchDelegate
 * 
 */
public class WebAppLaunchDelegateTest extends TestCase {

  private static SWTBot bot;

  /**
   * Set up the bot
   * 
   */
  @Override
  public void setUp() {
    bot = new SWTBot();
  }

  /**
   * Test when auto port selection is on promptUserToContinueIfPortNotAvailable
   * return true (continue with launch, ignore port information)
   * 
   * @throws CoreException
   */
  public void testAutoPortSelection() throws CoreException {
    final MockLaunchConfiguration config = new MockLaunchConfiguration();
    config.setAttribute(WebAppLaunchAttributes.AUTO_PORT_SELECTION, Boolean.TRUE);
    WebAppLaunchDelegate delegate = new WebAppLaunchDelegate();
    assertTrue(delegate.promptUserToContinueIfPortNotAvailable(config));
  }

  /**
   * Test when a port is in use and user explicitly continues
   * promptUserToContinueIfPortNotAvailable return true (continue with launch)
   * 
   * @throws CoreException
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException
   * @throws IOException
   */
  public void testPortInUseAndContinue() throws CoreException, InterruptedException,
      ExecutionException, TimeoutException, IOException {
    assertTrue(checkPortConflictUI("Yes"));
  }

  /**
   * Test when a port is in use and user cancels
   * promptUserToContinueIfPortNotAvailable return false (abort launch)
   * 
   * @throws CoreException
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException
   * @throws IOException
   */
  public void testPortInUseAndDontContinue() throws CoreException, InterruptedException,
      ExecutionException, TimeoutException, IOException {
    assertFalse(checkPortConflictUI("No"));
  }

  /**
   * Test when a port is not in use and promptUserToContinueIfPortNotAvailable
   * returns true (continue with launch)
   * 
   * @throws CoreException
   */
  public void testPortNotInUse() throws CoreException {
    final MockLaunchConfiguration config = new MockLaunchConfiguration();
    for (int i = 3000; i < 3500; i++) {
      String port = Integer.toString(i);
      if (NetworkUtilities.isPortAvailable(port)) {
        config.setAttribute(WebAppLaunchAttributes.SERVER_PORT, port);
        config.setAttribute(WebAppLaunchAttributes.AUTO_PORT_SELECTION, Boolean.FALSE);
        WebAppLaunchDelegate delegate = new WebAppLaunchDelegate();
        assertTrue(delegate.promptUserToContinueIfPortNotAvailable(config));
        return;
      }
    }
    fail("Test failed because we couldn't find an open port to test with");
  }

  /**
   * Internal function to check the result when a port is in use and a specific
   * action is taken on the UI
   * 
   * @param buttonToClick the string representing the button to click (Yes, No)
   * @return the result from the UI element
   * @throws ExecutionException If the thread ends with an exception
   * @throws InterruptedException
   * @throws TimeoutException If getting the value back from UI Widget takes too
   *           long
   * @throws IOException If an exception occurred with the socket (port not
   *           available)
   */
  private boolean checkPortConflictUI(String buttonToClick) throws InterruptedException,
      ExecutionException, TimeoutException, IOException {
    final MockLaunchConfiguration config = new MockLaunchConfiguration();
    ServerSocket ss = null;
    try {
      ss = new ServerSocket(0);
      String port = (new Integer(ss.getLocalPort())).toString();
      config.setAttribute(WebAppLaunchAttributes.SERVER_PORT, port);
      config.setAttribute(WebAppLaunchAttributes.AUTO_PORT_SELECTION, Boolean.FALSE);

      // this needs to run in separate thread because promptUser... is a
      // blocking call
      final Callable<Boolean> promptCallable = new Callable<Boolean>() {
        public Boolean call() throws CoreException {
          WebAppLaunchDelegate delegate = new WebAppLaunchDelegate();
          return delegate.promptUserToContinueIfPortNotAvailable(config);
        }
      };
      ExecutorService executor = Executors.newSingleThreadExecutor();
      Future<Boolean> result = executor.submit(promptCallable);

      // since the prompt is on another thread, allow it some time to come up
      // before trying to activate it
      bot.sleep(1000);
      bot.shell("Port in Use").activate();
      bot.button(buttonToClick).click();

      // get the result but timeout if something goes wrong after a second
      return result.get(1, TimeUnit.SECONDS).booleanValue();
    } finally {
      if (ss != null) {
        try {
          ss.close();
        } catch (IOException ioe) {
          // probably shouldn't be here
        }
      }
    }
  }
}
