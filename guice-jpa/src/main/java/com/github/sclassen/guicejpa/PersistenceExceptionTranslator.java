package com.github.sclassen.guicejpa;

/**
 * Implementations of this interface should provide exception translation functionality.
 * 
 * @author Piotr Ostrowski
 *
 * @param <E> subclass of RuntimeException for corresponding exception
 */
public interface PersistenceExceptionTranslator<E extends RuntimeException> {

  /**
   * Translate the given runtime exception thrown by a persistence framework to a corresponding exception, if possible.
   * Do not translate exceptions that are not understand by this translator.
   * 
   * @param e a RuntimeException thrown
   * @return the corresponding exception (or null if the exception could not be translated)
   */
  E translateExceptionIfPossible(RuntimeException e);
}
