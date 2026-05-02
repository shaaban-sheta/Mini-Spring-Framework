package minispring.container;

import java.lang.reflect.Method;

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
                case "singleton" -> SINGLETON;
                case "prototype" -> PROTOTYPE;
                default -> throw new IllegalArgumentException(
                        "Unknown scope: '" + value + "'");
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
}
