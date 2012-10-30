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

import java.util.Properties;

import junit.framework.TestCase;

import com.github.sclassen.guicejpa.EntityManagerProvider;
import com.github.sclassen.guicejpa.PersistenceModule;
import com.github.sclassen.guicejpa.PersistenceService;
import com.github.sclassen.guicejpa.UnitOfWork;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This class was copied from guice-persist v3.0 and adopted to fit the API of guice-jpa
 * 
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class CustomPropsEntityManagerFactoryProvisionTest extends TestCase {
  private Injector injector;

  @Override
  public void setUp() {
    Properties props = new Properties();
    props.put("blah", "blah");

    PersistenceModule pm = new PersistenceModule();
    pm.addApplicationManagedPersistenceUnit("testUnit", props);
    injector = Guice.createInjector(pm);
  }

  @Override
  public final void tearDown() {
    injector.getInstance(UnitOfWork.class).end();
    injector.getInstance(PersistenceService.class).stop();
  }

  public void testSessionCreateOnInjection() {

    assertEquals("SINGLETON VIOLATION " + UnitOfWork.class.getName(),
        injector.getInstance(UnitOfWork.class),
        injector.getInstance(UnitOfWork.class));

    //startup persistence
    injector.getInstance(PersistenceService.class).start();
    injector.getInstance(UnitOfWork.class).begin();

    //obtain em
    assertTrue(injector.getInstance(EntityManagerProvider.class).get().isOpen());
  }
}
