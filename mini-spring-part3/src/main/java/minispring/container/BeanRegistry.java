package minispring.container;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for bean definitions and singleton instances.
 *
 * Tracks destruction callbacks in creation order so that destroyAll()
 * can invoke @PreDestroy in reverse (dependents before dependencies).
 */
public class BeanRegistry {

    private final Map<String, BeanDefinition> definitions    = new ConcurrentHashMap<>();
    private final Map<String, Object>         singletonCache = new ConcurrentHashMap<>();
    private final List<DestructionCallback>   destructionCallbacks = new ArrayList<>();

    // ------------------------------------------------------------------ registration

    public void register(BeanDefinition definition) {
        String name = definition.getBeanName();
        if (definitions.putIfAbsent(name, definition) != null) {
            throw new RuntimeException(
                    "Duplicate bean name '" + name + "'. Two @Component classes resolve "
                    + "to the same name. Use @Component(\"uniqueName\") to disambiguate.");
        }
    }

    // ------------------------------------------------------------------ singleton cache

    public void putSingleton(String name, Object instance) {
        singletonCache.put(name, instance);
    }

    public Object getSingleton(String name) {
        return singletonCache.get(name);
    }

    // ------------------------------------------------------------------ lookup

    public BeanDefinition getBeanDefinition(String name) {
        BeanDefinition def = definitions.get(name);
        if (def == null) {
            throw new RuntimeException("No bean named '" + name + "' found in the container.");
        }
        return def;
    }

    public Map<String, BeanDefinition> getAllDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    /**
     * Find all singletons whose type is assignable to the given type.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> findByType(Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Map.Entry<String, Object> entry : singletonCache.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getClass())) {
                found.add((T) entry.getValue());
            }
        }
        // Also check prototype definitions for type matching
        for (Map.Entry<String, BeanDefinition> entry : definitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue().getBeanClass())
                    && entry.getValue().getScope() == BeanDefinition.Scope.SINGLETON
                    && !singletonCache.containsKey(entry.getKey())) {
                // Singleton not yet instantiated — include it via name resolution later
            }
        }
        return found;
    }

    /**
     * Find bean definitions whose class is assignable to the given type.
     */
    public List<BeanDefinition> findDefinitionsByType(Class<?> type) {
        List<BeanDefinition> found = new ArrayList<>();
        for (BeanDefinition def : definitions.values()) {
            if (type.isAssignableFrom(def.getBeanClass())) {
                found.add(def);
            }
        }
        return found;
    }

    // ------------------------------------------------------------------ destruction

    public void registerDestructionCallback(String beanName, Object instance, Method preDestroyMethod) {
        destructionCallbacks.add(new DestructionCallback(beanName, instance, preDestroyMethod));
    }

    /**
     * Destroy all singletons in REVERSE creation order.
     * Exceptions during @PreDestroy are caught and logged — never re-thrown.
     */
    public void destroyAll() {
        List<DestructionCallback> reversed = new ArrayList<>(destructionCallbacks);
        Collections.reverse(reversed);

        for (DestructionCallback callback : reversed) {
            try {
                callback.method().setAccessible(true);
                callback.method().invoke(callback.instance());
                System.out.println("[MiniSpring] @PreDestroy called on: " + callback.beanName());
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                System.err.println("[MiniSpring] Error in @PreDestroy '"
                        + callback.beanName() + "': " + cause.getMessage());
            }
        }
        singletonCache.clear();
    }

    // ------------------------------------------------------------------ record

    private record DestructionCallback(String beanName, Object instance, Method method) {}
}
