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
package com.google.gdt.eclipse.gph.util;

import com.google.gdt.eclipse.gph.ProjectHostingUIPlugin;
import com.google.gdt.eclipse.gph.model.GPHProject;

import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

/**
 * 
 * 
 * 
 */
public class ModelLabelProvider extends DelegatingStyledCellLabelProvider
    implements ILabelProvider {

  private static class StyledLabelProvider extends LabelProvider implements
      DelegatingStyledCellLabelProvider.IStyledLabelProvider {

    public StyledLabelProvider() {
    }

    @Override
    public Image getImage(Object element) {
      if (element instanceof GPHProject) {
        return ProjectHostingUIPlugin.getImage("projecthosting-16.png");
      }

      return super.getImage(element);
    }

    public StyledString getStyledText(Object element) {
      String mainText = getMainText(element);
      String secondaryText = getSecondaryText(element);

      StyledString str = new StyledString();

      str.append(mainText);

      if (secondaryText != null) {
        str.append(secondaryText, StyledString.DECORATIONS_STYLER);
      }

      return str;
    }

    private String getMainText(Object element) {
      if (element instanceof GPHProject) {
        GPHProject project = (GPHProject) element;

        return project.getName();
      }

      return super.getText(element);
    }

    private String getSecondaryText(Object element) {
      if (element instanceof GPHProject) {
        GPHProject project = (GPHProject) element;

        if (project.getDomain() != null) {
          return " [" + project.getScmType() + " - " + project.getDomain()
              + "]";
        } else {
          return " [" + project.getScmType() + "]";
        }
      }

      return null;
    }
  }

  public ModelLabelProvider() {
    super(new StyledLabelProvider());
  }

  @Override
  public Image getImage(Object element) {
    StyledLabelProvider styledProvider = (StyledLabelProvider) getStyledStringProvider();

    return styledProvider.getImage(element);
  }

  public String getText(Object element) {
    StyledLabelProvider styledProvider = (StyledLabelProvider) getStyledStringProvider();

    return styledProvider.getMainText(element);
  }

}
