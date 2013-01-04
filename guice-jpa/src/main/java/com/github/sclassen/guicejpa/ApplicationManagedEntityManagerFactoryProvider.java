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

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Implementation of {@link PersistenceService} and {@link EntityManagerFactoryProvider} for
 * application managed persistence units.
 *
 * @author Stephan Classen
 */
final class ApplicationManagedEntityManagerFactoryProvider implements EntityManagerFactoryProvider,
    PersistenceService {

  // ---- Members

  /** Name of the persistence unit as defined in the persistence.xml. */
  private final String puName;

  /** Additional properties. Theses override the ones defined in the persistence.xml. */
  private final Properties properties;

  /** EntityManagerFactory. */
  private EntityManagerFactory emf;


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param puName the name of the persistence unit as defined in the persistence.xml. Must not be {@code null}.
   * @param properties the additional properties. Theses override the ones defined in the persistence.xml. Must not be {@code null}.
   */
  public ApplicationManagedEntityManagerFactoryProvider(String puName, Properties properties) {
    checkNotNull(puName);
    checkNotNull(properties);

    this.puName = puName;
    this.properties = properties;
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
    emf = Persistence.createEntityManagerFactory(puName, properties);
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
    if (isRunning()) {
      emf.close();
      emf = null;
    }
  }

}
