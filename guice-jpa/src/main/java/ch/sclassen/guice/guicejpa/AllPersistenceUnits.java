package ch.sclassen.guice.guicejpa;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Annotation for {@link PersistenceService} and {@link UnitOfWork}. This is a special annotation
 * for retrieving a container holding all registered persistence units. Calling a method on one
 * of these container will trigger the method call on all registered instances. In other words
 * on all perisistence untis.
 *
 * @author Stephan Classen
 */
@Retention(RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD })
@BindingAnnotation
public @interface AllPersistenceUnits {

}
