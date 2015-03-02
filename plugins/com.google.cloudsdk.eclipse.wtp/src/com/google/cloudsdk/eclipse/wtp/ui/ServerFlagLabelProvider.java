/*******************************************************************************
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package com.google.cloudsdk.eclipse.wtp.ui;

import com.google.cloudsdk.eclipse.wtp.server.ServerFlagsInfo.Flag;

import org.eclipse.jface.viewers.LabelProvider;

/**
 * A label provider implementation for {@link Flag}.
 */
public class ServerFlagLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      if (element instanceof Flag) {
        Flag variable = (Flag) element;
        return variable.getName();
      }
      return super.getText(element);
    }
}
