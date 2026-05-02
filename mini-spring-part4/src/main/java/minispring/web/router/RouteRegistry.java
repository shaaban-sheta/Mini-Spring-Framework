package minispring.web.router;

import minispring.web.annotation.GetMapping;
import minispring.web.annotation.PostMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans controller instances for @GetMapping and @PostMapping methods
 * and stores them as RouteDefinition objects for request dispatch.
 */
public class RouteRegistry {

    private final List<RouteDefinition> routes = new ArrayList<>();

    public void registerController(Object controller) {
        Class<?> clazz = controller.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            GetMapping  getAnn  = method.getAnnotation(GetMapping.class);
            PostMapping postAnn = method.getAnnotation(PostMapping.class);

            if (getAnn != null) {
                routes.add(new RouteDefinition("GET", getAnn.value(), method, controller));
                System.out.println("[RouteRegistry] GET  " + getAnn.value());
            }
            if (postAnn != null) {
                routes.add(new RouteDefinition("POST", postAnn.value(), method, controller));
                System.out.println("[RouteRegistry] POST " + postAnn.value());
            }
        }
    }

    public List<RouteDefinition> findRoute(String method, String path) {
        return routes.stream()
                .filter(r -> r.matches(method, path))
                .toList();
    }

    public List<RouteDefinition> getRoutes() {
        return List.copyOf(routes);
    }
}
