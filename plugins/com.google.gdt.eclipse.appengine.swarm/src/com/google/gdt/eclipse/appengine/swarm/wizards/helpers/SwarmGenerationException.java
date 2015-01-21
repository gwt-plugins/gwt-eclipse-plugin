/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.gdt.eclipse.appengine.swarm.wizards.helpers;

import java.lang.reflect.InvocationTargetException;

/**
 * This exception is thrown in case of generation error to separate generation errors.
 */
public class SwarmGenerationException extends Exception {

  private static final long serialVersionUID = 5739164755412748605L;

  public SwarmGenerationException() {
    super();
  }

  public SwarmGenerationException(InvocationTargetException cause) {
    super(cause.getCause());
  }

  public SwarmGenerationException(String message) {
    super(message);
  }

  public SwarmGenerationException(String message, InvocationTargetException cause) {
    super(message, cause.getCause());
  }

  public SwarmGenerationException(String message, Throwable cause) {
    super(message, cause);
  }

  public SwarmGenerationException(Throwable cause) {
    super(cause);
  }
}
