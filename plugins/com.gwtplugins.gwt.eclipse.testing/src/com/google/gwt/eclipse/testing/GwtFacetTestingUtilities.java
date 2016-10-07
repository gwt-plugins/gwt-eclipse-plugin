/**
 *
 */
package com.google.gwt.eclipse.testing;

import com.google.gwt.eclipse.wtp.facet.data.IGwtFacetConstants;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.wst.common.project.facet.core.FacetedProjectFramework;

public class GwtFacetTestingUtilities {

  public static boolean hasGwtFacet(IProject project) throws CoreException {
    return FacetedProjectFramework.hasProjectFacet(project, IGwtFacetConstants.GWT_PLUGINS_FACET_ID);
  }

}
