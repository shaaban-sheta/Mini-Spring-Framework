package minispring.container;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The container's address book.
 *
 * Stores two distinct things:
 *   1. BeanDefinitions — metadata about every registered bean (always present)
 *   2. Singleton cache — live instances for singleton-scoped beans
 *
 * Thread Safety:
 *   Both maps use ConcurrentHashMap because beans are read from multiple
 *   threads during normal operation (web request handlers, background jobs).
 *   The slight memory overhead (~16 bytes per entry) is worth the thread safety.
 *
 * Collision Protection:
 *   registerBeanDefinition() uses putIfAbsent — it throws on duplicate names
 *   rather than silently overwriting. Silent overwrites are extremely hard to
 *   debug in multi-module or third-party library scenarios.
 */
public class BeanRegistry {

    /** Definition metadata for every registered bean. */
    private final Map<String, BeanDefinition> beanDefinitions =
            new ConcurrentHashMap<>();

    /** Live instances for SINGLETON-scoped beans only. */
    private final Map<String, Object> singletonCache =
            new ConcurrentHashMap<>();

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * Register a bean definition.
     * Throws IllegalStateException on name collision.
     */
    public void registerBeanDefinition(String name, BeanDefinition definition) {
        BeanDefinition existing = beanDefinitions.putIfAbsent(name, definition);
        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate bean name '" + name + "': "
                    + existing.getBeanClass().getName()
                    + " and "
                    + definition.getBeanClass().getName()
                    + ". Use @Component(\"uniqueName\") to disambiguate.");
        }
    }

    /**
     * Store a singleton instance after creation.
     */
    public void registerSingleton(String name, Object instance) {
        singletonCache.put(name, instance);
    }

    // ─── Lookup ───────────────────────────────────────────────────────────────

    /**
     * Get the cached singleton instance, or null if not yet created.
     */
    public Object getSingleton(String name) {
        return singletonCache.get(name);
    }

    /**
     * Get the definition for a bean by name, or null if not registered.
     */
    public BeanDefinition getBeanDefinition(String name) {
        return beanDefinitions.get(name);
    }

    /**
     * Check whether a bean with the given name is registered.
     */
    public boolean containsBean(String name) {
        return beanDefinitions.containsKey(name);
    }

    /**
     * Find all bean definitions whose class is assignable to the given type.
     *
     * Uses Class.isAssignableFrom() — so it matches:
     *   - the exact class
     *   - subclasses
     *   - implementations of the given interface
     *
     * This is what powers type-based injection:
     *   getBean(UserRepository.class) → finds JpaUserRepository if it implements UserRepository
     */
    public List<BeanDefinition> findByType(Class<?> type) {
        return beanDefinitions.values().stream()
                .filter(bd -> type.isAssignableFrom(bd.getBeanClass()))
                .collect(Collectors.toList());
    }

    /**
     * All registered bean names (definition keys).
     */
    public Iterable<String> getBeanNames() {
        return beanDefinitions.keySet();
    }

    /**
     * Total number of registered bean definitions.
     */
    public int getBeanCount() {
        return beanDefinitions.size();
    }

    /**
     * Clear all singleton instances (used during container shutdown).
     */
    public void clearSingletons() {
        singletonCache.clear();
    }
}
