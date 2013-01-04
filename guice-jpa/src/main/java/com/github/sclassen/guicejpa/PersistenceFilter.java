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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Filter for use in container.
 * The filter will start all persistence services upon container start and span a unit of work
 * around every request which is filtered.
 * <p/>
 * Usage example:
 * <pre>
 *  public class MyModule extends ServletModule {
 *    public void configure() {
 *      // bind your persistence units here
 *
 *      filter("/*").through(PersistenceFilter.class);
 *    }
 *  }
 * </pre>
 *
 * @author Stephan Classen
 */
public class PersistenceFilter implements Filter {

  // ---- Members

  private final PersistenceUnitContainer persistenceUnitsContainer;


  // ---- Constructor

  PersistenceFilter(PersistenceUnitContainer persistenceUnitsContainer) {
    checkNotNull(persistenceUnitsContainer);
    this.persistenceUnitsContainer = persistenceUnitsContainer;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
    throws IOException, ServletException {
    try {
      persistenceUnitsContainer.begin();
      chain.doFilter(request, response);
    }
    finally {
      persistenceUnitsContainer.end();
    }
  }

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
  public void destroy() {
    persistenceUnitsContainer.stop();
  }
}
