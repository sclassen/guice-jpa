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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * {@link MethodInterceptor} for intercepting methods of persistence units of type RESOURCE_LOCAL.
 *
 * @author Stephan Classen
 */
class ResourceLocalTxnInterceptor extends AbstractTxnInterceptor {

  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emProvider the provider for {@link EntityManager}.
   * @param puAnntoation the annotation of the persistence unit this interceptor belongs to.
   */
  public ResourceLocalTxnInterceptor(EntityManagerProviderImpl emProvider,
      Class<? extends Annotation> puAnntoation) {
    super(emProvider, emProvider, puAnntoation);
    checkNotNull(emProvider);
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  protected TransactionFacade getTransactionFacade(final EntityManager em) {
    EntityTransaction txn = em.getTransaction();
    if (txn.isActive()) {
      return new InnerTransaction(txn);
    }
    return new OuterTransaction(txn);
  }


  // ---- Inner Classes

  /**
   * TransactionFacade representing an inner (nested) transaction.
   * Starting and committing a transaction has no effect.
   * This Facade will set the rollbackOnly flag in case of a roll back.
   */
  private static class InnerTransaction implements TransactionFacade {
    private final EntityTransaction txn;

    InnerTransaction(EntityTransaction txn) {
      this.txn = txn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
      // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() {
      // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() {
      txn.setRollbackOnly();
    }
  }

  /**
   * TransactionFacade representing an outer transaction.
   * This Facade starts and ends the transaction.
   * If an inner transaction has set the rollbackOnly flag the transaction will be rolled back
   * in any case.
   */
  private static class OuterTransaction implements TransactionFacade {
    private final EntityTransaction txn;

    /**
     * {@inheritDoc}
     */
    OuterTransaction(EntityTransaction txn) {
      this.txn = txn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin() {
      txn.begin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit() {
      if (txn.getRollbackOnly()) {
        txn.rollback();
      } else {
        txn.commit();
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback() {
      txn.rollback();
    }
  }

}
