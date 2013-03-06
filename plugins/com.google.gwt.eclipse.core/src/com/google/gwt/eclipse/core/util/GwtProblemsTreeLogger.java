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
package com.google.gwt.eclipse.core.util;

import com.google.gwt.core.ext.TreeLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects GWT errors/warnings in flattened (non-hierarchical) form.
 */
public class GwtProblemsTreeLogger extends TreeLogger {

  private final List<String> errors = new ArrayList<String>();

  private final List<String> warnings = new ArrayList<String>();

  @Override
  public TreeLogger branch(TreeLogger.Type type, String msg, Throwable caught,
      HelpInfo helpInfo) {
    return this;
  }

  public List<String> getErrors() {
    return errors;
  }

  public List<String> getWarnings() {
    return warnings;
  }

  public boolean hasProblems() {
    return (getErrors().size() > 0 || getWarnings().size() > 0);
  }

  @Override
  public boolean isLoggable(TreeLogger.Type type) {
    return (!type.isLowerPriorityThan(TreeLogger.WARN));
  }

  @Override
  public void log(Type type, String msg, Throwable caught, HelpInfo helpInfo) {
    switch (type) {
      case ERROR:
        errors.add(msg);
        break;
      case WARN:
        warnings.add(msg);
        break;
    }
  }
}