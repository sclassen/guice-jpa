package com.github.sclassen.guicejpa;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.persistence.EntityTransaction;

import com.google.inject.Injector;

/**
 * Marks a method or class to be executed within a local transaction.
 * <p/>
 * This will span a new {@link EntityTransaction} around the method unless there is already a
 * running transaction. In the case that there is a running transaction no new transaction is
 * started. If a rollback happens for a method which did not start the transaction the already
 * existing transaction will be marked as rollbackOnly.
 * <p/>
 * Guice uses AOP to enhance a method annotated with @LocalTransactional with a wrapper.
 * This means the @LocalTransactional only works as expected when:
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
public @interface LocalTransactional {

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
