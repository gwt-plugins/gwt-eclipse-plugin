package com.google.gwt.eclipse.core.util;
import com.google.gdt.eclipse.core.CorePluginLog;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;

public class GwtFacetUtils {

  /**
   * Returns if this project has a GWT facet. TODO use extension point to get query GwtWtpPlugin...
   *
   * @param project
   * @return if the project has a GWT facet.
   */
  public static boolean hasGwtFacet(IProject project) {
    boolean hasFacet = false;
    try {
      hasFacet = FacetedProjectFramework.hasProjectFacet(project, "com.gwtplugins.gwt.facet");
    } catch (CoreException e) {
      CorePluginLog.logInfo("hasGetFacet: Error, can't figure GWT facet.", e);
    }

    return hasFacet;
  }

}
