/**
 * Copyright (C) 2010 Google, Inc.
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

package com.google.inject.persist.jpa;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import junit.framework.TestCase;

import com.github.sclassen.guicejpa.EntityManagerProvider;
import com.github.sclassen.guicejpa.Transactional;
import com.github.sclassen.guicejpa.PersistenceModule;
import com.github.sclassen.guicejpa.PersistenceService;
import com.github.sclassen.guicejpa.UnitOfWork;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * This test asserts class level @Transactional annotation behavior.
 *
 * Class-level @Transactional is a shortcut if all non-private methods in the class are meant to be
 * transactional.
 *
 * This class was copied from guice-persist v3.0 and adopted to fit the API of guice-jpa
 * 
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class ClassLevelManagedLocalTransactionsTest extends TestCase {
  private Injector injector;
  private static final String UNIQUE_TEXT = "JPAsome unique text88888" + new Date();
  private static final String UNIQUE_TEXT_2 = "JPAsome asda unique teasdalsdplasdxt" + new Date();
  private static final String TRANSIENT_UNIQUE_TEXT = "JPAsome other unique texaksoksojadasdt"
      + new Date();

  public void setUp() {
    PersistenceModule pm = new PersistenceModule();
    pm.addApplicationManagedPersistenceUnit("testUnit");
    injector = Guice.createInjector(pm);

    //startup persistence
    injector.getInstance(PersistenceService.class).start();
    injector.getInstance(UnitOfWork.class).begin();
  }

  public void tearDown() {
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(PersistenceService.class).stop();
    injector = null;
  }

  public void testSimpleTransaction() {
    injector.getInstance(TransactionalObject.class).runOperationInTxn();

    EntityManager session = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse("EntityManager was not closed by transactional service",
        session.getTransaction().isActive());

    //test that the data has been stored
    session.getTransaction().begin();
    Object result = session.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", UNIQUE_TEXT).getSingleResult();

    session.getTransaction().commit();

    assertTrue("odd result returned fatal", result instanceof JpaTestEntity);

    assertEquals("queried entity did not match--did automatic txn fail?",
        UNIQUE_TEXT, (((JpaTestEntity) result).getText()));
  }

  public void testSimpleTransactionRollbackOnChecked() {
    try {
      injector.getInstance(TransactionalObject2.class).runOperationInTxnThrowingChecked();
    } catch (IOException e) {
      //ignore
    }

    EntityManager session = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse("EntityManager was not closed by transactional service (rollback didnt happen?)",
        session.getTransaction().isActive());

    //test that the data has been stored
    session.getTransaction().begin();
    List<?> result = session.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", TRANSIENT_UNIQUE_TEXT).getResultList();

    session.getTransaction().commit();

    assertTrue("a result was returned! rollback sure didnt happen!!!", result.isEmpty());
  }

  public void testSimpleTransactionRollbackOnCheckedExcepting() {
    try {
      injector.getInstance(TransactionalObject3.class).runOperationInTxnThrowingCheckedExcepting();
      fail("Exception was not thrown by test txn-al method!");
    } catch (IOException e) {
      //ignored
    }

    EntityManager session = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse("Txn was not closed by transactional service (commit didnt happen?)",
        session.getTransaction().isActive());

    //test that the data has been stored
    session.getTransaction().begin();
    Object result = session.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", UNIQUE_TEXT_2).getSingleResult();

    session.getTransaction().commit();

    assertNotNull("a result was not returned! rollback happened anyway (ignore failed)!!!",
        result);
  }

  public void testSimpleTransactionRollbackOnUnchecked() {
    try {
      injector.getInstance(TransactionalObject4.class).runOperationInTxnThrowingUnchecked();
    } catch (RuntimeException re) {
      //ignore
    }

    EntityManager session = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse("EntityManager was not closed by transactional service (rollback didnt happen?)",
        session.getTransaction().isActive());

    //test that the data has been stored
    session.getTransaction().begin();
    List<?> result = session.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", TRANSIENT_UNIQUE_TEXT).getResultList();

    session.getTransaction().commit();

    assertTrue("a result was returned! rollback sure didnt happen!!!",
        result.isEmpty());
  }

  @Transactional
  public static class TransactionalObject {
    @Inject EntityManagerProvider sessionProvider;

    public void runOperationInTxn() {
      EntityManager session = sessionProvider.get();
      assertTrue(session.getTransaction().isActive());
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT);
      session.persist(entity);
    }

  }

  @Transactional
  public static class TransactionalObject4 {
    @Inject EntityManagerProvider sessionProvider;

    @Transactional
    public void runOperationInTxnThrowingUnchecked() {
      EntityManager session = sessionProvider.get();
      assertTrue(session.getTransaction().isActive());
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(TRANSIENT_UNIQUE_TEXT);
      session.persist(entity);

      throw new IllegalStateException();
    }
  }

  @Transactional(rollbackOn = IOException.class, ignore = FileNotFoundException.class)
  public static class TransactionalObject3 {
    @Inject EntityManagerProvider sessionProvider;

    public void runOperationInTxnThrowingCheckedExcepting() throws IOException {
      EntityManager session = sessionProvider.get();
      assertTrue(session.getTransaction().isActive());
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT_2);
      session.persist(entity);

      throw new FileNotFoundException();
    }
  }

  @Transactional(rollbackOn = IOException.class)
  public static class TransactionalObject2 {
    @Inject EntityManagerProvider sessionProvider;

    public void runOperationInTxnThrowingChecked() throws IOException {
      EntityManager session = sessionProvider.get();
      assertTrue(session.getTransaction().isActive());
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(TRANSIENT_UNIQUE_TEXT);
      session.persist(entity);

      throw new IOException();
    }
  }
}
