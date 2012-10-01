package ch.sclassen.guice.guicejpa;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

class PersistenceFilter implements Filter {

  // ---- Members

  private final PersistenceUnitContainer persistenceUnitsContainer;


  // ---- Constructor

  PersistenceFilter(PersistenceUnitContainer persistenceUnitsContainer) {
    this.persistenceUnitsContainer = persistenceUnitsContainer;
  }

  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    persistenceUnitsContainer.start();

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    persistenceUnitsContainer.begin();
    try {
      chain.doFilter(request, response);
    } finally {
      persistenceUnitsContainer.end();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() {
    persistenceUnitsContainer.stop();
  }
}
