package com.github.sclassen.guicejpa;

/**
 * Abstract super class for {@link ApplicationManagedPersistenceUnitBuilder} and
 * {@link ContainerManagedPersistenceUnitBuilder}.
 *
 * @author Stephan Classen
 */
abstract class AbstractPersistenceModuleBuilder {

  /**
   * Builds the module and also changes the state of the builder.
   * After calling this method all calls to the builder will result in an exception.
   *
   * @return the persistence module.
   */
  abstract AbstractPersistenceUnitModule build();

}
