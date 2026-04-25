package minispring.container;

import minispring.annotation.Component;

/**
 * Generates bean names following Spring's naming convention.
 *
 * Rules (in priority order):
 *   1. If @Component has a non-empty value → use that value
 *   2. Otherwise → lowercase the first character of the simple class name
 *
 * Examples:
 *   @Component("myService")  →  "myService"
 *   @Component on UserService  →  "userService"
 *   @Component on HTTPClient   →  "hTTPClient"  (Spring's actual behavior)
 *
 * Note: Spring only lowercases the FIRST character, not the entire name.
 * HTTPClient → hTTPClient (not httpClient). This matches the JavaBeans spec.
 */
public class BeanNameGenerator {

    private BeanNameGenerator() {} // utility class

    /**
     * Generate the bean name for a given component class.
     *
     * @param clazz any class annotated with @Component
     * @return the bean name that will be used in the registry
     */
    public static String generateBeanName(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);

        // Explicit name provided → use it directly
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }

        // Generate from class name
        String simpleName = clazz.getSimpleName();
        if (simpleName.isEmpty()) {
            // Anonymous or local class — fall back to fully qualified name
            return clazz.getName();
        }

        // Lowercase only the first character
        return Character.toLowerCase(simpleName.charAt(0))
               + simpleName.substring(1);
    }
}
