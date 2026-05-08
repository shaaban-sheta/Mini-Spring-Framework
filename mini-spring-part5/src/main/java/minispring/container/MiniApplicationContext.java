package minispring.container;

import minispring.annotation.*;
import minispring.aop.ProxyFactory;
import minispring.scanner.ClasspathScanner;

import java.lang.reflect.*;
import java.util.*;

/**
 * MiniApplicationContext — Part 5 (Complete Framework).
 *
 * Extended from Part 4 with:
 *   - AOP proxy wrapping after @PostConstruct
 *   - ProxyFactory injected into the context for controller access to cache eviction etc.
 */
public class MiniApplicationContext {

    private final BeanRegistry      registry   = new BeanRegistry();
    private final BeanNameGenerator nameGen    = new BeanNameGenerator();
    private final ProxyFactory      proxyFactory = new ProxyFactory();
    private final Set<String>       inCreation  = new HashSet<>();

    public MiniApplicationContext(String basePackage) {
        List<Class<?>> components = new ClasspathScanner().scan(basePackage);
        registerAll(components);
        instantiateSingletons();
        registerShutdownHook();
        System.out.println("[MiniSpring] Container ready with "
                + registry.getAllDefinitions().size() + " bean(s)");
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
    }

    private BeanDefinition.Scope determineScope(Class<?> clazz) {
        Scope ann = clazz.getAnnotation(Scope.class);
        return ann != null
                ? BeanDefinition.Scope.fromString(ann.value())
                : BeanDefinition.Scope.SINGLETON;
    }

    private void discoverLifecycleMethods(Class<?> clazz, BeanDefinition def) {
        Method post = null, pre = null;
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Method m : current.getDeclaredMethods()) {
                if (m.isAnnotationPresent(PostConstruct.class)) {
                    validateLifecycleMethod(m, "@PostConstruct", clazz);
                    if (post != null) throw new RuntimeException(
                            "Multiple @PostConstruct in " + clazz.getName());
                    post = m;
                }
                if (m.isAnnotationPresent(PreDestroy.class)) {
                    validateLifecycleMethod(m, "@PreDestroy", clazz);
                    if (pre != null) throw new RuntimeException(
                            "Multiple @PreDestroy in " + clazz.getName());
                    pre = m;
                }
            }
            current = current.getSuperclass();
        }
        def.setPostConstructMethod(post);
        def.setPreDestroyMethod(pre);
    }

    private void validateLifecycleMethod(Method m, String annName, Class<?> owner) {
        if (m.getParameterCount() != 0)
            throw new RuntimeException(annName + " '" + m.getName()
                    + "' in " + owner.getName() + " must have no parameters");
        if (m.getReturnType() != void.class)
            throw new RuntimeException(annName + " '" + m.getName() + "' must return void");
        if (Modifier.isStatic(m.getModifiers()))
            throw new RuntimeException(annName + " '" + m.getName() + "' must not be static");
    }

    // ---- phase 2: instantiate singletons -----------------------------------

    private void instantiateSingletons() {
        for (BeanDefinition def : registry.getAllDefinitions().values()) {
            if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
                getOrCreateBean(def);
            }
        }
    }

    // ---- bean creation -----------------------------------------------------

    private Object getOrCreateBean(BeanDefinition def) {
        if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
            Object cached = registry.getSingleton(def.getBeanName());
            if (cached != null) return cached;
        }
        return createBean(def);
    }

    private Object createBean(BeanDefinition def) {
        String name = def.getBeanName();
        if (def.getScope() == BeanDefinition.Scope.SINGLETON && !inCreation.add(name)) {
            throw new RuntimeException("Circular dependency for '" + name + "'");
        }
        try {
            Constructor<?> ctor = resolveConstructor(def.getBeanClass());
            Object[] args       = resolveConstructorArgs(ctor);
            ctor.setAccessible(true);
            Object instance     = ctor.newInstance(args);

            if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
                registry.putSingleton(name, instance);
            }

            injectFields(instance);
            invokePostConstruct(instance, def);

            // AOP proxy wrapping — must happen AFTER @PostConstruct
            Class<?>[] interfaces = def.getBeanClass().getInterfaces();
            if (interfaces.length > 0) {
                Object proxied = proxyFactory.createProxy(instance, interfaces);
                if (proxied != instance) {
                    instance = proxied;
                    if (def.getScope() == BeanDefinition.Scope.SINGLETON) {
                        registry.putSingleton(name, instance); // cache the proxy
                    }
                }
            }

            if (def.getScope() == BeanDefinition.Scope.SINGLETON
                    && def.getPreDestroyMethod() != null) {
                registry.registerDestructionCallback(
                        name, instance, def.getPreDestroyMethod());
            }
            return instance;

        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error in bean '" + name + "': "
                    + e.getTargetException().getMessage(), e.getTargetException());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create '" + name + "'", e);
        } finally {
            inCreation.remove(name);
        }
    }

    // ---- constructor resolution --------------------------------------------

    private Constructor<?> resolveConstructor(Class<?> clazz) {
        Constructor<?>[] ctors = clazz.getDeclaredConstructors();
        if (ctors.length == 1) return ctors[0];
        for (Constructor<?> c : ctors) {
            if (c.isAnnotationPresent(Autowired.class)) return c;
        }
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No suitable constructor for " + clazz.getName());
        }
    }

    private Object[] resolveConstructorArgs(Constructor<?> ctor) {
        Parameter[] params = ctor.getParameters();
        Object[] args = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            Qualifier q = params[i].getAnnotation(Qualifier.class);
            args[i] = resolveBean(params[i].getType(), q != null ? q.value() : null);
        }
        return args;
    }

    // ---- field injection ---------------------------------------------------

    private void injectFields(Object instance) throws Exception {
        Class<?> current = instance.getClass();
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Autowired.class)) continue;
                if (field.getType().equals(ObjectFactory.class)) {
                    ParameterizedType generic = (ParameterizedType) field.getGenericType();
                    Class<?> target = (Class<?>) generic.getActualTypeArguments()[0];
                    ObjectFactory<?> factory = () -> getBean(target);
                    field.setAccessible(true);
                    field.set(instance, factory);
                    continue;
                }
                if (Modifier.isFinal(field.getModifiers()))
                    throw new RuntimeException("Cannot inject final field '"
                            + field.getName() + "'");
                Qualifier q = field.getAnnotation(Qualifier.class);
                Object dep = resolveBean(field.getType(), q != null ? q.value() : null);
                field.setAccessible(true);
                field.set(instance, dep);
            }
            current = current.getSuperclass();
        }
    }

    // ---- bean resolution ---------------------------------------------------

    private Object resolveBean(Class<?> type, String qualifierName) {
        if (qualifierName != null && !qualifierName.isBlank()) {
            BeanDefinition def = registry.getBeanDefinition(qualifierName);
            if (!type.isAssignableFrom(def.getBeanClass()))
                throw new RuntimeException("Bean '" + qualifierName
                        + "' is not assignable to " + type.getName());
            return getOrCreateBean(def);
        }
        List<BeanDefinition> candidates = registry.findDefinitionsByType(type);
        if (candidates.isEmpty())
            throw new RuntimeException("No bean of type " + type.getName());
        if (candidates.size() > 1)
            throw new RuntimeException("Multiple beans of type " + type.getName()
                    + ": " + candidates.stream().map(BeanDefinition::getBeanName).toList());
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
            throw new RuntimeException("@PostConstruct '" + m.getName()
                    + "' threw: " + e.getTargetException().getMessage(),
                    e.getTargetException());
        } catch (Exception e) {
            throw new RuntimeException("@PostConstruct failed on '"
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
        return def.getScope() == BeanDefinition.Scope.PROTOTYPE
                ? (T) createBean(def)
                : (T) getOrCreateBean(def);
    }

    @SuppressWarnings("unchecked")
    public <T> T getBean(Class<T> type) {
        List<BeanDefinition> candidates = registry.findDefinitionsByType(type);
        if (candidates.isEmpty())
            throw new RuntimeException("No bean of type " + type.getName());
        if (candidates.size() > 1)
            throw new RuntimeException("Multiple beans of type " + type.getName());
        return (T) getOrCreateBean(candidates.get(0));
    }

    public Map<String, BeanDefinition> getAllBeanDefinitions() {
        return registry.getAllDefinitions();
    }

    public ProxyFactory getProxyFactory() {
        return proxyFactory;
    }
}
