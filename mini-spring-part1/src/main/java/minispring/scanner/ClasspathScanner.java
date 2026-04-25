package minispring.scanner;

import minispring.annotation.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Scans one or more Java packages and returns all classes annotated
 * with {@link Component}.
 *
 * How it works:
 *   1. Convert each package name to a filesystem path (dots → slashes)
 *   2. Ask the classloader for URLs that match that path
 *   3. Walk the directory or JAR, finding all .class files
 *   4. Load each class via Class.forName (WITHOUT initializing static blocks)
 *   5. Filter: keep only those with @Component that are concrete instantiable
 *
 * Security notes:
 *   - Never call scan("") — you would load every class on the classpath
 *   - The 'initialize=false' flag prevents hostile static blocks from executing
 *   - Only scan packages belonging to trusted application code
 */
public class ClasspathScanner {

    /**
     * Scan the given base packages and return all @Component-annotated,
     * non-abstract, non-interface, non-annotation classes.
     *
     * @param basePackages one or more package names, e.g. "app.service"
     * @return list of component classes ready for registration
     */
    public List<Class<?>> scan(String... basePackages) {
        List<Class<?>> componentClasses = new ArrayList<>();

        for (String basePackage : basePackages) {
            List<Class<?>> allClasses = findAllClasses(basePackage);

            for (Class<?> clazz : allClasses) {
                if (isComponent(clazz)) {
                    componentClasses.add(clazz);
                }
            }
        }

        return componentClasses;
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private boolean isComponent(Class<?> clazz) {
        return clazz.isAnnotationPresent(Component.class)
                && !clazz.isInterface()
                && !clazz.isAnnotation()
                && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers());
    }

    private List<Class<?>> findAllClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');

        try {
            ClassLoader classLoader =
                    Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                URI uri = resource.toURI();

                if ("file".equals(uri.getScheme())) {
                    // Running from exploded directory (IDE or Maven target/)
                    scanDirectory(Paths.get(uri), packageName, classes);
                } else if ("jar".equals(uri.getScheme())) {
                    // Running from a packaged JAR
                    scanJar(uri, path, packageName, classes);
                }
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(
                    "Failed to scan package '" + packageName + "'", e);
        }

        return classes;
    }

    private void scanDirectory(Path directory, String packageName,
                               List<Class<?>> classes) {
        if (!Files.exists(directory)) return;

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".class"))
                 .forEach(p -> {
                     // Convert path to fully-qualified class name
                     String relative = directory.relativize(p).toString();
                     String className = packageName + "."
                             + relative.replace(File.separatorChar, '.')
                                       .replace(".class", "");
                     loadClass(className, classes);
                 });
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to walk directory: " + directory, e);
        }
    }

    private void scanJar(URI jarUri, String path, String packageName,
                         List<Class<?>> classes) {
        try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of())) {
            Path jarPath = fs.getPath(path);
            if (!Files.exists(jarPath)) return;

            try (Stream<Path> paths = Files.walk(jarPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".class"))
                     .forEach(p -> {
                         String fullPath = p.toString();
                         // JAR paths always start with '/' — strip it
                         if (fullPath.startsWith("/")) {
                             fullPath = fullPath.substring(1);
                         }
                         String className = fullPath.replace('/', '.')
                                                    .replace(".class", "");
                         loadClass(className, classes);
                     });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan JAR: " + jarUri, e);
        }
    }

    /**
     * Load a class by name WITHOUT running its static initializers.
     *
     * The second argument 'false' is critical:
     *   - true  → run static { } blocks immediately (side effects, potential crash)
     *   - false → load metadata only (safe, no side effects)
     *
     * We catch both ClassNotFoundException (class file missing) and
     * NoClassDefFoundError (class file present but a dependency is missing).
     */
    private void loadClass(String className, List<Class<?>> classes) {
        try {
            Class<?> clazz = Class.forName(
                    className,
                    false, // do NOT initialize
                    Thread.currentThread().getContextClassLoader());
            classes.add(clazz);
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Skip classes with missing dependencies — log a warning
            System.err.println("[Scanner] Warning: skipping " + className
                    + " — " + e.getMessage());
        }
    }
}
