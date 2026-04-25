package minispring.container;

import java.lang.reflect.Method;

/**
 * Metadata about a single bean managed by the container.
 *
 * Stores:
 *   - The class to instantiate
 *   - The bean name used for lookup
 *   - The scope (singleton or prototype)
 *   - References to the @PostConstruct and @PreDestroy methods (if any)
 */
public class BeanDefinition {

    private final Class<?> beanClass;
    private final String   beanName;
    private final Scope    scope;

    private Method postConstructMethod;
    private Method preDestroyMethod;

    public enum Scope {
        SINGLETON, PROTOTYPE;

        public static Scope fromString(String value) {
            return switch (value.toLowerCase()) {
                case "singleton"  -> SINGLETON;
                case "prototype"  -> PROTOTYPE;
                default -> throw new IllegalArgumentException(
                        "Unknown scope: '" + value
                        + "'. Supported values: singleton, prototype");
            };
        }
    }

    public BeanDefinition(Class<?> beanClass, String beanName, Scope scope) {
        this.beanClass = beanClass;
        this.beanName  = beanName;
        this.scope     = scope;
    }

    public Class<?> getBeanClass()             { return beanClass; }
    public String   getBeanName()              { return beanName; }
    public Scope    getScope()                 { return scope; }
    public Method   getPostConstructMethod()   { return postConstructMethod; }
    public Method   getPreDestroyMethod()      { return preDestroyMethod; }
    public void     setPostConstructMethod(Method m) { postConstructMethod = m; }
    public void     setPreDestroyMethod(Method m)    { preDestroyMethod = m; }

    @Override
    public String toString() {
        return "BeanDefinition{name='" + beanName + "', class="
                + beanClass.getSimpleName() + ", scope=" + scope + "}";
    }
}
