package minispring.container;

import minispring.annotation.Autowired;
import minispring.annotation.Qualifier;
import minispring.scanner.ClasspathScanner;

import java.lang.reflect.*;
import java.util.*;

/**
 * IoC container with full dependency injection support.
 *
 * New in Part 2:
 *   - Constructor injection (@Autowired on constructor)
 *   - Field injection (@Autowired on field)
 *   - Type-based resolution with isAssignableFrom()
 *   - @Qualifier disambiguation
 *   - Circular dependency detection
 *   - Implicit single-constructor injection (Spring 4.3+)
 */
public class MiniApplicationContext {

    private final BeanRegistry    registry        = new BeanRegistry();
    private final ClasspathScanner scanner         = new ClasspathScanner();
    private final Set<String>     beansInCreation  = new HashSet<>();

    public MiniApplicationContext(String... basePackages) {
        List<Class<?>> componentClasses = scanner.scan(basePackages);

        for (Class<?> clazz : componentClasses) {
            String beanName         = BeanNameGenerator.generateBeanName(clazz);
            BeanDefinition definition = new BeanDefinition(
                    clazz, beanName, BeanDefinition.Scope.SINGLETON);
            registry.registerBeanDefinition(beanName, definition);
        }

        System.out.println("[MiniSpring] Registered "
                + registry.getBeanCount() + " bean definition(s)");

        for (String beanName : registry.getBeanNames()) {
            BeanDefinition def = registry.getBeanDefinition(beanName);
            if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
                Object instance = createBean(def);
                registry.registerSingleton(beanName, instance);
            }
        }

        System.out.println("[MiniSpring] Container initialized successfully");
    }

    // ─── Core: create a fully injected bean ──────────────────────────────────

    private Object createBean(BeanDefinition definition) {
        String   beanName = definition.getBeanName();
        Class<?> clazz    = definition.getBeanClass();

        // Circular dependency guard
        if (beansInCreation.contains(beanName)) {
            throw new RuntimeException(
                    "Circular dependency detected while creating bean '"
                    + beanName + "'. Chain: " + beansInCreation);
        }
        beansInCreation.add(beanName);

        try {
            Constructor<?> ctor = resolveConstructor(clazz);
            ctor.setAccessible(true);

            Object[] args     = resolveConstructorArgs(ctor);
            Object   instance = ctor.newInstance(args);

            injectFields(instance, clazz);

            return instance;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create bean '" + beanName + "'", e);
        } finally {
            beansInCreation.remove(beanName);
        }
    }

    // ─── Constructor selection ────────────────────────────────────────────────

    private Constructor<?> resolveConstructor(Class<?> clazz) {
        Constructor<?>[] all = clazz.getDeclaredConstructors();

        List<Constructor<?>> autowired = new ArrayList<>();
        for (Constructor<?> c : all) {
            if (c.isAnnotationPresent(Autowired.class)) autowired.add(c);
        }

        if (autowired.size() > 1)
            throw new RuntimeException(
                    "Multiple @Autowired constructors in " + clazz.getName());

        if (autowired.size() == 1) return autowired.get(0);

        try { return clazz.getDeclaredConstructor(); } // no-arg constructor
        catch (NoSuchMethodException ignored) {}

        if (all.length == 1) return all[0]; // implicit single-constructor injection

        throw new RuntimeException(
                "Ambiguous constructors in " + clazz.getName()
                + ". Add @Autowired to the constructor you want used.");
    }

    // ─── Constructor argument resolution ─────────────────────────────────────

    private Object[] resolveConstructorArgs(Constructor<?> ctor) {
        Parameter[] params = ctor.getParameters();
        Object[]    args   = new Object[params.length];

        for (int i = 0; i < params.length; i++) {
            Qualifier q     = params[i].getAnnotation(Qualifier.class);
            String qualifier = q != null ? q.value() : null;

            args[i] = resolveBean(
                    params[i].getType(), qualifier,
                    "constructor param '" + params[i].getName()
                    + "' of " + ctor.getDeclaringClass().getName());
        }
        return args;
    }

    // ─── Field injection ──────────────────────────────────────────────────────

    private void injectFields(Object instance, Class<?> clazz) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) continue;

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new RuntimeException(
                            "Cannot inject final field '" + field.getName()
                            + "' in " + clazz.getName()
                            + ". Use constructor injection.");
                }

                Qualifier q      = field.getAnnotation(Qualifier.class);
                String qualifier = q != null ? q.value() : null;

                Object dep = resolveBean(
                        field.getType(), qualifier,
                        "field '" + field.getName() + "' of " + clazz.getName());

                field.setAccessible(true);
                try {
                    field.set(instance, dep);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(
                            "Cannot set field '" + field.getName() + "'", e);
                }
            }
            current = current.getSuperclass();
        }
    }

    // ─── Type-based resolution ────────────────────────────────────────────────

    private Object resolveBean(Class<?> type, String qualifierName, String context) {

        if (qualifierName != null) {
            BeanDefinition def = registry.getBeanDefinition(qualifierName);
            if (def == null)
                throw new RuntimeException(
                        "No bean '" + qualifierName + "' for " + context);

            if (!type.isAssignableFrom(def.getBeanClass()))
                throw new RuntimeException(
                        "Bean '" + qualifierName + "' is not assignable to "
                        + type.getName() + " required by " + context);

            return getOrCreateBean(def);
        }

        List<BeanDefinition> candidates = registry.findByType(type);

        if (candidates.isEmpty())
            throw new RuntimeException(
                    "No bean of type " + type.getName()
                    + " for " + context + ". Did you add @Component?");

        if (candidates.size() > 1) {
            String names = candidates.stream()
                    .map(BeanDefinition::getBeanName)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new RuntimeException(
                    "Multiple beans of type " + type.getName()
                    + ": [" + names + "]. Add @Qualifier to " + context);
        }

        return getOrCreateBean(candidates.get(0));
    }

    private Object getOrCreateBean(BeanDefinition def) {
        if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
            Object existing = registry.getSingleton(def.getBeanName());
            if (existing != null) return existing;
        }
        Object instance = createBean(def);
        if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
            registry.registerSingleton(def.getBeanName(), instance);
        }
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public Object getBean(String name) {
        BeanDefinition def = registry.getBeanDefinition(name);
        if (def == null)
            throw new RuntimeException("No bean found: '" + name + "'");

        return def.getScope() == BeanDefinition.Scope.SINGLETON
                ? registry.getSingleton(name)
                : createBean(def);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        List<BeanDefinition> candidates = registry.findByType(type);
        if (candidates.isEmpty())
            throw new RuntimeException("No bean of type: " + type.getName());
        if (candidates.size() > 1) {
            String names = candidates.stream()
                    .map(BeanDefinition::getBeanName)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new RuntimeException(
                    "Multiple beans of type " + type.getName() + ": [" + names + "]");
        }
        return (T) getBean(candidates.get(0).getBeanName());
    }

    public boolean containsBean(String name) { return registry.containsBean(name); }
    public int getBeanCount()                { return registry.getBeanCount();     }
}
