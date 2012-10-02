package com.github.sclassen.guicejpa;

import java.lang.reflect.Method;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.Status;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import com.google.common.collect.MapMaker;

/**
 * {@link MethodInterceptor} for intercepting methods which span an global transaction.
 *
 * @author Stephan Classen
 */
class GlobalTxnInterceptor implements MethodInterceptor {

  // ---- Members

  /** Unit of work. */
  private final UnitOfWork unitOfWork;

  /** Provider for {@link EntityManager}. */
  private final EntityManagerProviderImpl emProvider;

  /** Provider for {@link UserTransactionFacade}. */
  private final UserTransactionProvider utProvider;

  /** Thread local store for user {@link UserTransactionFacade}. */
  private final ThreadLocal<UserTransactionFacade> userTransactions = new ThreadLocal<UserTransactionFacade>();

  /** cache for {@link LocalTransactional} annotations per method. */
  private final Map<Method, GlobalTransactional> transactionalCache = new MapMaker().weakKeys()
      .makeMap();


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emProvider the provider for {@link EntityManager}.
   * @param utProvider the provider for {@link UserTransactionFacade}.
   */
  public GlobalTxnInterceptor(EntityManagerProviderImpl emProvider,
      UserTransactionProvider utProvider) {
    this.unitOfWork = emProvider;
    this.emProvider = emProvider;
    this.utProvider = utProvider;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {

    final boolean weStartedTheUnitOfWork = !unitOfWork.isActive();
    if (weStartedTheUnitOfWork) {
      unitOfWork.begin();
    }

    final UserTransactionFacade ut;
    if (null == userTransactions.get()) {
      ut = utProvider.get();
      userTransactions.set(ut);
    } else {
      ut = userTransactions.get();
    }

    try {
      final EntityManager em = emProvider.get();
      final TransactionFacade transactionFacade = TransactionFacade.get(em, ut);

      return invoke(methodInvocation, transactionFacade);
    } finally {
      if (weStartedTheUnitOfWork) {
        unitOfWork.end();
      }
    }

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
      GlobalTransactional t = readTransactionMetadata(methodInvocation);
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
   * Reads the @GlobalTransactional of a given method invocation.
   *
   * @param methodInvocation the method invocation for which to obtain the @GlobalTransactional.
   * @return the @GlobalTransactional of the given method invocation. Never {@code null}.
   */
  private GlobalTransactional readTransactionMetadata(MethodInvocation methodInvocation) {
    final Method method = methodInvocation.getMethod();
    GlobalTransactional result;

    result = transactionalCache.get(method);
    if (null == result) {
      result = method.getAnnotation(GlobalTransactional.class);
      if (null == result) {
        final Class<?> targetClass = methodInvocation.getThis().getClass();
        result = targetClass.getAnnotation(GlobalTransactional.class);
      }
      if (null == result) {
        result = DefaultGlobalTransactional.class.getAnnotation(GlobalTransactional.class);
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
  private boolean rollbackIsNecessary(GlobalTransactional transactional, Throwable e) {
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
    public static TransactionFacade get(EntityManager em, UserTransactionFacade txn) {
      if (Status.STATUS_NO_TRANSACTION == txn.getStatus()) {
        return new InnerTransaction(txn);
      }
      return new OuterTransaction(txn, em);
    }

    protected final UserTransactionFacade txn;

    TransactionFacade(UserTransactionFacade txn) {
      this.txn = txn;
    }

    public abstract void begin();

    public abstract void commit();

    public abstract void rollback();
  }

  /**
   * TransactionFacade representing an inner (nested) transaction. Starting and
   * committing a transaction has no effect. This Facade will set the
   * rollbackOnly flag on the underlying transaction in case of a rollback.
   */
  private static class InnerTransaction extends TransactionFacade {
    InnerTransaction(UserTransactionFacade txn) {
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
   * TransactionFacade representing an outer transaction. This Facade starts
   * and ends the transaction. If an inner transaction has set the rollbackOnly
   * flag the transaction will be rolled back in any case.
   */
  private static class OuterTransaction extends TransactionFacade {
    private final EntityManager em;

    OuterTransaction(UserTransactionFacade txn, EntityManager em) {
      super(txn);
      this.em = em;
    }

    @Override
    public void begin() {
      txn.begin();
      em.joinTransaction();
    }

    @Override
    public void commit() {
      if (Status.STATUS_ACTIVE == txn.getStatus()) {
        txn.commit();
      } else {
        txn.rollback();
      }
    }

    @Override
    public void rollback() {
      txn.rollback();
    }
  }

  /** Helper class for obtaining the default of @GlobalTransactional. */
  @GlobalTransactional
  private static class DefaultGlobalTransactional {
  }

}
