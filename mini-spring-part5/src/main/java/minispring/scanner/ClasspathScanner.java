package minispring.scanner;

import minispring.annotation.Component;

import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Classpath scanner with meta-annotation support.
 *
 * A class is considered a component if:
 *   1. It is directly annotated with @Component, OR
 *   2. It has any annotation that is itself annotated with @Component
 *      (e.g. @Controller which has @Component as a meta-annotation)
 */
public class ClasspathScanner {

    public List<Class<?>> scan(String basePackage) {
        List<Class<?>> found = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            URL resource = loader.getResource(path);
            if (resource == null) {
                System.err.println("[Scanner] Package not found: " + basePackage);
                return found;
            }
            scanDirectory(new File(resource.toURI()), basePackage, loader, found);
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan package: " + basePackage, e);
        }

        System.out.println("[Scanner] Found " + found.size() + " component(s) in " + basePackage);
        return found;
    }

    private void scanDirectory(File dir, String packageName,
                               ClassLoader loader, List<Class<?>> found) throws Exception {
        if (!dir.exists() || !dir.isDirectory()) return;

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file, packageName + "." + file.getName(), loader, found);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + "."
                        + file.getName().replace(".class", "");
                // false = do NOT run static initializers during scan
                Class<?> clazz = Class.forName(className, false, loader);
                if (isComponent(clazz)) {
                    found.add(clazz);
                }
            }
        }
    }

    private boolean isComponent(Class<?> clazz) {
        // Direct @Component
        if (clazz.isAnnotationPresent(Component.class)) return true;

        // Meta-annotation: any annotation on the class that is itself @Component
        for (Annotation annotation : clazz.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Component.class)) {
                return true;
            }
        }
        return false;
    }
}
