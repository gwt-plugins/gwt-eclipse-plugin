package com.google.gdt.eclipse.core.browser;

public class LaunchMenuModel {

  private String id;
  private String menuLabel;
  private String browserName;
  private String url;
  private String debugMode;

  public LaunchMenuModel(String menuLabel, String browserName, String url) {
    this(menuLabel, browserName, url, "debug");
  }

  public LaunchMenuModel(String menuLabel, String browserName, String url, String debugMode) {
    this.menuLabel = menuLabel;
    this.browserName = browserName;
    this.url = url;
    this.debugMode = debugMode;

    id = browserName + "_" + menuLabel;
  }

  /**
   * @return the menuLabel
   */
  public String getMenuLabel() {
    return menuLabel;
  }

  /**
   * @return the browserName
   */
  public String getBrowserName() {
    return browserName;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @return the debugMode
   */
  public String getDebugMode() {
    return debugMode;
  }

  public String getId() {
    return id;
  }

}
