package com.github.sclassen.guicejpa;

import javax.persistence.EntityManagerFactory;

/**
 * Provider for {@link EntityManagerFactoryProvider}.
 *
 * @author Stephan Classen
 */
interface EntityManagerFactoryProvider extends PersistenceService {

  /**
   * @return the provider for {@link EntityManagerFactory}.
   * @throws IllegalStateException if {@link PersistenceService#isRunning()} returns {@code false}.
   */
  EntityManagerFactory get();

}
