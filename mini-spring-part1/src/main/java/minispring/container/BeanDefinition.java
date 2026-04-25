package minispring.container;

/**
 * Holds metadata about a bean — the description of a bean before it is
 * instantiated.
 *
 * Separating the description (BeanDefinition) from the live object is a key
 * design decision that enables:
 *   - Lazy initialization: create instances only when requested
 *   - Scope management: decide at request time whether to reuse or create
 *   - Early validation: detect missing dependencies before creating anything
 *
 * Analogous to Spring's BeanDefinition.
 */
public class BeanDefinition {

    private final Class<?> beanClass;
    private final String   beanName;
    private final Scope    scope;

    public enum Scope {
        /** One shared instance for the entire application lifetime. */
        SINGLETON,
        /** New instance created on every getBean() call. */
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

    @Override
    public String toString() {
        return "BeanDefinition{"
                + "name='" + beanName + '\''
                + ", class=" + beanClass.getSimpleName()
                + ", scope=" + scope
                + '}';
    }
}
