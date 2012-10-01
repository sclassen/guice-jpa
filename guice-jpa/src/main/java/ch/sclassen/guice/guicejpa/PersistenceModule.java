package ch.sclassen.guice.guicejpa;

import static com.google.inject.matcher.Matchers.any;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.transaction.UserTransaction;

import org.aopalliance.intercept.MethodInterceptor;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matcher;
import com.google.inject.matcher.Matchers;

/**
 * Main module of the jpa-persistence guice extension.
 * <p/>
 * Add either a {@link ApplicationManagedPersistenceUnitModule} or a
 * {@link ContainerManagedPersistenceUnitModule} per persistence unit using the methods
 * <ul>
 *    <li>{@link #add(ApplicationManagedPersistenceUnitModule)}</li>
 *    <li>{@link #addApplicationManagedPersistenceUnit(String)}</li>
 *    <li>{@link #addApplicationManagedPersistenceUnit(String, Properties)}</li>
 *    <li>{@link #add(ContainerManagedPersistenceUnitModule)}</li>
 *    <li>{@link #addContainerManagedPersistenceUnit(String)}</li>
 *    <li>{@link #addContainerManagedPersistenceUnit(String, Properties)}</li>
 * </ul>
 * <p/>
 * If container managed persistence units have been added and container managed transactions (CMT)
 * are supported. Use {@link #setUserTransactionJndiName(String)} to define the JNDI name of the
 * {@link UserTransaction} provided by the container.
 *
 * @author Stephan Classen
 */
public final class PersistenceModule extends AbstractModule {

  // ---- Members

  /** List of all module builders. */
  private final List<AbstractPersistenceModuleBuilder> moduleBuilders = new ArrayList<AbstractPersistenceModuleBuilder>();

  /**
   * List of all persistence unit modules.
   * If this list is empty it means that configure has not yet been called
   */
  private final List<AbstractPersistenceUnitModule> modules = new ArrayList<AbstractPersistenceUnitModule>();

  private final PersistenceUnitContainer puContainer = new PersistenceUnitContainer();

  /**
   * The provider for the {@link UserTransactionFacade}.
   */
  private UserTransactionProvider utProvider = null;


  // ---- Methods

  /**
   * Add an application managed persistence unit.
   *
   * @param puName the name of the persistence unit as specified in the persistence.xml.
   * @return a builder to further configure the persistence unit.
   */
  public ApplicationManagedPersistenceUnitBuilder addApplicationManagedPersistenceUnit(String puName) {
    return add(new ApplicationManagedPersistenceUnitModule(puName));
  }

  /**
   * Add an application managed persistence unit.
   *
   * @param puName the name of the persistence unit as specified in the persistence.xml.
   * @param properties the properties to pass to the {@link EntityManagerFactory}.
   * @return a builder to further configure the persistence unit.
   */
  public ApplicationManagedPersistenceUnitBuilder addApplicationManagedPersistenceUnit(String puName,
      Properties properties) {
    return add(new ApplicationManagedPersistenceUnitModule(puName, properties));
  }

  /**
   * Add an application managed persistence unit.
   *
   * @param module the module of the persistence unit.
   * @return a builder to further configure the persistence unit.
   */
  public ApplicationManagedPersistenceUnitBuilder add(ApplicationManagedPersistenceUnitModule module) {
    final ApplicationManagedPersistenceUnitBuilder builder = new ApplicationManagedPersistenceUnitBuilder(module);
    moduleBuilders.add(builder);
    return builder;
  }

  /**
   * Add an container managed persistence unit.
   *
   * @param puName the name of the persistence unit as specified in the persistence.xml.
   * @return a builder to further configure the persistence unit.
   */
  public ContainerManagedPersistenceUnitBuilder addContainerManagedPersistenceUnit(String emfJndiName) {
    return add(new ContainerManagedPersistenceUnitModule(emfJndiName));
  }

  /**
   * Add an container managed persistence unit.
   *
   * @param puName the name of the persistence unit as specified in the persistence.xml.
   * @param properties the properties to pass to the {@link EntityManager}.
   * @return a builder to further configure the persistence unit.
   */
  public ContainerManagedPersistenceUnitBuilder addContainerManagedPersistenceUnit(String emfJndiName,
      Properties properties) {
    return add(new ContainerManagedPersistenceUnitModule(emfJndiName, properties));
  }

  /**
   * Add an container managed persistence unit.
   *
   * @param module the module of the persistence unit.
   * @return a builder to further configure the persistence unit.
   */
  public ContainerManagedPersistenceUnitBuilder add(ContainerManagedPersistenceUnitModule module) {
    final ContainerManagedPersistenceUnitBuilder builder = new ContainerManagedPersistenceUnitBuilder(module);
    moduleBuilders.add(builder);
    return builder;
  }

  /**
   * Setter for defining the JNDI name of the container managed {@link UserTransaction}.
   *
   * @param utJndiName the JNDI name of the container managed {@link UserTransaction}.
   */
  public void setUserTransactionJndiName(String utJndiName) {
    if (configureHasNotBeenExecutedYet()) {
      utProvider = new UserTransactionProvider(utJndiName);
    }
    throw new IllegalStateException("cannot change a module after creating the injector.");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void configure() {
    if (configureHasNotBeenExecutedYet()) {
      for (AbstractPersistenceModuleBuilder builder : moduleBuilders) {
        final AbstractPersistenceUnitModule module = builder.build();
        puContainer.add(module.getPersistenceService(), module.getUnitOfWork());
        modules.add(module);
      }
    }

    for (AbstractPersistenceUnitModule module : modules) {
      install(module);

      final Matcher<AnnotatedElement> matcher = Matchers.annotatedWith(module.getTxnAnnotation());
      final MethodInterceptor transactionInterceptor = module.getTransactionInterceptor(utProvider);

      bindInterceptor(matcher, any(), transactionInterceptor);
      bindInterceptor(any(), matcher, transactionInterceptor);
    }

    bind(PersistenceService.class).annotatedWith(AllPersistenceUnits.class).toInstance(puContainer);
    bind(UnitOfWork.class).annotatedWith(AllPersistenceUnits.class).toInstance(puContainer);
  }

  /**
   * @return {@code true} if {@link #configure()} has not yet been invoked.
   */
  private boolean configureHasNotBeenExecutedYet() {
    return modules.size() == 0;
  }

}