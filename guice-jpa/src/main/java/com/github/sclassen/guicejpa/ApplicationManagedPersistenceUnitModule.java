package com.github.sclassen.guicejpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;

import org.aopalliance.intercept.MethodInterceptor;

/**
 * Persistence module for an application managed persistence unit.
 * <p/>
 * Use the {@link ApplicationManagedPersistenceUnitBuilder} to configure an instance of this class.
 * <p/>
 * This is a private module which will expose the following bindings:
 * <ul>
 *    <li>{@link UnitOfWork}</li>
 *    <li>{@link EntityManagerProvider}</li>
 * </ul>
 * If an annotation has been defined for this module the above classes are exposed with this
 * annotation. Within the private module the above classes are also binded without any annotation.
 * <p/>
 * You can extend this class and override {@link #configurePersistence()} to bind and expose
 * additional classes within this private module. This is useful if you require injection of the
 * above classes without annotation.
 *
 * @author Stephan Classen
 */
public class ApplicationManagedPersistenceUnitModule extends AbstractPersistenceUnitModule {

  // ---- Members

  /** Provider for {@link EntityManagerFactory} */
  private final ApplicationManagedEntityManagerFactoryProvider emfProvider;


  // ---- Constructors

  /**
   * Constructor.
   *
   * @param puName the name of the persistence unit as defined in the persistence.xml.
   */
  public ApplicationManagedPersistenceUnitModule(String puName) {
    this(puName, new Properties());
  }

  /**
   * Constructor.
   *
   * @param puName the name of the persistence unit as defined in the persistence.xml.
   * @param properties the additional properties. Theses override the ones defined in the persistence.xml.
   */
  public ApplicationManagedPersistenceUnitModule(String puName, Properties properties) {
    this(new ApplicationManagedEntityManagerFactoryProvider(puName, properties));
    checkNotNull(puName);
    checkNotNull(properties);
  }

  /**
   * Constructor.
   *
   * @param emfProvider the provider for {@link EntityManagerFactory}.
   */
  private ApplicationManagedPersistenceUnitModule(
      ApplicationManagedEntityManagerFactoryProvider emfProvider) {
    super(new EntityManagerProviderImpl(emfProvider));
    this.emfProvider = emfProvider;
  }


  // ---- Methods

  /**
   * {@inheritDoc}
   */
  @Override
  final Class<? extends Annotation> getTxnAnnotation() {
    return LocalTransactional.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  final MethodInterceptor getTxnInterceptor(EntityManagerProviderImpl emProvider,
      UserTransactionProvider utProvider) {
    return new LocalTxnInterceptor(emProvider, getAnnotation());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  final PersistenceService getPersistenceService() {
    return emfProvider;
  }

}
