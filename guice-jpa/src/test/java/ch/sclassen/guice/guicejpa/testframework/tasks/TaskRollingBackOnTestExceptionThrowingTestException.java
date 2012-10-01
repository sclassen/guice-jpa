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
package ch.sclassen.guice.guicejpa.testframework.tasks;

import ch.sclassen.guice.guicejpa.LocalTransactional;
import ch.sclassen.guice.guicejpa.testframework.TransactionTestEntity;
import ch.sclassen.guice.guicejpa.testframework.TransactionalTask;
import ch.sclassen.guice.guicejpa.testframework.exceptions.RuntimeTestException;
import ch.sclassen.guice.guicejpa.testframework.exceptions.TestException;

/**
 * Task which stores an entity and will:
 *  - roll back on {@link TestException}.
 *  - throw a new {@link TestException}.
 *
 * @author Stephan Classen
 */
public class TaskRollingBackOnTestExceptionThrowingTestException extends TransactionalTask {

  /**
   * {@inheritDoc}
   */
  @Override
  @LocalTransactional(rollbackOn=TestException.class)
  public void doTransactional() throws TestException, RuntimeTestException {
    storeEntity(new TransactionTestEntity());
    doOtherTasks();
    throw new TestException(getClass().getSimpleName());
  }

}
