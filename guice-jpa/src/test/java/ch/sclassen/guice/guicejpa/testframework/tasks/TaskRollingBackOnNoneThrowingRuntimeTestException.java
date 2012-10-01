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

package ch.sclassen.guice.guicejpa.testframework.tasks;

import ch.sclassen.guice.guicejpa.LocalTransactional;
import ch.sclassen.guice.guicejpa.testframework.TransactionTestEntity;
import ch.sclassen.guice.guicejpa.testframework.TransactionalTask;
import ch.sclassen.guice.guicejpa.testframework.exceptions.RuntimeTestException;
import ch.sclassen.guice.guicejpa.testframework.exceptions.TestException;

/**
 * Task which stores an entity and will:
 *  - never roll back.
 *  - throw a new {@link RuntimeTestException}.
 *
 * @author Stephan Classen
 */
public class TaskRollingBackOnNoneThrowingRuntimeTestException extends TransactionalTask {

  /**
   * {@inheritDoc}
   */
  @Override
  @LocalTransactional(ignore = Exception.class)
  public void doTransactional() throws TestException, RuntimeTestException {
    storeEntity(new TransactionTestEntity());
    doOtherTasks();
    throw new RuntimeTestException(getClass().getSimpleName());
  }

}
