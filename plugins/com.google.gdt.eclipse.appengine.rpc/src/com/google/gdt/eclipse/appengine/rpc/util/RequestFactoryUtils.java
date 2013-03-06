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
package com.google.gdt.eclipse.appengine.rpc.util;

import com.google.gdt.eclipse.appengine.rpc.AppEngineRPCPlugin;

import static com.google.gdt.eclipse.appengine.rpc.util.JavaUtils.hasParams;
import static com.google.gdt.eclipse.appengine.rpc.util.JavaUtils.isBooleanReturnType;
import static com.google.gdt.eclipse.appengine.rpc.util.JavaUtils.isPrefixedBy;
import static com.google.gdt.eclipse.appengine.rpc.util.JavaUtils.isPublicInstanceMethod;

import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO: doc me.
 */
public class RequestFactoryUtils {

  /**
   * TODO: doc me.
   */
  public static class PropertyListLabelProvider extends
      JavaElementLabelProvider {

    @Override
    public Image getImage(Object element) {
      if (element instanceof PropertyNode) {
        int flags = 0;
        try {
          flags = ((PropertyNode) element).getAccessors().get(0).getFlags();
        } catch (JavaModelException e) {
          AppEngineRPCPlugin.log(e);
        }
        return JavaUtils.getFieldImage(flags);
      }
      return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
      if (element instanceof PropertyNode) {
        return ((PropertyNode) element).getName();
      }
      return super.getText(element);
    }
  }
  static class EntityListContentProvider implements ITreeContentProvider {

    private List<IType> types = new ArrayList<IType>();

    public EntityListContentProvider(IJavaProject project)
        throws JavaModelException {
      if (project != null) {
        types.addAll(RequestFactoryUtils.findTypes(project, RpcType.ENTITY));
      }
    }

    public void dispose() {
      types.clear();
      types = null;
    }

    public Object[] getChildren(Object parentElement) {
      return getElements(parentElement);
    }

    public Object[] getElements(Object inputElement) {
      return types.toArray();
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // ignored
    }
  }
  static class PropertyList {
    public static PropertyList forType(IType type) throws JavaModelException {
      PropertyList properties = new PropertyList();
      for (IMethod method : type.getMethods()) {
        properties.insertOrIgnore(method);
      }
      return properties;
    }

    private final Map<String, PropertyNode> map = new HashMap<String, PropertyNode>();

    public void clear() {
      map.clear();
    }

    public Set<String> getNames() {
      return map.keySet();
    }

    public Collection<PropertyNode> getProperties() {
      return map.values();
    }

    public void insertOrIgnore(IMethod method) throws JavaModelException {
      if (isPropertyAccessor(method)) {
        String property = getPropertyName(method);
        PropertyNode propertyNode = getPropertyNodeForName(property);
        propertyNode.addAccessor(method);
      }
    }

    private PropertyNode getPropertyNodeForName(String property) {
      PropertyNode propertyNode = map.get(property);
      if (propertyNode == null) {
        propertyNode = new PropertyNode(property);
        map.put(property, propertyNode);
      }
      return propertyNode;
    }
  }

  static class PropertyListContentProvider implements ITreeContentProvider {

    private static final PropertyNode[] EMPTY = new PropertyNode[0];

    private PropertyList properties;

    public PropertyListContentProvider(IType type) throws JavaModelException {
      properties = PropertyList.forType(type);
    }

    public void dispose() {
      properties.clear();
      properties = null;
    }

    public Object[] getChildren(Object parentElement) {
      if (parentElement instanceof PropertyNode) {
        return ((PropertyNode) parentElement).getAccessors().toArray();
      }
      return EMPTY;
    }

    public Object[] getElements(Object inputElement) {
      return properties.getProperties().toArray();
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // ignored
    }
  }
  static class PropertyNode {

    private final List<IMethod> accessors = new ArrayList<IMethod>(2);
    private final String name;

    public PropertyNode(String name) {
      this.name = name;
    }

    public void addAccessor(IMethod method) {
      accessors.add(method);
    }

    public List<IMethod> getAccessors() {
      return accessors;
    }

    public String getName() {
      return name;
    }
  }

  static class ServiceListContentProvider implements ITreeContentProvider {

    private static final PropertyNode[] EMPTY = new PropertyNode[0];

    private final List<IMethod> methods = new ArrayList<IMethod>();

    public ServiceListContentProvider(IType type) throws JavaModelException {
      for (IMethod method : type.getMethods()) {
        if (isServiceMethod(method)) {
          methods.add(method);
        }
      }
    }

    public void dispose() {
      methods.clear();
    }

    public Object[] getChildren(Object parentElement) {
      return EMPTY;
    }

    public Object[] getElements(Object inputElement) {
      return methods.toArray();
    }

    public Object getParent(Object element) {
      return null;
    }

    public boolean hasChildren(Object element) {
      return getChildren(element).length > 0;
    }

    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // ignored
    }
  }

  private static final String[] BOOLEAN_GETTER_PREFIXES = new String[] {
      "has", "is"}; //$NON-NLS-1$ //$NON-NLS-2$
  /*
   * NOTE: Unless we create an AST we have no access to type bindings so we
   * cannot do a check against fully qualified type names. That said, this is
   * what Guice does with @Nullable, so at least there's some precedent.
   */
  private static String[] ENTITY_ANNOTATIONS = {"Entity", "PersistenceCapable"}; //$NON-NLS-1$//$NON-NLS-2$
  private static final String GETTER_PREFIX = "get"; //$NON-NLS-1$

  private static final List<String> PROPERTY_PREFIXES = new ArrayList<String>(
      Arrays.asList(BOOLEAN_GETTER_PREFIXES));

  private static final String SETTER_PREFIX = "set"; //$NON-NLS-1$

  static {
    PROPERTY_PREFIXES.add(GETTER_PREFIX);
    PROPERTY_PREFIXES.add(SETTER_PREFIX);
  }

  public static ITreeContentProvider createEntityListContentProvider(
      IJavaProject project) throws JavaModelException {
    return new EntityListContentProvider(project);
  }

  public static ITreeContentProvider createPropertyListContentProvider(
      IType type) throws JavaModelException {
    return new PropertyListContentProvider(type);
  }

  public static ILabelProvider createPropertyListLabelProvider() {
    return new PropertyListLabelProvider();
  }

  public static ITreeContentProvider createServiceListContentProvider(IType type)
      throws JavaModelException {
    return new ServiceListContentProvider(type);
  }

  public static String creatServiceNameProposal(String projectName) {
    projectName = unqualify(projectName);
    // fix case of leading character

    StringBuilder sb = new StringBuilder();
    sb.append(Character.toUpperCase(projectName.charAt(0)));
    sb.append(projectName.substring(1));

    sb.append("Service"); //$NON-NLS-1$
    return sb.toString();
  }

  /**
   * Find all the relevant request factory types in the project and populate the
   * given lists/hashmap
   * 
   * @param project IJavaProject
   * @param entities List<IType>
   * @param proxys List<IType>
   * @param requests HaspMap<String,ITYpe>
   * @throws JavaModelException
   */
  public static void findAllTypes(IJavaProject project, List<IType> entities,
      List<IType> proxys, HashMap<String, IType> requests)
      throws JavaModelException {

    for (IPackageFragmentRoot pkgRoot : project.getPackageFragmentRoots()) {
      if (pkgRoot.isArchive()) {
        continue;
      }

      for (IJavaElement fragment : pkgRoot.getChildren()) {
        IJavaElement[] children = ((IPackageFragment) fragment).getChildren();

        for (IJavaElement child : children) {
          if (child instanceof ICompilationUnit) {
            IType[] topLevelTypes = ((ICompilationUnit) child).getTypes();

            for (IType t : topLevelTypes) {

              determineType(t, entities, proxys, requests);
            }
          }
        }
      }
    }
  }

  /**
   * Find types in a project that satisfy a certain selection criteria
   * 
   * RPC_ENTITY : Find types annotated with the <code>@Entity</code> or
   * <code>@PersistenceCapable</code> annotations, less all framework-specific
   * entities (identified by {@link #isFrameworkClass(IType)})
   * 
   * RPC_PROXY : Find types that extend
   * com.google.web.bindery.requestfactory.shared.ValueProxy or
   * com.google.web.bindery.requestfactory.shared.EntityProxy
   * 
   * RPC_REQUEST : Find types that extend
   * com.google.web.bindery.requestfactory.shared.RequestContext
   * 
   */
  public static List<IType> findTypes(IJavaProject project, RpcType type)
      throws JavaModelException {

    List<IType> types = new ArrayList<IType>();
    for (IPackageFragmentRoot pkgRoot : project.getPackageFragmentRoots()) {
      if (pkgRoot.isArchive()) {
        continue;
      }
      for (IJavaElement elem : pkgRoot.getChildren()) {
        collectTypes(types, (IPackageFragment) elem, type);
      }
    }

    return types;
  }

  public static String getPropertyName(IMethod method) {
    return getPropertyName(method.getElementName());
  }

  public static IType getProxyForEntity(String entityName, IJavaProject project)
      throws JavaModelException {
    List<IType> proxies = RequestFactoryUtils.findTypes(project, RpcType.PROXY);
    for (IType type : proxies) {
      IAnnotation annotation = type.getAnnotation("ProxyForName"); //$NON-NLS-N$
      if (annotation.exists()) {
        IMemberValuePair[] values = annotation.getMemberValuePairs();
        if (values[0].getValue().equals(entityName)) {
          return type;
        }
      }
    }

    return null;
  }

  public static IType getProxyForEntity(String entityName, List<IType> proxies)
      throws JavaModelException {
    for (IType type : proxies) {
      IAnnotation annotation = type.getAnnotation("ProxyForName"); //$NON-NLS-N$
      if (annotation.exists()) {
        IMemberValuePair[] values = annotation.getMemberValuePairs();
        if (values[0].getValue().equals(entityName)) {
          return type;
        }
      }
    }

    return null;
  }

  public static IType getRequestForService(String serviceName,
      IJavaProject project) throws JavaModelException {

    List<IType> requests = RequestFactoryUtils.findTypes(project,
        RpcType.REQUEST);
    for (IType type : requests) {
      IAnnotation annotation = type.getAnnotation("ServiceName"); //$NON-NLS-N$
      if (annotation.exists()) {
        IMemberValuePair[] values = annotation.getMemberValuePairs();
        if (values[0].getValue().equals(serviceName)) {
          return type;
        }
      }
    }

    return null;
  }

  public static IType getRequestForService(String serviceName,
      List<IType> requests) throws JavaModelException {
    if (requests == null) {
      return null;
    }

    for (IType type : requests) {
      IAnnotation annotation = type.getAnnotation("ServiceName"); //$NON-NLS-N$
      if (annotation.exists()) {
        IMemberValuePair[] values = annotation.getMemberValuePairs();
        if (values[0].getValue().equals(serviceName)) {
          return type;
        }
      }
    }

    return null;
  }

  /**
   * Test if the given type is provided by the framework. This is essentially a
   * blacklist test. Used to filter out classes that are persistable but that
   * will not cross the wire.
   */
  public static boolean isFrameworkClass(IType type) {
    // DeviceInfo test
    // http://code.google.com/p/google-plugin-for-eclipse/issues/detail?id=12
    if (type.getElementName().equals("DeviceInfo")) { //$NON-NLS-1$
      try {
        return hasMethodThatReturns(type, "getDeviceRegistrationID", "QString;"); //$NON-NLS-1$ //$NON-NLS-2$
      } catch (JavaModelException e) {
        // ignore
      }
    }
    return false;
  }

  public static boolean isGetterMethod(IMethod method)
      throws JavaModelException {

    if (!isPublicInstanceMethod(method) || hasParams(method)) {
      return false;
    }

    String returnType = method.getReturnType();

    if (Signature.SIG_VOID.equals(returnType)) {
      return false;
    }

    String methodName = method.getElementName();

    // boolean getters {is,has}
    for (String prefix : BOOLEAN_GETTER_PREFIXES) {
      if (isPrefixedBy(methodName, prefix)) {
        return isBooleanReturnType(returnType);
      }
    }

    // standard getter {get}
    return isPrefixedBy(methodName, GETTER_PREFIX);
  }

  public static boolean isPropertyAccessor(IMethod method)
      throws JavaModelException {
    return isGetterMethod(method) || isSetterMethod(method);
  }

  public static boolean isServiceMethod(IMethod method)
      throws JavaModelException {
    return Flags.isPublic(method.getFlags()) && !isPropertyAccessor(method);
  }

  public static boolean isSetterMethod(IMethod method)
      throws JavaModelException {
    return hasSetterFormat(method.getElementName())
        && isPublicInstanceMethod(method)
        && Signature.SIG_VOID.equals(method.getReturnType());
  }

  /**
   * Check to see if this type should be proxied as an Entity (rather than a
   * simple Value).
   * 
   * @throws JavaModelException
   */
  public static boolean shouldBeProxiedAsAnEntity(IType type)
      throws JavaModelException {
    return hasIdProperty(type) && hasVersionProperty(type);
  }

  private static void collectTypes(List<IType> types, ICompilationUnit cu,
      RpcType type) throws JavaModelException {
    IType[] topLevelTypes = cu.getTypes();
    switch (type) {
      case PROXY:
        for (IType t : topLevelTypes) {
          if (isProxyType(t)) {
            types.add(t);
          }
        }
        break;
      case REQUEST:
        for (IType t : topLevelTypes) {
          if (isRequestType(t)) {
            types.add(t);
          }
        }
        break;
      case ENTITY:
        for (IType t : topLevelTypes) {
          if (isEntityAnnotatedType(t) && !isFrameworkClass(t)) {
            types.add(t);
          }
        }
        break;
      case REQ_FACTORY:
        for (IType t : topLevelTypes) {
          if (isRequestFactoryType(t)) {
            types.add(t);
          }
        }
        break;
    }
  }

  private static void collectTypes(List<IType> types,
      IPackageFragment fragment, RpcType type) throws JavaModelException {
    IJavaElement[] children = fragment.getChildren();
    for (IJavaElement child : children) {
      if (child instanceof ICompilationUnit) {
        collectTypes(types, (ICompilationUnit) child, type);
      }
    }
  }

  private static void determineType(IType type, List<IType> entities,
      List<IType> proxys, HashMap<String, IType> requests)
      throws JavaModelException {

    if (type.isInterface()) {
      List<String> interfaces = Arrays.asList(type.getSuperInterfaceNames());
      if (interfaces.contains("ValueProxy") //$NON-NLS-1$
          || interfaces.contains("EntityProxy")) { //$NON-NLS-2$
        proxys.add(type);
        return;
      }
      if (interfaces.contains("RequestContext")) { //$NON-NLS-N$
        IAnnotation annotation = type.getAnnotation("ServiceName"); //$NON-NLS-N$
        if (annotation.exists()) {
          IMemberValuePair[] values = annotation.getMemberValuePairs();
          requests.put((String) values[0].getValue(), type);
          return;
        }
      }
      return;
    }
    IAnnotation[] annotations = type.getAnnotations();
    for (IAnnotation annotation : annotations) {
      if (isEntityAnnotation(annotation)) {
        if (!isFrameworkClass(type)) {
          entities.add(type);
        }
      }
    }
  }

  private static String getPropertyName(String methodName) {
    for (String prefix : PROPERTY_PREFIXES) {
      if (isPrefixedBy(methodName, prefix)) {
        // fix case of property leading character
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(methodName.charAt(prefix.length())));
        sb.append(methodName.substring(prefix.length() + 1));
        return sb.toString();
      }
    }
    return null;
  }

  private static boolean hasIdProperty(IType type) throws JavaModelException {
    return hasMethodThatReturns(type, "getId", "QLong;"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private static boolean hasMethodThatReturns(IType type, String methodName,
      String returnType) throws JavaModelException {
    for (IMethod method : type.getMethods()) {
      if (method.getElementName().equals(methodName)) {
        if (method.getNumberOfParameters() == 0) {
          if (method.getReturnType().equals(returnType)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static boolean hasSetterFormat(String methodName) {
    return isPrefixedBy(methodName, SETTER_PREFIX);
  }

  private static boolean hasVersionProperty(IType type)
      throws JavaModelException {
    return hasMethodThatReturns(type, "getVersion", "QInteger;"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  private static boolean isEntityAnnotatedType(IType type)
      throws JavaModelException {
    IAnnotation[] annotations = type.getAnnotations();
    for (IAnnotation annotation : annotations) {
      if (isEntityAnnotation(annotation)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isEntityAnnotation(IAnnotation annotation) {
    String elementName = annotation.getElementName();
    for (String annotationName : ENTITY_ANNOTATIONS) {
      if (annotationName.equals(elementName)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isProxyType(IType type) throws JavaModelException {
    if (type.isInterface()) {
      List<String> interfaces = Arrays.asList(type.getSuperInterfaceNames());
      if (interfaces.contains("ValueProxy") //$NON-NLS-1$
          || interfaces.contains("EntityProxy")) { //$NON-NLS-2$
        return true;
      }
    }
    return false;
  }

  private static boolean isRequestFactoryType(IType type)
      throws JavaModelException {
    if (type.isInterface()) {
      List<String> interfaces = Arrays.asList(type.getSuperInterfaceNames());
      if (interfaces.contains("RequestFactory")) { //$NON-NLS-N$
        return true;
      }
    }
    return false;
  }

  private static boolean isRequestType(IType type) throws JavaModelException {
    if (type.isInterface()) {
      List<String> interfaces = Arrays.asList(type.getSuperInterfaceNames());
      if (interfaces.contains("RequestContext")) { //$NON-NLS-N$
        return true;
      }
    }
    return false;
  }

  private static String removeDots(String projectName) {
    int lastDot = projectName.lastIndexOf('.');
    if (lastDot != -1) {
      if (projectName.length() > lastDot) {
        projectName = projectName.substring(lastDot + 1);
      }
    }
    return projectName;
  }

  private static String removeSpaces(String projectName) {
    int firstSpace = projectName.indexOf(' ');
    if (firstSpace != -1) {
      projectName = projectName.substring(0, firstSpace);
    }
    return projectName;
  }

  private static String unqualify(String projectName) {
    projectName = removeDots(projectName);
    projectName = removeSpaces(projectName);
    if (projectName.contains("-AppEngine")) { //$NON-NLS-1$
      projectName = projectName.replaceAll("-AppEngine", ""); //$NON-NLS-1$//$NON-NLS-2$
    }
    return projectName;
  }

}
