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

import java.lang.annotation.Annotation;

/**
 * Builder class for configurating an container managed persistence unit.
 *
 * @author Stephan Classen
 */
public final class PersistenceUnitBuilder {

  // ---- Members

  /** The module which is built by this instance. */
  private AbstractPersistenceUnitModule module;


  // ---- Constructors

  /**
   * Constructor.
   *
   * @param module the module which is built by this instance.
   */
  PersistenceUnitBuilder(AbstractPersistenceUnitModule module) {
    this.module = module;
  }


  // ---- Methods

  /**
   * Add an annotation to the module. The annotation is used to bind the {@link UnitOfWork} and
   * the {@link EntityManagerProvider} in guice.
   *
   * @param annotation the annotation. May be {@code null}.
   */
  public PersistenceUnitBuilder annotatedWith(Class<? extends Annotation> annotation) {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.annotatedWith(annotation);
    return this;
  }

  /**
   * Configure the persistence unit to use local transactions. This means even if the data source
   * is managed by the container its transaction won't participate in a global container managed
   * transaction (CMT).
   */
  public void useLocalTransaction() {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.setTransactionType(TransactionType.LOCAL);
  }

  /**
   * Configure the persistence unit to use global transactions. This means all transactions on this
   * data source will participate in a global container managed transaction (CMT)
   */
  public void useGlobalTransaction() {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.setTransactionType(TransactionType.GLOBAL);
  }

  /**
   * Builds the module and also changes the state of the builder.
   * After calling this method all calls to the builder will result in an exception.
   *
   * @return the persistence module.
   */
  AbstractPersistenceUnitModule build() {
    checkNotNull(module, "build() can only be called once.");
    final AbstractPersistenceUnitModule m = module;
    module = null;
    return m;
  }

}
