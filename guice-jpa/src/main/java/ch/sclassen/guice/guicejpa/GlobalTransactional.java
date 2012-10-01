package ch.sclassen.guice.guicejpa;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.transaction.UserTransaction;

import com.google.inject.Injector;

/**
 * Marks a method or class to be executed within a global transaction.
 * <p/>
 * This will span a new {@link UserTransaction} around the method unless there is already a
 * running transaction. In the case that there is a running transaction no new transaction is
 * started. If a rollback happens for a method which did not start the transaction the already
 * existing transaction will be marked as rollbackOnly.
 * <p/>
 * Guice uses AOP to enhance a method annotated with @GlobalTransactional with a wrapper.
 * This means the @GlobalTransaction only works as expected when:
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
public @interface GlobalTransactional {

  /**
   * A list of exceptions to rollback on.
   * Default is {@link RuntimeException}
   */
  Class<? extends Exception>[] rollbackOn() default RuntimeException.class;

  /**
   * A list of exceptions to <b>not<b> rollback on.
   * Use this to exclude one ore more subclasses of the exceptions defined in rollbackOn().
   */
  Class<? extends Exception>[] ignore() default { };
}
