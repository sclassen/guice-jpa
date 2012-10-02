package com.github.sclassen.guicejpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;

import javax.persistence.EntityManager;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.PrivateModule;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;

/**
 * Abstract super class of {@link ApplicationManagedPersistenceUnitModule} and
 * {@link ContainerManagedPersistenceUnitModule}.
 *
 * @author Stephan Classen
 */
abstract class AbstractPersistenceUnitModule extends PrivateModule {

  // ---- Members

  /** The provder for {@link EntityManager}. */
  private final EntityManagerProviderImpl emProvider;

  /** The annotation for this persistence unit. May be {@code null}. */
  private Class<? extends Annotation> annotation;

  /** The method interceptor for transactional annotations. */
  private MethodInterceptor transactionInterceptor;


  // ---- Constructors

  /**
   * Constructor.
   *
   * @param emProvider the provider for {@link EntityManager}. Must not be null.
   */
  AbstractPersistenceUnitModule(EntityManagerProviderImpl emProvider) {
    checkNotNull(emProvider);

    this.emProvider = emProvider;
  }


  // ---- Methods

  /**
   * The method interceptor for intercepting transactional methods.
   *
   * @param utProvider the provider for {@link UserTransactionFacade}. May be {@code null}.
   * @return the interceptor for intercepting transactional methods.
   */
  final MethodInterceptor getTransactionInterceptor(UserTransactionProvider utProvider) {
    if (null == transactionInterceptor) {
      transactionInterceptor = getTxnInterceptor(emProvider, utProvider);
    }
    return transactionInterceptor;
  }

  /**
   * Subclasses must implement this method and return the appropriate MethodInterceptor.
   *
   * @param emProvider the provider for {@link EntityManager}. Must not be {@code null}.
   * @param utProvider the provider for {@link UserTransactionFacade}. May be {@code null}.
   * @return the interceptor for intercepting transactional methods.
   */
  abstract MethodInterceptor getTxnInterceptor(EntityManagerProviderImpl emProvider,
      UserTransactionProvider utProvider);

  /**
   * @return the annotation for transactional methods to intercept.
   */
  abstract Class<? extends Annotation> getTxnAnnotation();

  /**
   * @return the persistence service for the current persistence unit.
   */
  abstract PersistenceService getPersistenceService();

  /**
   * @return the unit of work for the this persistence unit.
   */
  UnitOfWork getUnitOfWork() {
    return emProvider;
  }

  /**
   * Binds the given type annotated with the annotation of this persistence unit and exposes
   * it on the same time.
   *
   * @param type the type to bind.
   * @return the bindingBuilder to define what to bind to the given type.
   */
  protected final <T> LinkedBindingBuilder<T> bindAndExpose(TypeLiteral<T> type) {
    if (null != annotation) {
      expose(type).annotatedWith(annotation);
      return bind(type).annotatedWith(annotation);
    } else {
      expose(type);
      return bind(type);
    }
  }

  /**
   * Binds the given type annotated with the annotation of this persistence unit and exposes
   * it on the same time.
   *
   * @param type the type to bind.
   * @return the bindingBuilder to define what to bind to the given type.
   */
  protected final <T> LinkedBindingBuilder<T> bindAndExpose(Class<T> type) {
    if (null != annotation) {
      expose(type).annotatedWith(annotation);
      return bind(type).annotatedWith(annotation);
    } else {
      expose(type);
      return bind(type);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected final void configure() {
    bind(UnitOfWork.class).toInstance(emProvider);
    bind(EntityManagerProvider.class).toInstance(emProvider);
    bind(PersistenceService.class).toInstance(getPersistenceService());

    if (null == annotation) {
      expose(UnitOfWork.class);
      expose(EntityManagerProvider.class);
      expose(PersistenceService.class);
    } else {
      bind(UnitOfWork.class).annotatedWith(annotation).toInstance(emProvider);
      bind(EntityManagerProvider.class).annotatedWith(annotation).toInstance(emProvider);
      bind(PersistenceService.class).annotatedWith(annotation).toInstance(getPersistenceService());

      expose(UnitOfWork.class).annotatedWith(annotation);
      expose(EntityManagerProvider.class).annotatedWith(annotation);
      expose(PersistenceService.class).annotatedWith(annotation);
    }

    configurePersistence();
  }

  /**
   * Subclasses can overwrite this method to bind (and expose) classes within the context of the
   * private module which defines the current persistence unit.
   */
  protected void configurePersistence() {
    // do nothing
  }

  /**
   * Setter for the annotation of the current persistence unit. The annotation is used to expose
   * the {@link UnitOfWork} and the {@link EntityManagerProvider}.
   *
   * @param annotation the annotation to use for binding the current persistence unit.
   */
  final void annotatedWith(Class<? extends Annotation> annotation) {
    this.annotation = annotation;
  }

  /**
   * @return the annotation used for binding the current persistence unit.
   */
  final Class<? extends Annotation> getAnnotation() {
    return annotation;
  }

}
