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
package com.google.gwt.eclipse.core.editors.java;

import com.google.gwt.eclipse.core.GWTPluginLog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.jdt.internal.ui.text.spelling.JavaSpellingReconcileStrategy;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.spelling.ISpellingEngine;
import org.eclipse.ui.texteditor.spelling.SpellingEngineDescriptor;
import org.eclipse.ui.texteditor.spelling.SpellingReconcileStrategy;
import org.eclipse.ui.texteditor.spelling.SpellingService;

import java.lang.reflect.Field;

/**
 * Spell checking strategy that always uses the {@link GWTSpellingEngine}.
 */
@SuppressWarnings("restriction")
public class GWTJavaSpellingReconcileStrategy extends
    JavaSpellingReconcileStrategy {

  /**
   * Dummy {@link IConfigurationElement}.
   * <p>
   * This interface has new methods added in E36. E33-E35 warn those methods are
   * unused, so making it static prevents the warning.
   */
  static class DummyConfigurationElement implements
      IConfigurationElement {
    @Override
    public Object createExecutableExtension(String propertyName)
        throws CoreException {
      return null;
    }

    @Override
    public String getAttribute(String name)
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getAttribute(String attrName, String locale)
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getAttributeAsIs(String name)
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String[] getAttributeNames() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public IConfigurationElement[] getChildren()
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public IConfigurationElement[] getChildren(String name)
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public IContributor getContributor() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public IExtension getDeclaringExtension()
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getName() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getNamespace() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getNamespaceIdentifier()
        throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public Object getParent() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getValue() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getValue(String locale) throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public String getValueAsIs() throws InvalidRegistryObjectException {
      return null;
    }

    @Override
    public boolean isValid() {
      return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IConfigurationElement#getHandleId()
     */
    @Override
    public int getHandleId() {
      // TODO(${user}): Auto-generated method stub
      return 0;
    }
  }

  private static class GWTSpellingService extends SpellingService {

    public static SpellingService spellingService;

    public static SpellingService getSpellingService() {
      if (spellingService == null) {
        spellingService = new GWTSpellingService();
      }
      return spellingService;
    }

    private GWTSpellingService() {
      super(EditorsPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public SpellingEngineDescriptor getActiveSpellingEngineDescriptor(
        IPreferenceStore preferences) {
      // Create dummy IConfigurationElement subclass so we have a non-null
      // instance to pass to the ctor of the anonymous subclass of
      // SpellingEngineDescriptor below
      IConfigurationElement configElement = new DummyConfigurationElement();

      // Dummy descriptor that always creates our spelling engine
      return new SpellingEngineDescriptor(configElement) {
        @Override
        public ISpellingEngine createEngine() throws CoreException {
          return new GWTSpellingEngine();
        }
      };
    }
  }

  public GWTJavaSpellingReconcileStrategy(ISourceViewer viewer,
      ITextEditor editor) {
    super(viewer, editor);

    try {
      // Reflectively set the spelling service to our own
      Field spellingServiceField = SpellingReconcileStrategy.class.getDeclaredField("fSpellingService");
      spellingServiceField.setAccessible(true);
      spellingServiceField.set(this, GWTSpellingService.getSpellingService());
    } catch (Exception e) {
      GWTPluginLog.logError(e);
    }
  }

}
