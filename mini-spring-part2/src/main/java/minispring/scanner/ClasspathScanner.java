package minispring.scanner;

import minispring.annotation.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

public class ClasspathScanner {

    public List<Class<?>> scan(String... basePackages) {
        List<Class<?>> result = new ArrayList<>();
        for (String pkg : basePackages) {
            for (Class<?> clazz : findAllClasses(pkg)) {
                if (clazz.isAnnotationPresent(Component.class)
                        && !clazz.isInterface()
                        && !clazz.isAnnotation()
                        && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                    result.add(clazz);
                }
            }
        }
        return result;
    }

    private List<Class<?>> findAllClasses(String packageName) {
        List<Class<?>> classes = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = cl.getResources(path);
            while (resources.hasMoreElements()) {
                URI uri = resources.nextElement().toURI();
                if ("file".equals(uri.getScheme())) {
                    scanDirectory(Paths.get(uri), packageName, classes);
                } else if ("jar".equals(uri.getScheme())) {
                    scanJar(uri, path, packageName, classes);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to scan: " + packageName, e);
        }
        return classes;
    }

    private void scanDirectory(Path dir, String pkg, List<Class<?>> out) {
        if (!Files.exists(dir)) return;
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".class"))
                 .forEach(p -> {
                     String rel = dir.relativize(p).toString();
                     String cn  = pkg + "." + rel.replace(File.separatorChar, '.').replace(".class", "");
                     loadClass(cn, out);
                 });
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private void scanJar(URI uri, String path, String pkg, List<Class<?>> out) {
        try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
            Path jarPath = fs.getPath(path);
            if (!Files.exists(jarPath)) return;
            try (Stream<Path> paths = Files.walk(jarPath)) {
                paths.filter(Files::isRegularFile)
                     .filter(p -> p.toString().endsWith(".class"))
                     .forEach(p -> {
                         String s = p.toString();
                         if (s.startsWith("/")) s = s.substring(1);
                         loadClass(s.replace('/', '.').replace(".class", ""), out);
                     });
            }
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private void loadClass(String name, List<Class<?>> out) {
        try {
            out.add(Class.forName(name, false,
                    Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {}
    }
}
