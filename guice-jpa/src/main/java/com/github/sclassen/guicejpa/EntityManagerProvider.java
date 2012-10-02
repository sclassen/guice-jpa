package com.github.sclassen.guicejpa;

import javax.persistence.EntityManager;

import com.google.inject.Provider;

/**
 * Provider for {@link EntityManager}.
 * <p/>
 * This class does not implement the {@link Provider} interface because the {@link EntityManager}
 * objects do have a life cycle and should therefore not be stored in instance/member variables.
 * <p/>
 * The {@link UnitOfWork} defines the life cycle of the {@link EntityManager}. An entity manager
 * will be created when the unit of work is started. It is open and valid for use during the entire
 * unit of work. When the unit of work is closed the entity manager is closed and can no longer
 * be used.
 * <p/>
 * It is a good practice to store this provider in a instance/member variable and only obtain an
 * {@link EntityManager} instance in a Method where it is used. This ensures that the method always
 * has access to a valid {@link EntityManager}.
 * <p/>
 * The {@link EntityManagerProvider} is thread save.
 *
 * @author Stephan Classen
 */
public interface EntityManagerProvider {

  /**
   * @return the {@link EntityManager}.
   * @throws IllegalStateException if {@link UnitOfWork#isActive()} returns false.
   */
  EntityManager get();

}
