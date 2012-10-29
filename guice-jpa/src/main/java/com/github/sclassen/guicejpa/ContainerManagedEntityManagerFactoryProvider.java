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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

/**
 * Implementation of {@link PersistenceService} and {@link EntityManagerFactoryProvider} for
 * container managed entity manager factories.
 *
 * @author Stephan Classen
 */
final class ContainerManagedEntityManagerFactoryProvider implements EntityManagerFactoryProvider,
    PersistenceService {

  // ---- Members

  /** The JNDI name of the {@link EntityManagerFactory}. */
  private final String emfJndiName;

  /** The {@link EntityManagerFactory}. */
  private EntityManagerFactory emf;


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}.
   */
  public ContainerManagedEntityManagerFactoryProvider(String emfJndiName) {
    this.emfJndiName = emfJndiName;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public EntityManagerFactory get() {
    if (isRunning()) {
      return emf;
    }

    throw new IllegalStateException("PersistenceService is not running.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    if (isRunning()) {
      throw new IllegalStateException("PersistenceService is already running.");
    }
    try {
      final InitialContext ctx = new InitialContext();
      emf = (EntityManagerFactory) ctx.lookup(emfJndiName);
    } catch (NamingException e) {
      throw new RuntimeException("lookup for EntityManagerFactory with JNDI name '" + emfJndiName
          + "' failed", e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRunning() {
    return null != emf;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    emf = null;
  }

}
