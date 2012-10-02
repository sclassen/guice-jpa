package com.github.sclassen.guicejpa;

import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Implementation of {@link EntityManagerProvider} and {@link UnitOfWork}.
 *
 * @author Stephan Classen
 */
final class EntityManagerProviderImpl implements EntityManagerProvider, UnitOfWork {

  // ---- Members

  /** Provider for {@link EntityManagerFactory}. */
  private final EntityManagerFactoryProvider emfProvider;

  /** Additional properties to be set on every {@link EntityManager} which is created. */
  private final Properties properties;

  /** Thread local store of {@link EntityManager}s. */
  private final ThreadLocal<EntityManager> entityManagers = new ThreadLocal<EntityManager>();


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emfProvider the provider for {@link EntityManagerFactory}.
   */
  public EntityManagerProviderImpl(EntityManagerFactoryProvider emfProvider) {
    this(emfProvider, null);
  }

  /**
   * Constructor.
   *
   * @param emfProvider the provider for {@link EntityManagerFactory}.
   * @param properties additional properties to be set on every {@link EntityManager} which is created.
   */
  public EntityManagerProviderImpl(EntityManagerFactoryProvider emfProvider, Properties properties) {
    this.emfProvider = emfProvider;
    this.properties = properties;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public EntityManager get() {
    if (isActive()) {
      return entityManagers.get();
    }

    throw new IllegalStateException("UnitOfWork is not running.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() {
    if (isActive()) {
      throw new IllegalStateException("Unit of work has already been started.");
    }

    final EntityManagerFactory emf = emfProvider.get();
    final EntityManager em;
    if (null == properties) {
      em = emf.createEntityManager();
    } else {
      em = emf.createEntityManager(properties);
    }

    entityManagers.set(em);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isActive() {
    return null != entityManagers.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() {
    if (isActive()) {
      final EntityManager em = entityManagers.get();
      em.close();
      entityManagers.remove();
    }
  }

}
