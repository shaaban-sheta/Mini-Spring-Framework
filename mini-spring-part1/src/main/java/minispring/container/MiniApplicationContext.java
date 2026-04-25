package minispring.container;

import minispring.scanner.ClasspathScanner;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * The main entry point for the MiniSpring IoC container.
 *
 * Responsibilities:
 *   1. Accept base packages to scan
 *   2. Discover all @Component-annotated classes (via ClasspathScanner)
 *   3. Register their metadata (BeanDefinition) in the BeanRegistry
 *   4. Instantiate all singleton beans eagerly (fail-fast at startup)
 *   5. Serve beans on request via getBean()
 *
 * Analogous to Spring's ApplicationContext.
 *
 * Usage:
 *   MiniApplicationContext ctx = new MiniApplicationContext("app.service", "app.repo");
 *   UserService svc = ctx.getBean(UserService.class);
 */
public class MiniApplicationContext {

    private final BeanRegistry registry = new BeanRegistry();
    private final ClasspathScanner scanner = new ClasspathScanner();

    /**
     * Bootstrap the entire container.
     *
     * Three phases (separated intentionally):
     *   Phase 1 — Scan:        filesystem I/O to find @Component classes
     *   Phase 2 — Register:    create BeanDefinitions, detect name collisions
     *   Phase 3 — Instantiate: create singleton instances using reflection
     *
     * @param basePackages packages to scan, e.g. "app.service", "app.repository"
     */
    public MiniApplicationContext(String... basePackages) {

        // ── Phase 1: Classpath Scan ────────────────────────────────────────────
        List<Class<?>> componentClasses = scanner.scan(basePackages);

        // ── Phase 2: Register Definitions ─────────────────────────────────────
        for (Class<?> clazz : componentClasses) {
            String beanName = BeanNameGenerator.generateBeanName(clazz);
            BeanDefinition definition = new BeanDefinition(
                    clazz, beanName, BeanDefinition.Scope.SINGLETON);
            registry.registerBeanDefinition(beanName, definition);
        }

        System.out.println("[MiniSpring] Registered "
                + registry.getBeanCount() + " bean definition(s)");

        // ── Phase 3: Eager Singleton Instantiation ─────────────────────────────
        // Why eager? Fail fast — a misconfigured bean crashes at startup, not
        // at runtime when the first user request hits it.
        for (String beanName : registry.getBeanNames()) {
            BeanDefinition definition = registry.getBeanDefinition(beanName);
            if (definition.getScope() == BeanDefinition.Scope.SINGLETON) {
                Object instance = createBean(definition);
                registry.registerSingleton(beanName, instance);
            }
        }

        System.out.println("[MiniSpring] Container initialized successfully");
    }

    // ─── Bean Creation ────────────────────────────────────────────────────────

    /**
     * Create a bean instance from its definition.
     *
     * In Part 1, this uses only no-arg constructors.
     * Part 2 will add constructor injection and field injection.
     *
     * setAccessible(true) is called deliberately:
     *   - Beans may legitimately have private or package-private constructors
     *   - The container is trusted framework infrastructure
     *   - Risk is mitigated by scanning only explicitly declared packages
     */
    private Object createBean(BeanDefinition definition) {
        Class<?> clazz = definition.getBeanClass();
        try {
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();

        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Bean '" + definition.getBeanName()
                    + "' (" + clazz.getName() + ") has no no-arg constructor. "
                    + "Add a no-arg constructor, or wait for Part 2 which adds "
                    + "constructor injection.", e);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to instantiate bean '"
                    + definition.getBeanName() + "'", e);
        }
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Retrieve a bean by its name.
     *
     * @throws RuntimeException if no bean with that name exists
     */
    public Object getBean(String name) {
        BeanDefinition definition = registry.getBeanDefinition(name);
        if (definition == null) {
            throw new RuntimeException("No bean found with name: '" + name + "'");
        }

        if (definition.getScope() == BeanDefinition.Scope.SINGLETON) {
            Object instance = registry.getSingleton(name);
            if (instance == null) {
                throw new IllegalStateException(
                        "Singleton bean '" + name
                        + "' has a definition but was not initialized");
            }
            return instance;
        }

        // PROTOTYPE: create a fresh instance on every call
        return createBean(definition);
    }

    /**
     * Type-safe bean retrieval.
     *
     * Throws if no matching bean is found, or if multiple beans match
     * (use getBean(name) or @Qualifier in Part 2 to disambiguate).
     *
     * @param type the class or interface to look up
     * @return the unique bean assignable to that type
     */
    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        List<BeanDefinition> candidates = registry.findByType(type);

        if (candidates.isEmpty()) {
            throw new RuntimeException(
                    "No bean found of type: " + type.getName()
                    + ". Did you forget @Component?");
        }

        if (candidates.size() > 1) {
            String names = candidates.stream()
                    .map(BeanDefinition::getBeanName)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            throw new RuntimeException(
                    "Multiple beans of type " + type.getName()
                    + " found: [" + names + "]. "
                    + "Use getBean(\"name\") to select one. "
                    + "@Qualifier support is coming in Part 2.");
        }

        return (T) getBean(candidates.get(0).getBeanName());
    }

    /**
     * Check whether a bean with the given name has been registered.
     */
    public boolean containsBean(String name) {
        return registry.containsBean(name);
    }

    /**
     * Number of registered bean definitions.
     */
    public int getBeanCount() {
        return registry.getBeanCount();
    }
}
