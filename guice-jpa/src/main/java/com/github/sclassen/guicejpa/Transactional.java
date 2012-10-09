/**
 * Copyright (C) 2012 Stephan Classen
 * Based on guice-perist (Copyright (C) 2010 Google, Inc.)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.sclassen.guicejpa;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.Injector;

/**
 * Marks a method or class to be executed within a transaction.
 * <p/>
 * This will span a new transaction around the method unless there is already a
 * running transaction. In the case that there is a running transaction no new transaction is
 * started. If a rollback happens for a method which did not start the transaction the already
 * existing transaction will be marked as rollbackOnly.
 * <p/>
 * Guice uses AOP to enhance a method annotated with @Transactional with a wrapper.
 * This means the @Transactional only works as expected when:
 * <ul>
 *    <li>
 *        The object on which the method is called has been created by guice. This can be either
 *        done by having it injected into your class or by calling {@link Injector#getInstance()}
 *    </li>
 *    <li>
 *        The method which should be run transactional is not private and not final
 *    </li>
 * </ul>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Transactional {

  /**
   * A List of annotations for application managed persistence units on which to start a
   * transaction. Default is on all persistence units.
   */
  Class<? extends Annotation>[] onUnit() default {};

  /**
   * A list of exceptions to rollback on. Default is {@link RuntimeException}
   */
  Class<? extends Exception>[] rollbackOn() default RuntimeException.class;

  /**
   * A list of exceptions to <b>not<b> rollback on. Use this to exclude one ore more subclasses of
   * the exceptions defined in rollbackOn(). Default is none.
   */
  Class<? extends Exception>[] ignore() default {};
}
