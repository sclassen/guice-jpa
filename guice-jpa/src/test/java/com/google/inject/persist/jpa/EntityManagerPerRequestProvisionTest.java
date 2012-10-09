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
 * A test around providing sessions (starting, closing etc.)
 *
 * This class was copied from guice-persist v3.0 and adopted to fit the API of guice-jpa
 * 
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class EntityManagerPerRequestProvisionTest extends TestCase {
  private Injector injector;

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

  public void testEntityManagerLifecyclePerTxn() {
    //obtain em
    JpaDao dao = injector.getInstance(JpaDao.class);

    //obtain same em again (bound to txn)
    JpaTestEntity te = new JpaTestEntity();

    dao.persist(te);

    //im not sure this hack works...
    assertEquals("Entity managers closed inside same thread-scope",
        injector.getInstance(EntityManagerProvider.class).get(), JpaDao.emProvider.get());

    //try to start a new em in a new txn
    dao = injector.getInstance(JpaDao.class);

    assertTrue("EntityManager was closed and reopened around txn"
        + " (persistent object does not persist)", dao.contains(te));
  }

  public void testEntityManagerLifecyclePerTxn2() {
    //obtain em
    JpaDao dao = injector.getInstance(JpaDao.class);

    //obtain same em again (bound to txn)
    JpaTestEntity te = new JpaTestEntity();

    dao.persist(te);

    //im not sure this hack works...
    assertEquals("Duplicate entity managers crossing-scope",
        injector.getInstance(EntityManagerProvider.class).get(), JpaDao.emProvider.get());
    assertEquals("Duplicate entity managers crossing-scope",
        injector.getInstance(EntityManagerProvider.class).get(), JpaDao.emProvider.get());

    //try to start a new em in a new txn
    dao = injector.getInstance(JpaDao.class);

    assertTrue("EntityManager was closed and reopened around txn"
        + " (persistent object doesnt persist)", dao.contains(te));
  }

  public static class JpaDao {
    static EntityManagerProvider emProvider;

    @Inject
    public JpaDao(EntityManagerProvider emProvider) {
      JpaDao.emProvider = emProvider;
    }

    @Transactional
    public <T> void persist(T t) {
      EntityManager em = emProvider.get();
      assertTrue("em is not open!", em.isOpen());
      assertTrue("no active txn!", em.getTransaction().isActive());
      em.persist(t);

      assertTrue("Persisting object failed", em.contains(t));
    }

    @Transactional
    public <T> boolean contains(T t) {
      return emProvider.get().contains(t);
    }
  }
}