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
package com.google.gdt.eclipse.platform.debug.eval;

import com.google.gdt.eclipse.platform.Activator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.debug.core.IJavaArray;
import org.eclipse.jdt.debug.core.IJavaClassObject;
import org.eclipse.jdt.debug.core.IJavaClassType;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaValue;
import org.eclipse.jdt.internal.debug.eval.ast.instructions.CompoundInstruction;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class that implements the base functionality for invoking methods on
 * JSO objects via JDI.
 * 
 * Will be compiled against Eclipse 3.7 with a custom JDT patch, but will not be
 * executed unless the user has the custom JDT patch installed.
 */
// Using org.eclipse.jdt.internal.debug.*
@SuppressWarnings("restriction")
public abstract class AbstractJsoDelegationMessage extends CompoundInstruction {

  private static final Map<String, String[]> boxingMap = createBoxingMap();

  private static final String PLUGIN_ID = "com.google.gdt.eclipse.platform.e37";

  /*
   * Method signature for:
   * 
   * JsoEval.call(Class klass, Object obj, String methodName, Class[] types,
   * Object... args)
   */
  private static final String JSOEVAL_CALL_METHOD_SIGNATURE = Type.getMethodDescriptor(
      Type.getType(Object.class),
      new Type[] {
          Type.getType(Class.class), Type.getType(Object.class),
          Type.getType(String.class), Type.getType(Class[].class),
          Type.getType(Object[].class)});

  private static Map<String, String[]> createBoxingMap() {
    ILog platformLog = Platform.getLog(Platform.getBundle(PLUGIN_ID));

    Map<String, String[]> boxMap = new HashMap<String, String[]>();

    try {
      for (Class<?> k : new Class<?>[] {
          Integer.class, Short.class, Character.class, Byte.class, Long.class,
          Float.class, Double.class, Boolean.class}) {

        Type primType = Type.getType((Class<?>) k.getField("TYPE").get(null));

        boxMap.put(
            primType.getDescriptor(),
            new String[] {
                k.getName(),
                Type.getMethodDescriptor(Type.getType(k), new Type[] {primType})});
      }
    } catch (NoSuchFieldException e) {
      platformLog.log(new Status(
          IStatus.ERROR,
          PLUGIN_ID,
          "Unable to initialize boxing map; JSO evaluation in the debugger will fail",
          e));
    } catch (IllegalArgumentException e) {
      platformLog.log(new Status(
          IStatus.ERROR,
          PLUGIN_ID,
          "Unable to initialize boxing map; JSO evaluation in the debugger will fail",
          e));
    } catch (SecurityException e) {
      platformLog.log(new Status(
          IStatus.ERROR,
          PLUGIN_ID,
          "Unable to initialize boxing map; JSO evaluation in the debugger will fail",
          e));
    } catch (IllegalAccessException e) {
      platformLog.log(new Status(
          IStatus.ERROR,
          PLUGIN_ID,
          "Unable to initialize boxing map; JSO evaluation in the debugger will fail",
          e));
    }

    return boxMap;
  }

  protected final int argCount;

  protected final String selector;
  protected final String signature;
  protected final String declaringType;

  public AbstractJsoDelegationMessage(String selector, String signature,
      int argCount, String declaringType, int start) {
    super(start);
    this.argCount = argCount;
    this.selector = selector;
    this.signature = signature;
    this.declaringType = declaringType;
  }

  public abstract void execute() throws CoreException;

  /**
   * Boxes primitive values.
   */
  protected IJavaValue boxArg(IJavaValue value) throws CoreException {

    if (boxingMap.isEmpty()) {
      throw new CoreException(
          new Status(
              IStatus.ERROR,
              PLUGIN_ID,
              "Boxing map has not been initialized. JSO evaluation in the debugger will fail. See the error log for details."));
    }

    if (value.getSignature() == null) {
      // represents the null value;
      return value;
    }

    String[] boxingInvocationArgs = boxingMap.get(value.getSignature());

    if (boxingInvocationArgs == null) {
      // No match for primitive types, return the value
      return value;
    }

    assert (boxingInvocationArgs.length == 2);

    IJavaClassObject classReference = getContext().classForName(
        boxingInvocationArgs[0]);
    IJavaValue boxedValue = ((IJavaClassType) classReference.getInstanceType()).sendMessage(
        "valueOf", boxingInvocationArgs[1], new IJavaValue[] {value},
        getContext().getThread());

    return boxedValue;
  }

  /**
   * Helper method that boxes the arguments, gets the types of the arguments,
   * and then makes a method invocation via JDI to JsoEval.call.
   */
  protected IJavaValue invokeJsoEval(IJavaObject jso, IJavaValue[] args)
      throws CoreException {
    List<IJavaValue> argsToJsoEval = new ArrayList<IJavaValue>();

    assert (declaringType != null);
    IJavaClassObject jsoDeclaringClass = getContext().classForName(
        declaringType);
    argsToJsoEval.add(jsoDeclaringClass);

    argsToJsoEval.add(jso);

    argsToJsoEval.add(newValue(selector));

    Type[] argTypes = Type.getArgumentTypes(signature);

    IJavaClassObject[] methodArgTypes = new IJavaClassObject[argTypes.length];
    for (int i = 0; i < argTypes.length; i++) {

      if (argTypes[i].getSort() == Type.OBJECT
          || argTypes[i].getSort() == Type.ARRAY) {
        methodArgTypes[i] = ((IJavaReferenceType) getType(argTypes[i].getClassName())).getClassObject();
      } else {
        // primitive, non-array type
        methodArgTypes[i] = ((IJavaReferenceType) getPrimitiveType(argTypes[i].getClassName())).getClassObject();
      }
    }

    IJavaArray methodArgTypesArray = getArrayType("Ljava.lang.Class;", 1).newInstance(
        methodArgTypes.length);

    /*
     * Seems to be a problem with ArrayReferenceImpl.setValues when dealing with
     * 0-length arrays. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=359450
     * for details.
     * 
     * We add an explicit check to work around the problem.
     */
    if (methodArgTypes.length > 0) {
      methodArgTypesArray.setValues(methodArgTypes);
    }

    argsToJsoEval.add(methodArgTypesArray);

    IJavaArray methodArgs = getArrayType("Ljava.lang.Object;", 1).newInstance(
        args.length);

    if (args.length > 0) {
      methodArgs.setValues(args);
    }

    argsToJsoEval.add(methodArgs);

    IJavaClassObject classReference = getContext().classForName(
        "com.google.gwt.core.ext.debug.JsoEval");

    if (classReference == null || classReference.isNull()) {
      /*
       * TODO: Don't provide an ASTInstructionCompiler in the case where the
       * project is using a version of GWT earlier than 2.4.0.
       * 
       * See note in JSOEvalingASTInstructionCompilerFactory.
       */
      throw new CoreException(
          new Status(IStatus.ERROR, Activator.PLUGIN_ID,
              "Unable to evaluate JavaScriptObject expression - you must be using GWT 2.4.0+"));
    }

    IJavaValue result = ((IJavaClassType) classReference.getInstanceType()).sendMessage(
        "call", JSOEVAL_CALL_METHOD_SIGNATURE,
        argsToJsoEval.toArray(new IJavaValue[argsToJsoEval.size()]),
        getContext().getThread());

    return result;
  }

  protected void setMethodReturnValue(IJavaValue result) {
    if (!signature.endsWith(")V")) {
      // only push the result if not a void method
      setLastValue(result);
      push(result);
    } else {
      /*
       * We need to explicitly do this, because calls to JsoEval for void
       * methods return the string "[success]". This happens becuase
       * JsoEval.call has a return type of Object, so it has to return
       * something.
       */
      setLastValue(getVM().voidValue());
    }
  }
}
