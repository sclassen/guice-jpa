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
package com.github.sclassen.guicejpa;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.matcher.Matchers.any;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

/**
 * Main module of the jpa-persistence guice extension.
 * <p/>
 * Add either a {@link ApplicationManagedPersistenceUnitModule} or a
 * {@link ContainerManagedPersistenceUnitModule} per persistence unit using the methods
 * <ul>
 *    <li>{@link #add(ApplicationManagedPersistenceUnitModule)}</li>
 *    <li>{@link #addApplicationManagedPersistenceUnit(String)}</li>
 *    <li>{@link #addApplicationManagedPersistenceUnit(String, Properties)}</li>
 *    <li>{@link #add(ContainerManagedPersistenceUnitModule)}</li>
 *    <li>{@link #addContainerManagedPersistenceUnit(String)}</li>
 *    <li>{@link #addContainerManagedPersistenceUnit(String, Properties)}</li>
 * </ul>
 * <p/>
 * If container managed persistence units have been added and JTA transactions are supported.
 * Use {@link #setUserTransactionJndiName(String)} to define the JNDI name of the
 * {@link UserTransaction} provided by the container.
 *
 * @author Stephan Classen
 */
public final class PersistenceModule extends AbstractModule {

  // ---- Members

  /** List of all module builders. */
  private final List<PersistenceUnitBuilder> moduleBuilders = new ArrayList<PersistenceUnitBuilder>();

  /**
   * List of all persistence unit modules.
   * If this list is empty it means that configure has not yet been called
   */
  private final List<AbstractPersistenceUnitModule> modules = new ArrayList<AbstractPersistenceUnitModule>();

  /** Container for holding all registered persistence units. */
  private final PersistenceUnitContainer puContainer = new PersistenceUnitContainer();

  /**
   * The JNDI name to lookup the {@link UserTransaction}.
   */
  private String utJndiName;

  /**
   * The {@link UserTransactionFacade}.
   */
  private UserTransactionFacade utFacade = null;


  // ---- Methods

  /**
   * Adds an application managed persistence unit.
   *
   * @param puName the name of the persistence unit as specified in the persistence.xml.
   * @return a builder to further configure the persistence unit.
   */
  public PersistenceUnitBuilder addApplicationManagedPersistenceUnit(String puName) {
    return add(new ApplicationManagedPersistenceUnitModule(puName));
  }

  /**
   * Adds an application managed persistence unit.
   *
   * @param puName the name of the persistence unit as specified in the persistence.xml.
   * @param properties the properties to pass to the {@link EntityManagerFactory}.
   * @return a builder to further configure the persistence unit.
   */
  public PersistenceUnitBuilder addApplicationManagedPersistenceUnit(String puName,
      Properties properties) {
    return add(new ApplicationManagedPersistenceUnitModule(puName, properties));
  }

  /**
   * Adds an application managed persistence unit.
   *
   * @param module the module of the persistence unit.
   * @return a builder to further configure the persistence unit.
   */
  public PersistenceUnitBuilder add(ApplicationManagedPersistenceUnitModule module) {
    ensureConfigurHasNotYetBeenExecuted();
    checkNotNull(module);
    final PersistenceUnitBuilder builder = new PersistenceUnitBuilder(module);
    moduleBuilders.add(builder);
    return builder;
  }

  /**
   * Adds an container managed persistence unit.
   *
   * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}.
   * @return a builder to further configure the persistence unit.
   */
  public PersistenceUnitBuilder addContainerManagedPersistenceUnit(String emfJndiName) {
    return add(new ContainerManagedPersistenceUnitModule(emfJndiName));
  }

  /**
   * Adds an container managed persistence unit.
   *
   * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}.
   * @param properties the properties to pass to the {@link EntityManager}.
   * @return a builder to further configure the persistence unit.
   */
  public PersistenceUnitBuilder addContainerManagedPersistenceUnit(String emfJndiName,
      Properties properties) {
    return add(new ContainerManagedPersistenceUnitModule(emfJndiName, properties));
  }

  /**
   * Adds an container managed persistence unit.
   *
   * @param module the module of the persistence unit.
   * @return a builder to further configure the persistence unit.
   */
  public PersistenceUnitBuilder add(ContainerManagedPersistenceUnitModule module) {
    ensureConfigurHasNotYetBeenExecuted();
    checkNotNull(module);
    final PersistenceUnitBuilder builder = new PersistenceUnitBuilder(module);
    moduleBuilders.add(builder);
    return builder;
  }

  /**
   * Setter for defining the JNDI name of the container managed {@link UserTransaction}.
   *
   * @param utJndiName the JNDI name of the container managed {@link UserTransaction}.
   */
  public void setUserTransactionJndiName(String utJndiName) {
    ensureConfigurHasNotYetBeenExecuted();
    this.utJndiName = utJndiName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configure() {
    if (configureHasNotBeenExecutedYet()) {
      if (0 == moduleBuilders.size()) {
        addError("no persistence units defined. At least one persistence unit is required.");
        return;
      }
      initUserTransactionFacade();
      for (PersistenceUnitBuilder builder : moduleBuilders) {
        final AbstractPersistenceUnitModule module = builder.build();
        puContainer.add(module.getPersistenceService(), module.getUnitOfWork());
        modules.add(module);
      }
    }

    for (AbstractPersistenceUnitModule module : modules) {
      install(module);

      final Matcher<AnnotatedElement> matcher = Matchers.annotatedWith(Transactional.class);
      final MethodInterceptor transactionInterceptor = module.getTransactionInterceptor(utFacade);

      bindInterceptor(matcher, any(), transactionInterceptor);
      bindInterceptor(any(), matcher, transactionInterceptor);
    }

    bind(PersistenceService.class).annotatedWith(AllPersistenceUnits.class).toInstance(puContainer);
    bind(UnitOfWork.class).annotatedWith(AllPersistenceUnits.class).toInstance(puContainer);
    bind(PersistenceFilter.class).toInstance(new PersistenceFilter(puContainer));
  }

  /**
   * @return {@code true} if {@link #configure()} has not yet been invoked.
   */
  private boolean configureHasNotBeenExecutedYet() {
    return modules.size() == 0;
  }

  /**
   * Make sure that the {@link #configure()} method has not been executed yet.
   */
  private void ensureConfigurHasNotYetBeenExecuted() {
    if (configureHasNotBeenExecutedYet()) {
      return;
    }
    throw new IllegalStateException("cannot change a module after creating the injector.");
  }

  /**
   * Initializes the field {@link #utFacade} with the {@link UserTransaction} obtained by a
   * JNDI lookup.
   */
  private void initUserTransactionFacade() {
    if (null != utJndiName) {
      try {
        final InitialContext ctx = new InitialContext();
        UserTransaction txn = (UserTransaction) ctx.lookup(utJndiName);
        utFacade = new UserTransactionFacade(txn);
      } catch (NamingException e) {
        addError("lookup for UserTransaction with JNDI name '%s' failed", utJndiName);
      }
    }
  }

}
