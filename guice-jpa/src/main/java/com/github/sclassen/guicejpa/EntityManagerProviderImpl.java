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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implementation of {@link EntityManagerProvider} and {@link UnitOfWork}.
 *
 * @author Stephan Classen
 */
final class EntityManagerProviderImpl implements EntityManagerProvider, UnitOfWork {

  // ---- Members

  /** Provider for {@link EntityManagerFactory}. */
  private final EntityManagerFactoryProvider emfProvider;

  /** Additional properties to be set on every {@link EntityManager} which is created. */
  private final Properties properties;

  /** Thread local store of {@link EntityManager}s. */
  private final ThreadLocal<EntityManager> entityManagers = new ThreadLocal<EntityManager>();


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emfProvider the provider for {@link EntityManagerFactory}.
   */
  public EntityManagerProviderImpl(EntityManagerFactoryProvider emfProvider) {
    this(emfProvider, null);
  }

  /**
   * Constructor.
   *
   * @param emfProvider the provider for {@link EntityManagerFactory}. Must not be {@code null}.
   * @param properties additional properties to be set on every {@link EntityManager} which is created.
   */
  public EntityManagerProviderImpl(EntityManagerFactoryProvider emfProvider, Properties properties) {
    checkNotNull(emfProvider);

    this.emfProvider = emfProvider;
    this.properties = properties;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public EntityManager get() {
    final EntityManager entityManager = entityManagers.get();
    if (null != entityManager) {
      return entityManager;
    }

    throw new IllegalStateException("UnitOfWork is not running.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() {
    if (isActive()) {
      throw new IllegalStateException("Unit of work has already been started.");
    }

    final EntityManagerFactory emf = emfProvider.get();
    final EntityManager em;
    if (null == properties) {
      em = emf.createEntityManager();
    }
    else {
      em = emf.createEntityManager(properties);
    }

    entityManagers.set(em);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isActive() {
    return null != entityManagers.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() {
    final EntityManager em = entityManagers.get();
    if (null != em) {
      em.close();
      entityManagers.remove();
    }
  }

}
