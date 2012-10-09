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

import java.util.Date;

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
 * Created with IntelliJ IDEA. On: 2/06/2007
 *
 * For instance, a session-per-request strategy will control the opening and closing of the EM at
 * its own (manual) discretion. As opposed to a transactional unit of work.
 *
 * This class was copied from guice-persist v3.0 and adopted to fit the API of guice-jpa
 * 
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 * @since 1.0
 */
public class ManualLocalTransactionsWithCustomMatcherTest extends TestCase {
  private Injector injector;
  private static final String UNIQUE_TEXT = "some unique text" + new Date();
  private static final String UNIQUE_TEXT_2 = "some other unique text" + new Date();

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
  public void tearDown() {
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(PersistenceService.class).stop();
  }

  public void testSimpleCrossTxnWork() {
    //pretend that the request was started here
    EntityManager em = injector.getInstance(EntityManagerProvider.class).get();

    JpaTestEntity entity = injector
        .getInstance(ManualLocalTransactionsWithCustomMatcherTest.TransactionalObject.class)
        .runOperationInTxn();
    injector.getInstance(ManualLocalTransactionsWithCustomMatcherTest.TransactionalObject.class)
        .runOperationInTxn2();

    //persisted entity should remain in the same em (which should still be open)
    assertTrue("EntityManager  appears to have been closed across txns!",
        injector.getInstance(EntityManagerProvider.class).get().contains(entity));
    assertTrue("EntityManager  appears to have been closed across txns!", em.contains(entity));
    assertTrue("EntityManager appears to have been closed across txns!", em.isOpen());

    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(UnitOfWork.class).begin();

    //try to query them back out
    em = injector.getInstance(EntityManagerProvider.class).get();
    assertNotNull(em.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", UNIQUE_TEXT).getSingleResult());
    assertNotNull(em.createQuery("from JpaTestEntity where text = :text")
        .setParameter("text", UNIQUE_TEXT_2).getSingleResult());
  }

  public static class TransactionalObject {
    @Inject EntityManagerProvider emProvider;

    @Transactional
    public JpaTestEntity runOperationInTxn() {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT);
      emProvider.get().persist(entity);

      return entity;
    }

    @Transactional
    public void runOperationInTxn2() {
      JpaTestEntity entity = new JpaTestEntity();
      entity.setText(UNIQUE_TEXT_2);
      emProvider.get().persist(entity);
    }

  }
}
