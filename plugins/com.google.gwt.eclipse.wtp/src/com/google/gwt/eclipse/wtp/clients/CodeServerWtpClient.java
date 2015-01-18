/**
 *
 */
package com.google.gwt.eclipse.wtp.clients;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ClientDelegate;

/**
 * @author branflake2267
 *
 */
public class CodeServerWtpClient extends ClientDelegate {

  /* (non-Javadoc)
   * @see org.eclipse.wst.server.core.model.ClientDelegate#launch(org.eclipse.wst.server.core.IServer, java.lang.Object, java.lang.String, org.eclipse.debug.core.ILaunch)
   */
  @Override
  public IStatus launch(IServer server, Object launchable, String launchMode, ILaunch launch) {
    // TODO(${user}): Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.wst.server.core.model.ClientDelegate#supports(org.eclipse.wst.server.core.IServer, java.lang.Object, java.lang.String)
   */
  @Override
  public boolean supports(IServer server, Object launchable, String launchMode) {
    // TODO(${user}): Auto-generated method stub
    return super.supports(server, launchable, launchMode);
  }



}
