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

import javax.persistence.Query;

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
public class JpaWorkManagerTest extends TestCase {
  private Injector injector;
  private static final String UNIQUE_TEXT_3 = JpaWorkManagerTest.class.getSimpleName()
      + "CONSTRAINT_VIOLATING some other unique text" + new Date();

  @Override
  public void setUp() {
    PersistenceModule pm = new PersistenceModule();
    pm.addApplicationManagedPersistenceUnit("testUnit");
    injector = Guice.createInjector(pm);

    //startup persistence
    injector.getInstance(PersistenceService.class).start();
  }

  @Override
  public void tearDown() {
    injector.getInstance(PersistenceService.class).stop();
  }

  public void testWorkManagerInSession() {
    injector.getInstance(UnitOfWork.class).begin();
    try {
      injector.getInstance(TransactionalObject.class).runOperationInTxn();
    } finally {
      injector.getInstance(UnitOfWork.class).end();

    }

    injector.getInstance(UnitOfWork.class).begin();
    injector.getInstance(EntityManagerProvider.class).get().getTransaction().begin();
    try {
      final Query query = injector.getInstance(EntityManagerProvider.class).get()
          .createQuery("select e from JpaTestEntity as e where text = :text");

      query.setParameter("text", UNIQUE_TEXT_3);
      final Object o = query.getSingleResult();

      assertNotNull("no result!!", o);
      assertTrue("Unknown type returned " + o.getClass(), o instanceof JpaTestEntity);
      JpaTestEntity ent = (JpaTestEntity) o;

      assertEquals("Incorrect result returned or not persisted properly" + ent.getText(),
          UNIQUE_TEXT_3, ent.getText());

    } finally {
      injector.getInstance(EntityManagerProvider.class).get().getTransaction().commit();
      injector.getInstance(UnitOfWork.class).end();
    }
  }

  public void testCloseMoreThanOnce() {
    injector.getInstance(PersistenceService.class).stop();
    injector.getInstance(PersistenceService.class).stop();
  }

  public static class TransactionalObject {
    @Inject EntityManagerProvider emProvider;

    @Transactional
    public void runOperationInTxn() {
      JpaTestEntity testEntity = new JpaTestEntity();

      testEntity.setText(UNIQUE_TEXT_3);
      emProvider.get().persist(testEntity);
    }

    @Transactional
    public void runOperationInTxnError() {

      JpaTestEntity testEntity = new JpaTestEntity();

      testEntity.setText(UNIQUE_TEXT_3 + "transient never in db!" + hashCode());
      emProvider.get().persist(testEntity);
    }
  }
}