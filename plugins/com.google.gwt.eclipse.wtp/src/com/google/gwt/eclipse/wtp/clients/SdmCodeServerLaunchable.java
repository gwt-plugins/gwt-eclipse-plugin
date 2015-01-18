package com.google.gwt.eclipse.wtp.clients;

import java.util.Properties;

/**
 * A representation of an object in JNDI that can be tested on a server.
 * <p>
 * <b>Provisional API:</b> This class/interface is part of an interim API that is still under
 * development and expected to change significantly before reaching stability. It is being made
 * available at this early stage to solicit feedback from pioneering adopters on the understanding
 * that any code that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 * </p>
 *
 * @plannedfor 3.0
 */
public class SdmCodeServerLaunchable {
  private Properties props;
  private String jndiName;

  /**
   * Create a reference to an object that is launchable via JNDI.
   *
   * @param props the JNDI properties required to connect to the object
   * @param jndiName the JNDI name of the object
   */
  public SdmCodeServerLaunchable(Properties props, String jndiName) {
    this.jndiName = jndiName;
    this.props = props;
  }

  /**
   * Returns the JNDI properties required to connect to the object.
   *
   * @return the JNDI properties required to connect to the object
   */
  public Properties getProperties() {
    return props;
  }

  /**
   * Returns the JNDI name of the object.
   *
   * @return the JNDI name of the object
   */
  public String getJNDIName() {
    return jndiName;
  }
}
