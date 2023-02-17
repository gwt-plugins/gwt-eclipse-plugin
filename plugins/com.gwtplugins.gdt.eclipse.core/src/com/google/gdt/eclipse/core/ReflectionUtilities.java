/**
 *
 */
package com.google.gdt.eclipse.core;

/**
 * @author luca
 *
 */
public class ReflectionUtilities {

  public static <T, R> R invoke(T obj, String method) throws ReflectiveOperationException {
    return (R) obj.getClass().getMethod(method).invoke(obj);
  }

}
