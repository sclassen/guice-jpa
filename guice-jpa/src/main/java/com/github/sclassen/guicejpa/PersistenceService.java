package com.github.sclassen.guicejpa;

/**
 * Persistence provider service.
 *
 * @author Stephan Classen
 */
public interface PersistenceService {

  /**
   * Starts the underlying persistence engine and makes jpa-persist ready for use.
   * This method must be called by your code prior to using any other jpa-persist artifacts.
   * <ul>
   *  <li>If already started, calling this method does nothing.</li>
   * </ul>
   */
  void start();

  /**
   * @return {@code true} if the underlying persistence engine is running.
   *         {@code false} otherwise.
   **/
  boolean isRunning();

  /**
   * Stops the underlying persistence engine.
   * <ul>
   *  <li>If already stopped, calling this method does nothing.</li>
   *  <li>If not yet started, it also does nothing.</li>
   * </ul>
   */
  void stop();

}
