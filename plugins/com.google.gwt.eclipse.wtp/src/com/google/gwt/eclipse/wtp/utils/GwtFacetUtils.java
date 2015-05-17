package com.google.gwt.eclipse.wtp.utils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;

import com.google.gwt.eclipse.wtp.GwtWtpPlugin;
import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

public class GwtFacetUtils {

  public static boolean hasGwtFacet(IProject project) {
    boolean hasFacet = false;
    try {
      hasFacet = FacetedProjectFramework.hasProjectFacet(project, IGwtFacetConstants.GWT_FACET_ID);
    } catch (CoreException e) {
      GwtWtpPlugin.logError("hasGetFacet: Error, can't figure GWT facet.", e);
    }

    return hasFacet;
  }
  
}
