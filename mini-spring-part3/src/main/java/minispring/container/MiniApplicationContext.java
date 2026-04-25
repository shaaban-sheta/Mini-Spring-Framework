package minispring.container;

import minispring.annotation.*;
import minispring.scanner.ClasspathScanner;

import java.lang.reflect.*;
import java.util.*;

/**
 * The heart of the Mini Spring Framework — Part 3.
 *
 * Features added in this part:
 *   - @PostConstruct lifecycle callback (after full injection)
 *   - @PreDestroy lifecycle callback (at container shutdown)
 *   - @Scope annotation support (singleton / prototype)
 *   - ObjectFactory<T> injection for safe prototype-in-singleton usage
 *   - JVM shutdown hook for automatic @PreDestroy on System.exit / Ctrl+C
 *
 * Bean creation order (unchanged):
 *   Constructor → Field injection → @PostConstruct
 */
public class MiniApplicationContext {

    private final BeanRegistry   registry    = new BeanRegistry();
    private final BeanNameGenerator nameGen  = new BeanNameGenerator();
    private final Set<String>    inCreation  = new HashSet<>();

    // ------------------------------------------------------------------ bootstrap

    public MiniApplicationContext(String basePackage) {
        List<Class<?>> components = new ClasspathScanner().scan(basePackage);
        registerAll(components);
        instantiateSingletons();
        registerShutdownHook();
        System.out.println("[MiniSpring] Container initialized successfully");
    }

    // ---- phase 1: register -------------------------------------------------

    private void registerAll(List<Class<?>> classes) {
        for (Class<?> clazz : classes) {
            String name  = nameGen.generateBeanName(clazz);
            BeanDefinition.Scope scope = determineScope(clazz);
            BeanDefinition def = new BeanDefinition(clazz, name, scope);
            discoverLifecycleMethods(clazz, def);
            registry.register(def);
        }
        System.out.println("[MiniSpring] Registered "
                + registry.getAllDefinitions().size() + " bean definition(s)");
    }

    private BeanDefinition.Scope determineScope(Class<?> clazz) {
        Scope annotation = clazz.getAnnotation(Scope.class);
        if (annotation != null) {
            return BeanDefinition.Scope.fromString(annotation.value());
        }
        return BeanDefinition.Scope.SINGLETON;
    }

    // ---- lifecycle method discovery ----------------------------------------

    private void discoverLifecycleMethods(Class<?> clazz, BeanDefinition def) {
        Method postConstruct = null;
        Method preDestroy    = null;

        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method method : current.getDeclaredMethods()) {

                if (method.isAnnotationPresent(PostConstruct.class)) {
                    validateLifecycleMethod(method, "@PostConstruct", clazz);
                    if (postConstruct != null) {
                        throw new RuntimeException("Multiple @PostConstruct methods in "
                                + clazz.getName() + ": "
                                + postConstruct.getName() + " and " + method.getName());
                    }
                    postConstruct = method;
                }

                if (method.isAnnotationPresent(PreDestroy.class)) {
                    validateLifecycleMethod(method, "@PreDestroy", clazz);
                    if (preDestroy != null) {
                        throw new RuntimeException("Multiple @PreDestroy methods in "
                                + clazz.getName() + ": "
                                + preDestroy.getName() + " and " + method.getName());
                    }
                    preDestroy = method;
                }
            }
            current = current.getSuperclass();
        }

        def.setPostConstructMethod(postConstruct);
        def.setPreDestroyMethod(preDestroy);
    }

    private void validateLifecycleMethod(Method m, String annotName, Class<?> owner) {
        if (m.getParameterCount() != 0) {
            throw new RuntimeException(annotName + " method '" + m.getName()
                    + "' in " + owner.getName() + " must have no parameters");
        }
        if (m.getReturnType() != void.class) {
            throw new RuntimeException(annotName + " method '" + m.getName()
                    + "' must return void (returns "
                    + m.getReturnType().getSimpleName() + ")");
        }
        if (Modifier.isStatic(m.getModifiers())) {
            throw new RuntimeException(annotName + " method '" + m.getName()
                    + "' must not be static");
        }
    }

    // ---- phase 2: instantiate singletons -----------------------------------

    private void instantiateSingletons() {
        for (BeanDefinition def : registry.getAllDefinitions().values()) {
            if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
                getOrCreateBean(def);
            }
        }
    }

    // ---- bean creation core ------------------------------------------------

    private Object getOrCreateBean(BeanDefinition def) {
        if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
            Object cached = registry.getSingleton(def.getBeanName());
            if (cached != null) return cached;
        }
        return createBean(def);
    }

    private Object createBean(BeanDefinition def) {
        String name = def.getBeanName();

        if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
            if (!inCreation.add(name)) {
                throw new RuntimeException(
                        "Circular dependency detected for bean '" + name
                        + "'. Break the cycle with setter injection or ObjectFactory<T>.");
            }
        }

        try {
            // Step 1: resolve and invoke constructor
            Constructor<?> constructor = resolveConstructor(def.getBeanClass());
            Object[] args = resolveConstructorArgs(constructor);
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(args);

            // Step 2: cache singleton BEFORE field injection (avoids some circular dep issues)
            if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
                registry.putSingleton(name, instance);
            }

            // Step 3: inject @Autowired fields
            injectFields(instance);

            // Step 4: invoke @PostConstruct
            invokePostConstruct(instance, def);

            // Step 5: register @PreDestroy callback for singletons
            if (def.getScope() == BeanDefinition.Scope.SINGLETON
                    && def.getPreDestroyMethod() != null) {
                registry.registerDestructionCallback(name, instance, def.getPreDestroyMethod());
            }

            return instance;

        } catch (InvocationTargetException e) {
            throw new RuntimeException("Exception during creation of bean '"
                    + name + "': " + e.getTargetException().getMessage(),
                    e.getTargetException());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create bean '" + name + "'", e);
        } finally {
            inCreation.remove(name);
        }
    }

    // ---- constructor resolution --------------------------------------------

    private Constructor<?> resolveConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();

        // Single constructor — implicit injection regardless of @Autowired
        if (constructors.length == 1) {
            return constructors[0];
        }

        // Multiple — find the @Autowired one
        for (Constructor<?> c : constructors) {
            if (c.isAnnotationPresent(Autowired.class)) {
                return c;
            }
        }

        // Fallback: no-arg constructor
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No suitable constructor found for '"
                    + clazz.getName()
                    + "'. Add @Autowired to one constructor or provide a no-arg constructor.");
        }
    }

    private Object[] resolveConstructorArgs(Constructor<?> constructor) {
        Parameter[] params = constructor.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Qualifier qualifier = params[i].getAnnotation(Qualifier.class);
            args[i] = resolveBean(params[i].getType(),
                    qualifier != null ? qualifier.value() : null);
        }
        return args;
    }

    // ---- field injection ---------------------------------------------------

    private void injectFields(Object instance) throws Exception {
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) continue;

                // ObjectFactory<T> — inject a factory lambda
                if (field.getType().equals(ObjectFactory.class)) {
                    injectObjectFactory(instance, field);
                    continue;
                }

                if (Modifier.isFinal(field.getModifiers())) {
                    throw new RuntimeException(
                            "Cannot inject final field '" + field.getName()
                            + "' in " + instance.getClass().getName()
                            + ". Use constructor injection instead.");
                }

                Qualifier qualifier = field.getAnnotation(Qualifier.class);
                Object dependency = resolveBean(field.getType(),
                        qualifier != null ? qualifier.value() : null);

                field.setAccessible(true);
                field.set(instance, dependency);
            }
            current = current.getSuperclass();
        }
    }

    private void injectObjectFactory(Object instance, Field field) throws Exception {
        ParameterizedType generic = (ParameterizedType) field.getGenericType();
        Class<?> targetType = (Class<?>) generic.getActualTypeArguments()[0];
        ObjectFactory<?> factory = () -> getBean(targetType);
        field.setAccessible(true);
        field.set(instance, factory);
    }

    // ---- bean resolution ---------------------------------------------------

    private Object resolveBean(Class<?> type, String qualifierName) {
        if (qualifierName != null && !qualifierName.isBlank()) {
            BeanDefinition def = registry.getBeanDefinition(qualifierName);
            if (!type.isAssignableFrom(def.getBeanClass())) {
                throw new RuntimeException("Bean '" + qualifierName
                        + "' is of type " + def.getBeanClass().getName()
                        + " which is not assignable to " + type.getName());
            }
            return getOrCreateBean(def);
        }

        List<BeanDefinition> candidates = registry.findDefinitionsByType(type);
        if (candidates.isEmpty()) {
            throw new RuntimeException("No bean of type '" + type.getName()
                    + "' found. Did you forget @Component?");
        }
        if (candidates.size() > 1) {
            List<String> names = candidates.stream()
                    .map(BeanDefinition::getBeanName).toList();
            throw new RuntimeException("Multiple beans of type '" + type.getName()
                    + "': " + names + ". Use @Qualifier(\"beanName\") to specify one.");
        }
        return getOrCreateBean(candidates.get(0));
    }

    // ---- lifecycle invocation ----------------------------------------------

    private void invokePostConstruct(Object instance, BeanDefinition def) {
        Method m = def.getPostConstructMethod();
        if (m == null) return;
        try {
            m.setAccessible(true);
            m.invoke(instance);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            throw new RuntimeException("@PostConstruct method '" + m.getName()
                    + "' in " + def.getBeanClass().getName()
                    + " threw: " + cause.getMessage(), cause);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke @PostConstruct on '"
                    + def.getBeanName() + "'", e);
        }
    }

    // ---- shutdown ----------------------------------------------------------

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(this::close, "minispring-shutdown"));
    }

    public void close() {
        System.out.println("[MiniSpring] Shutting down...");
        registry.destroyAll();
        System.out.println("[MiniSpring] Shutdown complete");
    }

    // ---- public API --------------------------------------------------------

    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        BeanDefinition def = registry.getBeanDefinition(name);
        if (def.getScope() == BeanDefinition.Scope.PROTOTYPE) {
            return (T) createBean(def);
        }
        return (T) getOrCreateBean(def);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        List<BeanDefinition> candidates = registry.findDefinitionsByType(type);
        if (candidates.isEmpty()) {
            throw new RuntimeException("No bean of type '" + type.getName() + "' found.");
        }
        if (candidates.size() > 1) {
            List<String> names = candidates.stream()
                    .map(BeanDefinition::getBeanName).toList();
            throw new RuntimeException(
                    "Multiple beans of type '" + type.getName() + "': " + names
                    + ". Use getBean(String name) or @Qualifier.");
        }
        return (T) getOrCreateBean(candidates.get(0));
    }
}
