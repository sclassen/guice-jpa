package ch.sclassen.guice.guicejpa;

import java.util.HashSet;
import java.util.Set;

/**
 * Container of persistence units.
 * This is a convenience wrapper for multiple persistence units. calling any method of either
 * {@link PersistenceService} or {@link UnitOfWork} will propagate this call to all added
 * peristence units.
 *
 * @author Stephan Classen
 */
class PersistenceUnitContainer implements PersistenceService, UnitOfWork {

  private final Set<PersistenceService> persistenceServices = new HashSet<PersistenceService>();
  private final Set<UnitOfWork> unitsOfWork = new HashSet<UnitOfWork>();

  /**
   * Adds a persistence service to this container.
   *
   * @param ps the persistence service to add.
   */
  void add(PersistenceService ps, UnitOfWork uow) {
    persistenceServices.add(ps);
    unitsOfWork.add(uow);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void start() {
    for (PersistenceService ps : persistenceServices) {
      ps.start();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized boolean isRunning() {
    for (PersistenceService ps : persistenceServices) {
      if (!ps.isRunning()) {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void stop() {
    for (PersistenceService ps : persistenceServices) {
      ps.stop();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void begin() {
    for (UnitOfWork unitOfWork : unitsOfWork) {
      unitOfWork.begin();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isActive() {
    for (UnitOfWork unitOfWork : unitsOfWork) {
      if (!unitOfWork.isActive()) {
        return false;
      }
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void end() {
    for (UnitOfWork unitOfWork : unitsOfWork) {
      unitOfWork.end();
    }

  }

}
