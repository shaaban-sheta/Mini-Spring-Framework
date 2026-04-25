package minispring.container;

import minispring.annotation.Component;

public class BeanNameGenerator {
    private BeanNameGenerator() {}

    public static String generateBeanName(Class<?> clazz) {
        Component component = clazz.getAnnotation(Component.class);
        if (component != null && !component.value().isEmpty()) {
            return component.value();
        }
        String simpleName = clazz.getSimpleName();
        if (simpleName.isEmpty()) return clazz.getName();
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }
}
