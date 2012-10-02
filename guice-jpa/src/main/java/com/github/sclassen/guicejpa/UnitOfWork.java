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
