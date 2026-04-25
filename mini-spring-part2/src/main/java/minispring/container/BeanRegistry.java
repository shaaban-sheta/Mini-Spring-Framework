package minispring.container;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BeanRegistry {

    private final Map<String, BeanDefinition> beanDefinitions = new ConcurrentHashMap<>();
    private final Map<String, Object>         singletonCache  = new ConcurrentHashMap<>();

    public void registerBeanDefinition(String name, BeanDefinition definition) {
        BeanDefinition existing = beanDefinitions.putIfAbsent(name, definition);
        if (existing != null) {
            throw new IllegalStateException(
                    "Duplicate bean name '" + name + "': "
                    + existing.getBeanClass().getName() + " and "
                    + definition.getBeanClass().getName());
        }
    }

    public void registerSingleton(String name, Object instance) {
        singletonCache.put(name, instance);
    }

    public Object          getSingleton(String name)      { return singletonCache.get(name);  }
    public BeanDefinition  getBeanDefinition(String name) { return beanDefinitions.get(name); }
    public boolean         containsBean(String name)      { return beanDefinitions.containsKey(name); }
    public Iterable<String> getBeanNames()                { return beanDefinitions.keySet();  }
    public int             getBeanCount()                 { return beanDefinitions.size();    }

    public List<BeanDefinition> findByType(Class<?> type) {
        return beanDefinitions.values().stream()
                .filter(bd -> type.isAssignableFrom(bd.getBeanClass()))
                .collect(Collectors.toList());
    }
}
