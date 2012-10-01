package ch.sclassen.guice.guicejpa;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;

/**
 * Builder class for configurating an container managed persistence unit.
 *
 * @author Stephan Classen
 */
public final class ContainerManagedPersistenceUnitBuilder extends AbstractPersistenceModuleBuilder {

  // ---- Members

  /** The module which is built by this instance. */
  private ContainerManagedPersistenceUnitModule module;

  // ---- Constructors

  /**
   * Constructor.
   *
   * @param module the module which is built by this instance.
   */
  ContainerManagedPersistenceUnitBuilder(ContainerManagedPersistenceUnitModule module) {
    this.module = module;
  }

  // ---- Methods

  /**
   * Add an annotation to the module. The annotation is used to bind the {@link UnitOfWork} and
   * the {@link EntityManagerProvider} in guice.
   *
   * @param annotation the annotation. May be {@code null}.
   */
  public ContainerManagedPersistenceUnitBuilder annotatedWith(Class<? extends Annotation> annotation) {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.annotatedWith(annotation);
    return this;
  }

  /**
   * Configure the persistence unit to use local transactions. This means even if the data source
   * is managed by the container its transaction won't participate in a global container managed
   * transaction (CMT).
   */
  public void useLocalTransaction() {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.setTransactionAnnotation(LocalTransactional.class);
  }

  /**
   * Configure the persistence unit to use global transactions. This means all transactions on this
   * data source will participate in a global container managed transaction (CMT)
   */
  public void useGlobalTransaction() {
    checkNotNull(module, "cannot change a module after creating the injector.");
    module.setTransactionAnnotation(GlobalTransactional.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  ContainerManagedPersistenceUnitModule build() {
    checkNotNull(module, "build() can only be called once.");
    final ContainerManagedPersistenceUnitModule m = module;
    module = null;
    return m;
  }

}