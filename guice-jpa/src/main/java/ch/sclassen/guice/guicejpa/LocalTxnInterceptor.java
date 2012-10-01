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

package ch.sclassen.guice.guicejpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.common.collect.MapMaker;

/**
 * {@link MethodInterceptor} for intercepting methods which span an local transaction.
 *
 * @author Stephan Classen
 */
class LocalTxnInterceptor implements MethodInterceptor {

  /** Unit of work. */
  private final UnitOfWork unitOfWork;

  /** Provider for {@link EntityManager}. */
  private final EntityManagerProviderImpl emProvider;

  /** Annotation of the persistence unit this interceptor belongs to. */
  private final Class<? extends Annotation> puAnntoation;

  /** cache for {@link LocalTransactional} annotations per method. */
  private final Map<Method, LocalTransactional> transactionalCache = new MapMaker().weakKeys()
      .makeMap();


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emProvider the provider for {@link EntityManager}.
   * @param puAnntoation the annotation of the persistence unit this interceptor belongs to.
   */
  public LocalTxnInterceptor(EntityManagerProviderImpl emProvider,
      Class<? extends Annotation> puAnntoation) {
    checkNotNull(emProvider);

    this.unitOfWork = emProvider;
    this.emProvider = emProvider;
    this.puAnntoation = puAnntoation;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    if (!transactionCoversThisPersistenceUnit(methodInvocation)) {
      return methodInvocation.proceed();
    }

    final boolean weStartedTheUnitOfWork = !unitOfWork.isActive();
    if (weStartedTheUnitOfWork) {
      unitOfWork.begin();
    }

    try {
      final EntityManager em = emProvider.get();
      final TransactionFacade transactionFacade = TransactionFacade.get(em);

      return invoke(methodInvocation, transactionFacade);
    } finally {
      if (weStartedTheUnitOfWork) {
        unitOfWork.end();
      }
    }

  }

  /**
   * Check whether the persistence unit of this interceptor participates in the transaction or not.
   *
   * @param methodInvocation the original method invocation.
   * @return {@code true} if the persistence unit participates in the transaction
   *         {@code false} otherwise.
   */
  private boolean transactionCoversThisPersistenceUnit(MethodInvocation methodInvocation) {
    if (null == puAnntoation) {
      return true;
    }

    final LocalTransactional localTransaction = readTransactionMetadata(methodInvocation);
    final Class<? extends Annotation>[] units = localTransaction.onUnit();
    if (null == units || 0 == units.length) {
      return true;
    }

    for (int i = 0, n = units.length; i < n; i++) {
      if (puAnntoation.equals(units[i])) {
        return true;
      }
    }

    return false;
  }

  /**
   * Invoke the original method surrounded by a transaction.
   *
   * @param methodInvocation the original method invocation.
   * @param transactionFacade the Facade to the transaction.
   * @return the result of the invocation of the original method.
   * @throws Throwable if an exception occurs during the call to the original method.
   */
  private Object invoke(MethodInvocation methodInvocation, TransactionFacade transactionFacade)
      throws Throwable {

    transactionFacade.begin();
    final Object result = doTransactional(methodInvocation, transactionFacade);
    transactionFacade.commit();

    return result;
  }

  /**
   * Invoke the original method assuming a transaction has already been started.
   *
   * @param methodInvocation the original method invocation.
   * @param transactionFacade the Facade to the transaction.
   * @return the result of the invocation of the original method.
   * @throws Throwable if an exception occurs during the call to the original method.
   */
  private Object doTransactional(MethodInvocation methodInvocation,
      TransactionFacade transactionFacade) throws Throwable {
    try {
      return methodInvocation.proceed();
    } catch (Throwable e) {
      LocalTransactional t = readTransactionMetadata(methodInvocation);
      if (rollbackIsNecessary(t, e)) {
        transactionFacade.rollback();
      } else {
        transactionFacade.commit();
      }
      // In any case: throw the original exception.
      throw e;
    }
  }

  /**
   * Reads the @LocalTransactional of a given method invocation.
   *
   * @param methodInvocation the method invocation for which to obtain the @LocalTransactional.
   * @return the @LocalTransactional of the given method invocation. Never {@code null}.
   */
  private LocalTransactional readTransactionMetadata(MethodInvocation methodInvocation) {
    final Method method = methodInvocation.getMethod();
    LocalTransactional result;

    result = transactionalCache.get(method);
    if (null == result) {
      result = method.getAnnotation(LocalTransactional.class);
      if (null == result) {
        final Class<?> targetClass = methodInvocation.getThis().getClass();
        result = targetClass.getAnnotation(LocalTransactional.class);
      }
      if (null == result) {
        result = DefaultLocalTransactional.class.getAnnotation(LocalTransactional.class);
      }

      transactionalCache.put(method, result);
    }
    return result;
  }

  /**
   * Returns True if a rollback is necessary.
   *
   * @param transactional The metadata annotation of the method
   * @param e The exception to test for rollback
   * @return {@code true} if a rollback is necessary, {@code false} otherwise.
   */
  private boolean rollbackIsNecessary(LocalTransactional transactional, Throwable e) {
    for (Class<? extends Exception> rollbackOn : transactional.rollbackOn()) {
      if (rollbackOn.isInstance(e)) {
        for (Class<? extends Exception> ignore : transactional.ignore()) {
          if (ignore.isInstance(e)) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }


  // ---- Inner Classes

  /**
   * Abstract class which hides away the details of inner (nested) and outer transactions.
   */
  private abstract static class TransactionFacade {
    public static TransactionFacade get(EntityManager em) {
      EntityTransaction txn = em.getTransaction();
      if (txn.isActive()) {
        return new InnerTransaction(txn);
      }
      return new OuterTransaction(txn);
    }

    protected final EntityTransaction txn;

    TransactionFacade(EntityTransaction txn) {
      this.txn = txn;
    }

    public abstract void begin();

    public abstract void commit();

    public abstract void rollback();
  }

  /**
   * TransactionFacade representing an inner (nested) transaction.
   * Starting and committing a transaction has no effect.
   * This Facade will set the rollbackOnly flag in case of a roll back.
   */
  private static class InnerTransaction extends TransactionFacade {
    InnerTransaction(EntityTransaction txn) {
      super(txn);
    }

    @Override
    public void begin() {
      // Do nothing
    }

    @Override
    public void commit() {
      // Do nothing
    }

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
  private static class OuterTransaction extends TransactionFacade {
    OuterTransaction(EntityTransaction txn) {
      super(txn);
    }

    @Override
    public void begin() {
      txn.begin();
    }

    @Override
    public void commit() {
      if (txn.getRollbackOnly()) {
        txn.rollback();
      } else {
        txn.commit();
      }
    }

    @Override
    public void rollback() {
      txn.rollback();
    }
  }

  /** Helper class for obtaining the default of @GlobalTransactional. */
  @LocalTransactional
  private static class DefaultLocalTransactional {
  }

}
