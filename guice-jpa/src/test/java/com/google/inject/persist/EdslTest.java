package com.google.inject.persist;

import junit.framework.TestCase;

import com.github.sclassen.guicejpa.PersistenceModule;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Stage;

/**
 * This class was copied from guice-persist v3.0 and adopted to fit the API of guice-jpa
 * 
 * @author dhanji@google.com (Dhanji R. Prasanna)
 */
public class EdslTest extends TestCase {

  public void testModuleConfigUsingJpa() throws Exception {
    Guice.createInjector(Stage.PRODUCTION, new AbstractModule() {
      @Override
      protected void configure() {
        PersistenceModule pm = new PersistenceModule();
        pm.addApplicationManagedPersistenceUnit("myunit");
        install(pm);
        binder().requireExplicitBindings();
      };
    });
  }
}
