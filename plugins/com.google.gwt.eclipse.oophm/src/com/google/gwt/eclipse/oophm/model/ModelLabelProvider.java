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
package com.google.gwt.eclipse.oophm.model;

import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.eclipse.core.speedtracer.SpeedTracerLaunchConfiguration;
import com.google.gwt.eclipse.oophm.Activator;
import com.google.gwt.eclipse.oophm.DevModeImages;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

/**
 * A label provider for the model objects. This provider is used by the
 * TreeNavigationView (com.google.gwt.eclipse.oophm.views.hierarchical.TreeNavigationView)
 * and the BreadcrumbNavigationView.
 */
public class ModelLabelProvider extends ColumnLabelProvider implements
    ILabelProvider {

  @Override
  public Font getFont(Object element) {
    if (element instanceof IModelNode) {
      IModelNode modelNode = (IModelNode) element;
      if (modelNode.isTerminated()) {
        return JFaceResources.getFontRegistry().getItalic(
            JFaceResources.DEFAULT_FONT);
      }
    }
    return null;
  }

  @Override
  public Image getImage(Object element) {
    if (element instanceof LaunchConfiguration) {
      return getLaunchConfigurationImage((LaunchConfiguration) element);      
    } else if (element instanceof BrowserTab) {
      return getBrowserTabImage((BrowserTab) element);
    }

    return null;
  }
  
  @Override
  public String getText(Object element) {
    assert (element instanceof IModelNode);
    IModelNode modelNode = (IModelNode) element;
    return modelNode.getName();
  }

  private Image getBrowserTabImage(BrowserTab browserTab) {
    String imageId = DevModeImages.WEB_BROWSER;
    if (browserTab.isTerminated()) {
      imageId = DevModeImages.WEB_BROWSER_TERMINATED;
    } else {
      String attentionLevel = browserTab.getNeedsAttentionLevel();
      if (attentionLevel != null) {
        Type logLevel = LogEntry.toTreeLoggerType(attentionLevel);
        if (logLevel == Type.ERROR) {
          imageId = DevModeImages.WEB_BROWSER_ERROR;
        } else if (logLevel == Type.WARN) {
          imageId = DevModeImages.WEB_BROWSER_WARNING;
        }
      }
    }
    
    return Activator.getDefault().getImage(imageId);
  }

  private Image getLaunchConfigurationImage(LaunchConfiguration launchConfiguration) {
    String launchConfigType = launchConfiguration.getLaunchTypeId();
    String imageId;

    if (SpeedTracerLaunchConfiguration.TYPE_ID.equals(launchConfigType)) {
      imageId = launchConfiguration.isTerminated()
          ? DevModeImages.SPEED_TRACER_ICON_TERMINATED
          : DevModeImages.SPEED_TRACER_ICON;
      
    } else {
      imageId = DevModeImages.GDT_ICON;

      if (launchConfiguration.isTerminated()) {
        imageId = DevModeImages.GDT_ICON_TERMINATED;
      } else {
        String attentionLevel = launchConfiguration.getNeedsAttentionLevel();
        if (attentionLevel != null) {
          Type logLevel = LogEntry.toTreeLoggerType(attentionLevel);
          if (logLevel == Type.ERROR) {
            imageId = DevModeImages.GDT_ICON_ERROR;
          } else if (logLevel == Type.WARN) {
            imageId = DevModeImages.GDT_ICON_WARNING;
          }
        }
      }
    }

    return Activator.getDefault().getImage(imageId);
  }
}
