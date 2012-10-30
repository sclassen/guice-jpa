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

import junit.framework.TestCase;

import com.github.sclassen.guicejpa.testframework.TransactionalTask;
import com.github.sclassen.guicejpa.testframework.TransactionalWorker;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnAnyThrowingNone;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnAnyThrowingRuntimeTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnAnyThrowingTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnNoneThrowingNone;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnNoneThrowingRuntimeTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnNoneThrowingTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnRuntimeTestExceptionThrowingNone;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnRuntimeTestExceptionThrowingTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnTestExceptionThrowingNone;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnTestExceptionThrowingRuntimeTestException;
import com.github.sclassen.guicejpa.testframework.tasks.TaskRollingBackOnTestExceptionThrowingTestException;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Tests running nested transactions.
 * The test make us of the testframework.
 * Since the test is running a loop only the injector is created directly in the test to ensure
 * that for every {@link TestVector} a new injector instance is used.
 *
 * @author Stephan Classen
 */
public class NestedTransactionTest extends TestCase {

  /**
   * All possible combination of {@link TransactionalTask}s
   * and if they should have been rolled back.
   */
  private static final TestVector[] TEST_VECTORS = {
    new TestVector(TaskRollingBackOnAnyThrowingNone.class, TaskRollingBackOnAnyThrowingNone.class,
        false),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class, TaskRollingBackOnNoneThrowingNone.class,
        false),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnNoneThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnAnyThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnNoneThrowingNone.class, TaskRollingBackOnAnyThrowingNone.class,
        false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnNoneThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnNoneThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnAnyThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnNoneThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnAnyThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnNoneThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingNone.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true),

    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnAnyThrowingNone.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnAnyThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnAnyThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnNoneThrowingNone.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnNoneThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnNoneThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingRuntimeTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnRuntimeTestExceptionThrowingTestException.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingNone.class, true),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingRuntimeTestException.class, false),
    new TestVector(TaskRollingBackOnTestExceptionThrowingTestException.class,
        TaskRollingBackOnTestExceptionThrowingTestException.class, true), };

  /**
   * Test which iterates over ALL possible combinations of inner and outer tasks.
   */
  public void testNestedTransactions() {
    final StringBuilder msg = new StringBuilder();
    for (TestVector v : TEST_VECTORS) {
      try {
        doTestNestedTransaction(v);
      }
      catch (AssertionError e) {
        msg.append("\n");
        msg.append(e.getMessage());
      }
    }
    if (msg.length() > 0) {
      fail(msg.toString());
    }
  }

  private void doTestNestedTransaction(TestVector testVector) {
    final PersistenceModule pm = new PersistenceModule();
    pm.addApplicationManagedPersistenceUnit("testUnit");
    final Injector injector = Guice.createInjector(pm);
    final PersistenceService persistService = injector.getInstance(PersistenceService.class);
    persistService.start();
    try {
      // given
      final TransactionalWorker worker = injector.getInstance(TransactionalWorker.class);
      worker.scheduleTask(testVector.getOuterTask());
      worker.scheduleTask(testVector.getInnerTask());

      // when
      worker.doTasks();

      // then
      if (testVector.shouldRollBack()) {
        worker.assertNoEntityHasBeenPersisted();
      }
      else {
        worker.assertAllEntitesHaveBeenPersisted();
      }
    }
    finally {
      persistService.stop();
    }

  }

  private static class TestVector {
    private final Class<? extends TransactionalTask> outerTask;
    private final Class<? extends TransactionalTask> innerTask;
    private final boolean shouldRollBack;

    public TestVector(Class<? extends TransactionalTask> outerTask,
        Class<? extends TransactionalTask> innerTask, boolean shouldRollBack) {
      this.outerTask = outerTask;
      this.innerTask = innerTask;
      this.shouldRollBack = shouldRollBack;
    }

    public Class<? extends TransactionalTask> getOuterTask() {
      return outerTask;
    }

    public Class<? extends TransactionalTask> getInnerTask() {
      return innerTask;
    }

    public boolean shouldRollBack() {
      return shouldRollBack;
    }
  }
}
