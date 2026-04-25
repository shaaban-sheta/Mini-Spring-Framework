package minispring.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Spring-managed component (a "bean").
 *
 * The IoC container discovers classes annotated with @Component during
 * classpath scanning and registers them in the bean registry.
 *
 * Usage:
 *   @Component                  → bean name generated from class name
 *   @Component("myBeanName")    → explicit bean name
 *
 * Rules enforced here at compile time:
 *   - Can only be placed on types (classes, not methods or fields)
 *   - Must have RUNTIME retention so reflection can see it
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

    /**
     * Optional explicit bean name.
     * If empty (""), the container generates a name from the class name
     * by lowercasing the first character: UserService → "userService".
     */
    String value() default "";
}
