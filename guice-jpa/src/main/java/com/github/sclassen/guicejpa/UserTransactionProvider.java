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

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.UserTransaction;

/**
 * Provider for {@link UserTransactionFacade}.
 *
 * @author Stephan Classen
 */
class UserTransactionProvider {

  // ---- Memebers

  /** The JNDI name of the user transaction. */
  private final String utJndiName;


  // ---- Constructors

  /**
   * Constructor.
   *
   * @param utJndiName the JNDI name of the user transaction of the container.
   */
  UserTransactionProvider(String utJndiName) {
    this.utJndiName = utJndiName;
  }


  // ---- Methods

  /**
   * @return the user transaction facade.
   */
  UserTransactionFacade get() {
    try {
      final InitialContext ctx = new InitialContext();
      UserTransaction txn = (UserTransaction) ctx.lookup(utJndiName);
      return new UserTransactionFacade(txn);
    } catch (NamingException e) {
      throw new RuntimeException("lookup for UserTransaction with JNDI name '" + utJndiName
          + "' failed", e);
    }
  }

}
