package ch.sclassen.guice.guicejpa;

import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

/**
 * Implementation of {@link PersistenceService} and {@link EntityManagerFactoryProvider} for
 * application managed entity manager factories.
 *
 * @author Stephan Classen
 */
final class ApplicationManagedEntityManagerFactoryProvider implements EntityManagerFactoryProvider {

  // ---- Members

  /** Name of the persistence unit as defined in the persistence.xml.  */
  private final String puName;

  /** Additional properties. Theses override the ones defined in the persistence.xml. */
  private final Properties properties;

  /** EntityManagerFactory. */
  private EntityManagerFactory emf;


  // ---- Constructor

  /**
   * Constructor.
   *
   * @param puName the name of the persistence unit as defined in the persistence.xml.
   * @param properties the additional properties. Theses override the ones defined in the persistence.xml.
   */
  public ApplicationManagedEntityManagerFactoryProvider(String puName, Properties properties) {
    this.puName = puName;
    this.properties = properties;
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
      emf = Persistence.createEntityManagerFactory(puName, properties);
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
    if (isRunning()) {
      emf.close();
      emf = null;
    }
  }

}
