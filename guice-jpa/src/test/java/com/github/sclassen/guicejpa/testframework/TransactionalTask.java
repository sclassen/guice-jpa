/**
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
package com.github.sclassen.guicejpa.testframework;

import java.util.ArrayList;
import java.util.List;

import com.github.sclassen.guicejpa.EntityManagerProvider;
import com.github.sclassen.guicejpa.testframework.exceptions.RuntimeTestException;
import com.github.sclassen.guicejpa.testframework.exceptions.TestException;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

/**
 * A {@link TransactionalTask} is a task which is executed during a transaction
 * test. {@link TransactionalTask}s are passed to a {@link TransactionalWorker} which will call
 * them one after another.
 * The sub classes of {@link TransactionalTask} should create a {@link TransactionTestEntity} and
 * use {@link #storeEntity(TransactionTestEntity)} to persist entities in the DB. They also must
 * call {@link #doOtherTasks()} to allow the {@link TransactionalWorker} to call the other scheduled
 * tasks.
 *
 * @author Stephan Classen
 */
public abstract class TransactionalTask {

  @Inject
  private EntityManagerProvider emProvider;
  private TransactionalWorker worker;
  private final List<TransactionTestEntity> persistedEntities = new ArrayList<TransactionTestEntity>();

  /**
   * Should 'try to' create entities in the persistent storage (i.e. DB).
   * Use {@link #storeEntity(TransactionTestEntity)} to persist entities.
   *
   * @throws TestException may be thrown to test rollback.
   * @throws RuntimeTestException may be thrown to test rollback.
   */
  public abstract void doTransactional() throws TestException, RuntimeTestException;

  /**
   * Does other tasks.
   *
   * @throws TestException may be thrown to test rollback.
   * @throws RuntimeTestException may be thrown to test rollback.
   */
  protected final void doOtherTasks() throws TestException, RuntimeTestException {
    worker.doNextTask();
  }

  /**
   * Stores an entity.
   *
   * @param entity the entity to store.
   */
  protected final void storeEntity(TransactionTestEntity entity) {
    emProvider.get().persist(entity);
    persistedEntities.add(entity);
  }

  @VisibleForTesting
  void setWorker(TransactionalWorker transactionalWorker) {
    worker = transactionalWorker;
  }

  @VisibleForTesting
  List<TransactionTestEntity> getPersistedEntities() {
    return persistedEntities;
  }

}
