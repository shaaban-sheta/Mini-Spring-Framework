package minispring.container;

import minispring.annotation.Component;
import minispring.web.annotation.Controller;

import java.lang.annotation.Annotation;

/**
 * Derives a bean name from the class annotation.
 *
 * Supports both direct @Component and meta-annotated @Controller, @Service etc.
 */
public class BeanNameGenerator {

    public String generateBeanName(Class<?> clazz) {
        // Direct @Component
        Component direct = clazz.getAnnotation(Component.class);
        if (direct != null && !direct.value().isBlank()) return direct.value();

        // @Controller (which has @Component as meta-annotation)
        Controller ctrl = clazz.getAnnotation(Controller.class);
        if (ctrl != null && !ctrl.value().isBlank()) return ctrl.value();

        // Default: camelCase simple name
        String simple = clazz.getSimpleName();
        return Character.toLowerCase(simple.charAt(0)) + simple.substring(1);
    }
}
