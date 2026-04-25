package minispring.container;

public class BeanDefinition {

    private final Class<?> beanClass;
    private final String   beanName;
    private final Scope    scope;

    public enum Scope {
        SINGLETON,
        PROTOTYPE
    }

    public BeanDefinition(Class<?> beanClass, String beanName, Scope scope) {
        this.beanClass = beanClass;
        this.beanName  = beanName;
        this.scope     = scope;
    }

    public Class<?> getBeanClass() { return beanClass; }
    public String   getBeanName()  { return beanName;  }
    public Scope    getScope()     { return scope;     }
}
