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

import java.io.IOException;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

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
 * This class was copied from guice-persist v3.0 and adopted to fit the API of guice-jpa
 * 
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class ManagedLocalTransactionsAcrossRequestTest extends TestCase {
  private Injector injector;
  private static final String UNIQUE_TEXT = "some unique text" + new Date();
  private static final String UNIQUE_TEXT_MERGE = "meRG_Esome unique text" + new Date();
  private static final String UNIQUE_TEXT_MERGE_FORDF = "aSdoaksdoaksdmeRG_Esome unique text"
      + new Date();
  private static final String TRANSIENT_UNIQUE_TEXT = "some other unique text" + new Date();

  @Override
  public void setUp() {
    PersistenceModule pm = new PersistenceModule();
    pm.addApplicationManagedPersistenceUnit("testUnit");
    injector = Guice.createInjector(pm);

    //startup persistence
    injector.getInstance(PersistenceService.class).start();
    injector.getInstance(UnitOfWork.class).begin();
  }

  @Override
  public final void tearDown() {
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(PersistenceService.class).stop();
  }

  public void testSimpleTransaction() {
    injector.getInstance(TransactionalObject.class).runOperationInTxn();

    EntityManager em = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse(em.getTransaction().isActive());

    //test that the data has been stored
    Object result = em.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", UNIQUE_TEXT).getSingleResult();

    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(UnitOfWork.class).begin();

    assertTrue("odd result returned fatal", result instanceof JpaTestEntity);

    assertEquals("queried entity did not match--did automatic txn fail?", UNIQUE_TEXT,
        ((JpaTestEntity) result).getText());
  }

  public void testSimpleTransactionWithMerge() {
    EntityManager emOrig = injector.getInstance(EntityManagerProvider.class).get();
    JpaTestEntity entity = injector.getInstance(TransactionalObject.class)
        .runOperationInTxnWithMerge();

    assertNotNull("Entity was not given an id (was not persisted correctly?)", entity.getId());

    EntityManager em = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse(em.getTransaction().isActive());

    //test that the data has been stored
    assertTrue("Em was closed after txn!", em.isOpen());
    assertEquals("Em was not kept open across txns", emOrig, em);
    assertTrue("Merge did not store state or did not return persistent copy", em.contains(entity));

    Object result = em.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", UNIQUE_TEXT_MERGE).getSingleResult();

    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(UnitOfWork.class).begin();

    assertTrue(result instanceof JpaTestEntity);

    assertEquals("queried entity did not match--did automatic txn fail?", UNIQUE_TEXT_MERGE,
        ((JpaTestEntity) result).getText());
    injector.getInstance(UnitOfWork.class).end();

  }

  public void disabled_testSimpleTransactionWithMergeAndDF() {
    EntityManager emOrig = injector.getInstance(EntityManagerProvider.class).get();
    JpaTestEntity entity = injector.getInstance(TransactionalObject.class)
        .runOperationInTxnWithMergeForDf();

    EntityManager em = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse("txn was not closed by transactional service", em.getTransaction().isActive());

    //test that the data has been stored
    assertTrue("Em was closed after txn!", em.isOpen());
    assertEquals("Em was not kept open across txns", emOrig, em);
    assertTrue("Merge did not store state or did not return persistent copy", em.contains(entity));
  }

  public void testSimpleTransactionRollbackOnChecked() {
    try {
      injector.getInstance(TransactionalObject.class).runOperationInTxnThrowingChecked();
    } catch (IOException e) {
      //ignore
      injector.getInstance(UnitOfWork.class).end();
      injector.getInstance(UnitOfWork.class).begin();
    }

    EntityManager em = injector.getInstance(EntityManagerProvider.class).get();

    assertFalse("Previous EM was not closed by transactional service (rollback didnt happen?)", em
        .getTransaction().isActive());

    //test that the data has been stored
    try {
      em.createQuery("from JpaTestEntity where text = :text")
          .setParameter("text", TRANSIENT_UNIQUE_TEXT).getSingleResult();
      fail();
    } catch (NoResultException e) {
      // ok when no result found
    }
  }

  public void testSimpleTransactionRollbackOnUnchecked() {
    try {
      injector.getInstance(TransactionalObject.class).runOperationInTxnThrowingUnchecked();
    } catch (RuntimeException re) {
      //ignore
      injector.getInstance(UnitOfWork.class).end();
      injector.getInstance(UnitOfWork.class).begin();
    }

    EntityManager em = injector.getInstance(EntityManagerProvider.class).get();
    assertFalse("Session was not closed by transactional service (rollback didnt happen?)", em
        .getTransaction().isActive());

    try {
      em.createQuery("from JpaTestEntity where text = :text")
          .setParameter("text", TRANSIENT_UNIQUE_TEXT).getSingleResult();
      injector.getInstance(UnitOfWork.class).end();
      fail();
    } catch (NoResultException e) {
      // ok when no result found
    }
  }

  public static class TransactionalObject {
    private final EntityManagerProvider emProvider;

    @Inject
    public TransactionalObject(EntityManagerProvider emProvider) {
      this.emProvider = emProvider;
    }

    @Transactional
    public void runOperationInTxn() {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT);
      emProvider.get().persist(entity);
    }

    @Transactional
    public JpaTestEntity runOperationInTxnWithMerge() {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT_MERGE);
      return emProvider.get().merge(entity);
    }

    @Transactional
    public JpaTestEntity runOperationInTxnWithMergeForDf() {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT_MERGE_FORDF);
      return emProvider.get().merge(entity);
    }

    @Transactional(rollbackOn = IOException.class)
    public void runOperationInTxnThrowingChecked() throws IOException {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(TRANSIENT_UNIQUE_TEXT);
      emProvider.get().persist(entity);

      throw new IOException();
    }

    @Transactional
    public void runOperationInTxnThrowingUnchecked() {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(TRANSIENT_UNIQUE_TEXT);
      emProvider.get().persist(entity);

      throw new IllegalStateException();
    }

  }
}