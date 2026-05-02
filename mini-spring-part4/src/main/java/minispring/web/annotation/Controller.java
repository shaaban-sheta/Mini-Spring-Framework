package minispring.web.annotation;

import minispring.annotation.Component;
import java.lang.annotation.*;

/**
 * Marks a class as an HTTP request handler (controller).
 *
 * Uses @Component as a meta-annotation so that @Controller-annotated classes
 * are automatically picked up by the classpath scanner — no separate @Component needed.
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String value() default "";
}
