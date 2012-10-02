package com.github.sclassen.guicejpa;

import javax.persistence.EntityManager;

/**
 * The Unit of work correlates with the life cycle of the {@link EntityManager}.
 * According to JPA every thread should use its own {@link EntityManager}. Therefore the unit of
 * work will control the life cycle of the {@link EntityManager} on a per thread basis.
 * <p/>
 * Most of the time it is not recommended to manual control the unit of work.
 * <p/>
 * For applications running in a container the {@link PersistenceFilter} is recommended.
 * It will start a unit of work for every incoming request and properly close it at the end.
 * <p/>
 * For stand alone application it is recommended to relay on the @{@link LocalTransactional}.
 * The transaction handler will automatically span a unit of work around a transaction.
 * <p/>
 * The most likely scenario in which one would want to take manual control over the unit of work
 * is in a background thread within a container (i.e. timer triggered jobs).
 * <p/>
 * Recommended pattern:
 * <pre>
 * public void someMethod() {
 *   final boolean unitOfWorkWasInactive = ! unitOfWork.isActive();
 *   if (unitOfWorkWasInactive) {
 *     unitOfWork.begin();
 *   }
 *   try {
 *     // do work
 *   }
 *   finally {
 *     if (unitOfWorkWasInactive) {
 *       unitOfWork.end();
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Stephan Classen
 */
public interface UnitOfWork {

  /**
   * Starts the unit of work.
   * When the unit of work has already been started for the current thread an
   * {@link IllegalStateException} is thrown.
   *
   * @throws IllegalStateException if the unit of work is already running for this thread.
   */
  void begin();

  /**
   * @return {@code true} if the unit of work is already running for this thread
   *         {@code false} otherwise.
   */
  boolean isActive();

  /**
   * Stops the unit of work.
   * When not unit of work is active this method will do nothing.
   */
  void end();

}
