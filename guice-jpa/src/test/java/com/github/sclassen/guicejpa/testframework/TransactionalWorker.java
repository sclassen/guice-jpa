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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.github.sclassen.guicejpa.EntityManagerProvider;
import com.github.sclassen.guicejpa.LocalTransactional;
import com.github.sclassen.guicejpa.UnitOfWork;
import com.github.sclassen.guicejpa.testframework.exceptions.RuntimeTestException;
import com.github.sclassen.guicejpa.testframework.exceptions.TestException;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Worker for transactional tests. The worker instantiates the {@link TransactionalTask} and
 * executes them.
 *
 * @author Stephan Classen
 */
public class TransactionalWorker {

  private static final String DO_TRANSACTIONAL = "doTransactional";

  private final TransactionalTasks tasks = new TransactionalTasks();
  private final List<TransactionTestEntity> storedEntities = new ArrayList<TransactionTestEntity>();

  @Inject
  private Injector injector;
  @Inject
  private UnitOfWork unitOfWork;
  @Inject
  private EntityManagerProvider emProvider;

  /**
   * Schedules a task for execution by this worker.
   * If more than one task are scheduled they will be called in the order they have been
   * scheduled.
   *
   * @param taskType the task to schedule for execution.
   */
  public void scheduleTask(Class<? extends TransactionalTask> taskType) {
    checkTransactionalAnnotation(taskType);
    TransactionalTask task = injector.getInstance(taskType);
    task.setWorker(this);
    tasks.add(task);
  }

  private void checkTransactionalAnnotation(Class<? extends TransactionalTask> taskType) {
    try {
      final Method method = taskType.getMethod(DO_TRANSACTIONAL);
      final LocalTransactional annotation = method.getAnnotation(LocalTransactional.class);
      checkNotNull(annotation,
          "@Transactional annotation missing on %s.%s", taskType.getSimpleName(), DO_TRANSACTIONAL);
    }
    catch (NoSuchMethodException e) {
      // should never occure.
      throw new RuntimeException(e);
    }
  }

  /**
   * Executes the previously specified tasks. All entities which were stored using
   * {@link TransactionalTask#storeEntity(TransactionTestEntity)} are collected by the worker.<p/>
   *
   * @return a list with the 'stored' entities
   */
  public void doTasks() {
    checkState(tasks.hasTasks(), "no tasks have been added to the worker.");
    checkState(tasks.hasNext(), "doTasks() has already been executed.");
    checkState(!unitOfWork.isActive(), "Active UnitOfWork found.");

    try {
      doNextTask();
    }
    catch (TestException e) {
      // do nothing
    }
    catch (RuntimeTestException e) {
      // do nothing
    }

    checkState(!tasks.hasNext(), "One of the tasks forgot to call doOtherTasks().");
    checkState(!unitOfWork.isActive(), "Active UnitOfWork after tasks found.");
  }

  /**
   * Check all stored entities if they actually have been persisted in the DB.
   */
  @LocalTransactional
  public void assertAllEntitesHaveBeenPersisted() {
    checkState(!storedEntities.isEmpty(), "no entities to check");
    for(TransactionTestEntity storedEntity:storedEntities) {
      assertNotNull("At least one entity which should have been peristed was NOT found in the DB. "
          + tasks, emProvider.get().find(TransactionTestEntity.class, storedEntity.getId()));
    }
  }

  /**
   * Check all stored entities if they actually have NOT been persisted in the DB.
   */
  @LocalTransactional
  public void assertNoEntityHasBeenPersisted() {
    checkState(!storedEntities.isEmpty(), "no entities to check");
    for(TransactionTestEntity storedEntity:storedEntities) {
      assertNull("At least one entity which should NOT have been peristed was found in the DB. "
          + tasks, emProvider.get().find(TransactionTestEntity.class, storedEntity.getId()));
    }
  }

  @VisibleForTesting
  void doNextTask() throws TestException {
    if(tasks.hasNext()) {
      TransactionalTask task = tasks.next();
      try {
        task.doTransactional();
      }
      finally {
        storedEntities.addAll(task.getPersistedEntities());
      }
    }
  }


  /**
   * Class holding the tasks of a worker.
   *
   * @author Stephan Classen
   */
  private static class TransactionalTasks {

    private final List<TransactionalTask> tasks = new ArrayList<TransactionalTask>();
    private int pos = 0;

    /**
     * @return {@code true} if there have already been tasks added.
     */
    public boolean hasTasks() {
      return ! tasks.isEmpty();
    }

    /**
     * Add a task.
     *
     * @param task the task to add.
     * @throws IllegalStateException if {@link #next()} has already been called on this instance.
     */
    public void add(TransactionalTask task) {
      checkState(pos == 0);
      tasks.add(task);
    }

    /**
     * @return {@code true} if there are more tasks.
     */
    public boolean hasNext() {
      return pos < tasks.size();
    }

    /**
     * @return the next task.
     * @throws IndexOutOfBoundsException if there are no more tasks.
     */
    public TransactionalTask next() {
      TransactionalTask result = tasks.get(pos);
      pos++;
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Tasks[");
      String separator = "";
      for (TransactionalTask t: tasks) {
        sb.append(separator);
        final String taskType = t.getClass().getSimpleName();
        sb.append(taskType.replaceAll("\\$\\$EnhancerByGuice\\$\\$.*", ""));
        separator = ", ";
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
