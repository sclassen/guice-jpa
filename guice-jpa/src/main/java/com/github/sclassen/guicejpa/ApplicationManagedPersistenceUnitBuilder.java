package com.github.sclassen.guicejpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;

/**
 * Builder class for configurating an application managed persistence unit.
 *
 * @author Stephan Classen
 */
public final class ApplicationManagedPersistenceUnitBuilder extends
    AbstractPersistenceModuleBuilder {

  // ---- Members

  /** The module which is built by this instance. */
  private ApplicationManagedPersistenceUnitModule module;


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param module the module which is built by this instance.
   */
  ApplicationManagedPersistenceUnitBuilder(ApplicationManagedPersistenceUnitModule module) {
    this.module = module;
  }


  // ---- Methods

  /**
   * Add an annotation to the module. The annotation is used to bind the {@link UnitOfWork} and
   * the {@link EntityManagerProvider} in guice.
   *
   * @param annotation the annotation. May be {@code null}.
   */
  public void annotatedWith(Class<? extends Annotation> annotation) {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.annotatedWith(annotation);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  ApplicationManagedPersistenceUnitModule build() {
    checkNotNull(module, "build() can only be called once.");
    final ApplicationManagedPersistenceUnitModule m = module;
    module = null;
    return m;
  }
}
