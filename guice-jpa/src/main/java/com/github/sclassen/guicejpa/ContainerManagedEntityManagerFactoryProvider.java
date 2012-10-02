package com.github.sclassen.guicejpa;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

/**
 * Implementation of {@link PersistenceService} and {@link EntityManagerFactoryProvider} for
 * container managed entity manager factories.
 *
 * @author Stephan Classen
 */
final class ContainerManagedEntityManagerFactoryProvider implements EntityManagerFactoryProvider {

  // ---- Members

  /** The JNDI name of the {@link EntityManagerFactory}. */
  private final String emfJndiName;

  /** The {@link EntityManagerFactory}. */
  private EntityManagerFactory emf;


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param emfJndiName the JNDI name of the {@link EntityManagerFactory}.
   */
  public ContainerManagedEntityManagerFactoryProvider(String emfJndiName) {
    this.emfJndiName = emfJndiName;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public EntityManagerFactory get() {
    if (isRunning()) {
      return emf;
    }

    throw new IllegalStateException("PersistenceService is not running.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void start() {
    if (!isRunning()) {
      try {
        final InitialContext ctx = new InitialContext();
        emf = (EntityManagerFactory) ctx.lookup(emfJndiName);
      } catch (NamingException e) {
        throw new RuntimeException("lookup for EntityManagerFactory with JNDI name '" + emfJndiName
            + "' failed", e);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isRunning() {
    return null != emf;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stop() {
    emf = null;
  }

}
