package minispring.container;

/**
 * Derives a bean name for a class annotated with @Component.
 *
 * Rules:
 *   1. If @Component("explicitName") is provided, use that name.
 *   2. Otherwise, use the simple class name with the first letter lower-cased.
 *
 * Examples:
 *   UserService.class   -> "userService"
 *   @Component("repo")  -> "repo"
 */
public class BeanNameGenerator {

    public String generateBeanName(Class<?> clazz) {
        minispring.annotation.Component annotation =
                clazz.getAnnotation(minispring.annotation.Component.class);

        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }

        String simpleName = clazz.getSimpleName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
