package minispring.container;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BeanRegistry {

    private final Map<String, BeanDefinition> definitions       = new ConcurrentHashMap<>();
    private final Map<String, Object>         singletonCache    = new ConcurrentHashMap<>();
    private final List<DestructionCallback>   destructionCallbacks = new ArrayList<>();

    public void register(BeanDefinition definition) {
        String name = definition.getBeanName();
        if (definitions.putIfAbsent(name, definition) != null) {
            throw new RuntimeException("Duplicate bean name '" + name + "'.");
        }
    }

    public void putSingleton(String name, Object instance) {
        singletonCache.put(name, instance);
    }

    public Object getSingleton(String name) {
        return singletonCache.get(name);
    }

    public BeanDefinition getBeanDefinition(String name) {
        BeanDefinition def = definitions.get(name);
        if (def == null) throw new RuntimeException("No bean named '" + name + "'.");
        return def;
    }

    public Map<String, BeanDefinition> getAllDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

    public List<BeanDefinition> findDefinitionsByType(Class<?> type) {
        List<BeanDefinition> found = new ArrayList<>();
        for (BeanDefinition def : definitions.values()) {
            if (type.isAssignableFrom(def.getBeanClass())) {
                found.add(def);
            }
        }
        return found;
    }

    public void registerDestructionCallback(String name, Object instance, Method m) {
        destructionCallbacks.add(new DestructionCallback(name, instance, m));
    }

    public void destroyAll() {
        List<DestructionCallback> reversed = new ArrayList<>(destructionCallbacks);
        Collections.reverse(reversed);
        for (DestructionCallback cb : reversed) {
            try {
                cb.method().setAccessible(true);
                cb.method().invoke(cb.instance());
                System.out.println("[BeanRegistry] @PreDestroy: " + cb.beanName());
            } catch (Exception e) {
                System.err.println("[BeanRegistry] @PreDestroy error on '"
                        + cb.beanName() + "': " + e.getMessage());
            }
        }
        singletonCache.clear();
    }

    private record DestructionCallback(String beanName, Object instance, Method method) {}
}
