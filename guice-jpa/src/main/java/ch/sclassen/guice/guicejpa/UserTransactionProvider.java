package ch.sclassen.guice.guicejpa;

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
