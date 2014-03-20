package com.github.sclassen.guicejpa;

public interface PersistenceExceptionTranslator<E extends RuntimeException> {

  E translateExceptionIfPossible(RuntimeException e);
}
