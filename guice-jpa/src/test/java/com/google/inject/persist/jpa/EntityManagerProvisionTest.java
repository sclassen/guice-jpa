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
public class EntityManagerProvisionTest extends TestCase {
  private Injector injector;

  public void setUp() {
    PersistenceModule pm = new PersistenceModule();
    pm.addApplicationManagedPersistenceUnit("testUnit");
    injector = Guice.createInjector(pm);

    //startup persistence
    injector.getInstance(PersistenceService.class).start();
  }

  public final void tearDown() {
    injector.getInstance(PersistenceService.class).stop();
  }

  public void testEntityManagerLifecyclePerTxn() {
    //obtain em
    JpaDao dao = injector.getInstance(JpaDao.class);

    //obtain same em again (bound to txn)
    JpaTestEntity te = new JpaTestEntity();

    dao.persist(te);

    //im not sure this hack works...
    injector.getInstance(UnitOfWork.class).begin();
    assertFalse("Duplicate entity managers crossing-scope",
        dao.lastEm.equals(injector.getInstance(EntityManagerProvider.class).get()));
    injector.getInstance(UnitOfWork.class).end();

    //try to start a new em in a new txn
    dao = injector.getInstance(JpaDao.class);

    assertFalse("EntityManager wasnt closed and reopened properly around txn"
        + " (persistent object persists)", dao.contains(te));
  }

  public void testEntityManagerLifecyclePerTxn2() {
    //obtain em
    JpaDao dao = injector.getInstance(JpaDao.class);

    //obtain same em again (bound to txn)
    JpaTestEntity te = new JpaTestEntity();

    dao.persist(te);

    //im not sure this hack works...
    injector.getInstance(UnitOfWork.class).begin();
    assertFalse("Duplicate entity managers crossing-scope",
        dao.lastEm.equals(injector.getInstance(EntityManagerProvider.class).get()));
    injector.getInstance(UnitOfWork.class).end();

    //try to start a new em in a new txn
    dao = injector.getInstance(JpaDao.class);

    assertFalse("EntityManager wasnt closed and reopened properly around txn"
        + " (persistent object persists)", dao.contains(te));
  }

  public static class JpaDao {
    private final EntityManagerProvider emProvider;
    EntityManager lastEm;

    @Inject
    public JpaDao(EntityManagerProvider emProvider) {
     this.emProvider = emProvider;
    }

    @Transactional
    public <T> void persist(T t) {
      lastEm = emProvider.get();
      assertTrue("em is not open!", lastEm.isOpen());
      assertTrue("no active txn!", lastEm.getTransaction().isActive());
      lastEm.persist(t);

      assertTrue("Persisting object failed", lastEm.contains(t));
    }

    @Transactional
    public <T> boolean contains(T t) {
      if (null == lastEm) {
        lastEm = emProvider.get();
      }
      return lastEm.contains(t);
    }
  }
}
