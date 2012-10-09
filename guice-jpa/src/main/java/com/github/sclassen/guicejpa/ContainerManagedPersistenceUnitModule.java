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

import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * Persistence module for an application managed persistence unit.
 * <p/>
 * Use the {@link ContainerManagedPersistenceUnitBuilder} to configure an instance of this class.
 * <p/>
 * This is a private module which will expose the following bindings:
 * <ul>
 *    <li>{@link UnitOfWork}</li>
 *    <li>{@link EntityManagerProvider}</li>
 * </ul>
 * If an annotation has been defined for this module the above classes are exposed with this
 * annotation. Within the private module the above classes are also binded without any annotation.
 * <p/>
 * You can extend this class and override {@link #configurePersistence()} to bind and expose
 * additional classes within this private module. This is useful if you require injection of the
 * above classes without annotation.
 *
 * @author Stephan Classen
 */
public class ContainerManagedPersistenceUnitModule extends AbstractPersistenceUnitModule {

  // ---- Members

  /** Provider for {@link EntityManagerFactory} */
  private final ContainerManagedEntityManagerFactoryProvider emfProvider;

  /**
   * Annotation for the transactional methods. This defines if the PU uses local or global
   * Transaction
   */
  private TransactionType transactionType = TransactionType.LOCAL;


  // ---- Constructors

  /**
   * Constructor.
   *
   * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}.
   */
  public ContainerManagedPersistenceUnitModule(String emfJndiName) {
    this(emfJndiName, new Properties());
  }

  /**
   * Constructor.
   *
   * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}.
   * @param properties the additional properties. Theses override the ones defined in the persistence.xml.
   */
  public ContainerManagedPersistenceUnitModule(String emfJndiName, Properties properties) {
    this(new ContainerManagedEntityManagerFactoryProvider(emfJndiName), properties);
    checkNotNull(emfJndiName);
  }

  /**
   * Constructor.
   *
   * @param emfProvider the provider for {@link EntityManagerFactory}.
   * @param properties the additional properties. Theses override the ones defined in the persistence.xml.
   */
  private ContainerManagedPersistenceUnitModule(
      ContainerManagedEntityManagerFactoryProvider emfProvider, Properties properties) {
    super(new EntityManagerProviderImpl(emfProvider, properties));
    checkNotNull(properties);
    this.emfProvider = emfProvider;
  }


  // ---- Methods

  /**
   * @return the type of transaction used for this persistence unit.
   */
  final TransactionType getTransactionType() {
    return transactionType;
  }


  /**
   * Sets the type of transaction to use for this persistence unit.
   * 
   * @param transactionType
   */
  final void setTransactionType(TransactionType transactionType) {
    this.transactionType = transactionType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  final MethodInterceptor getTxnInterceptor(EntityManagerProviderImpl emProvider,
      UserTransactionProvider utProvider) {
    if (TransactionType.LOCAL == transactionType) {
      return new LocalTxnInterceptor(emProvider, getAnnotation());
    }
    if (TransactionType.GLOBAL == transactionType) {
      checkNotNull(utProvider, "the JNDI name of the user transaction must be specified if a "
          + "persistence wants to use global transactions");
      return new GlobalTxnInterceptor(emProvider, utProvider);
    }

    throw new IllegalStateException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  PersistenceService getPersistenceService() {
    return emfProvider;
  }

}
