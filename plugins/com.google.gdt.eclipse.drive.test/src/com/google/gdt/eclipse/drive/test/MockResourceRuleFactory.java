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
package com.google.gdt.eclipse.drive.test;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

/**
 * A mock implementation of {@link IResourceRuleFactory} that returns only null resource rules and
 * does not depend on an open workspace.
 */
public class MockResourceRuleFactory implements IResourceRuleFactory {
  
    @Override public ISchedulingRule createRule(IResource resource) { return null; }
    @Override public ISchedulingRule buildRule() { return null; }
    @Override public ISchedulingRule charsetRule(IResource resource) { return null; }
    @Override public ISchedulingRule derivedRule(IResource resource) { return null; }
    @Override public ISchedulingRule deleteRule(IResource resource) { return null; }
    @Override public ISchedulingRule markerRule(IResource resource) { return null; }
    @Override public ISchedulingRule modifyRule(IResource resource) { return null; }
    @Override public ISchedulingRule refreshRule(IResource resource) { return null; }
    @Override public ISchedulingRule validateEditRule(IResource[] resources) { return null; }
    
    @Override
    public ISchedulingRule copyRule(IResource source, IResource destination) { return null; }
    
    @Override
    public ISchedulingRule moveRule(IResource source, IResource destination) { return null; }
}