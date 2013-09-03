/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
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
package com.google.appengine.eclipse.wtp.jpa;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Invokes the JPA class synchronization (with persistence.xml) action on the Eclipse 4.3 platform.
 * 
 * The name and package of this class must match that declared in the Eclipse 4.2 version of the JPA
 * plugin.
 */
public class SynchronizeClassesRunner implements ISynchronizeClassesRunner {

  @Override
  public void syncClasses(IFile persistenceXml, IProgressMonitor monitor) {
    try {
      Class<?> clazz = Class.forName("org.eclipse.jpt.jpa.ui.internal.handlers.SynchronizeClassesHandler$SyncRunnable");
      Constructor<?> ctor = clazz.getDeclaredConstructor(IFile.class);
      ctor.setAccessible(true);
      Object instance = ctor.newInstance(persistenceXml);     
      
      // copied from SynchronizeClassesHandler.execute(...)
      // true ==> fork; true ==> cancellable 
      new ProgressMonitorDialog(null).run(true, true, (IRunnableWithProgress) instance);
    } catch (ClassNotFoundException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (NoSuchMethodException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (SecurityException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (InstantiationException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (IllegalAccessException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (IllegalArgumentException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (InvocationTargetException e) {
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
    } catch (InterruptedException e) {
      // copied from SynchronizeClassesHandler.execute(...)
      AppEngineJpaPlugin.logMessage("Failed to synchronize classes with " + persistenceXml.getFullPath() + ".", e);
      Thread.currentThread().interrupt();
    }
  }

}
