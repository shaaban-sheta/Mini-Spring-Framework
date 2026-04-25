package minispring.scanner;

import minispring.annotation.Component;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans a base package on the classpath for classes annotated with @Component.
 *
 * Uses Class.forName(name, false, classLoader) to load classes without
 * running static initializers — safe for scanning.
 */
public class ClasspathScanner {

    public List<Class<?>> scan(String basePackage) {
        List<Class<?>> components = new ArrayList<>();
        String path = basePackage.replace('.', '/');
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        try {
            URL resource = loader.getResource(path);
            if (resource == null) {
                System.err.println("[Scanner] Package not found: " + basePackage);
                return components;
            }
            scanDirectory(new File(resource.toURI()), basePackage, loader, components);
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan package: " + basePackage, e);
        }

        return components;
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
                if (clazz.isAnnotationPresent(Component.class)) {
                    found.add(clazz);
                }
            }
        }
    }
}
