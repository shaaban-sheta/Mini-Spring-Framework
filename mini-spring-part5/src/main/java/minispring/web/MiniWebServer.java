package minispring.web;

import minispring.container.MiniApplicationContext;
import minispring.web.annotation.Controller;
import minispring.web.router.ParameterResolver;
import minispring.web.router.RouteDefinition;
import minispring.web.router.RouteRegistry;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Embedded HTTP server built on the JDK's built-in HttpServer.
 *
 * On startup:
 *   1. Finds all @Controller beans in the container (via meta-annotation check)
 *   2. Scans each controller's methods for @GetMapping / @PostMapping
 *   3. Registers them as RouteDefinitions
 *
 * On each request:
 *   1. Match URL and HTTP method to a RouteDefinition
 *   2. Extract path variables and parse query string
 *   3. Resolve method arguments via ParameterResolver
 *   4. Invoke the handler method via reflection
 *   5. Write the return value as the response body
 */
public class MiniWebServer {

    private final MiniApplicationContext context;
    private final RouteRegistry          routeRegistry = new RouteRegistry();
    private final ParameterResolver      paramResolver = new ParameterResolver();
    private       HttpServer             httpServer;

    public MiniWebServer(MiniApplicationContext context) {
        this.context = context;
        discoverControllers();
    }

    private void discoverControllers() {
        for (var entry : context.getAllBeanDefinitions().entrySet()) {
            Class<?> clazz = entry.getValue().getBeanClass();
            if (isController(clazz)) {
                Object instance = context.getBean(entry.getKey());
                routeRegistry.registerController(instance);
            }
        }
    }

    private boolean isController(Class<?> clazz) {
        if (clazz.isAnnotationPresent(Controller.class)) return true;
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().isAnnotationPresent(Controller.class)) return true;
        }
        return false;
    }

    public void start(int port) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        httpServer.createContext("/", this::handle);
        httpServer.start();
        System.out.println("[MiniWebServer] Listening on http://localhost:" + port);
        System.out.println("[MiniWebServer] Routes registered: "
                + routeRegistry.getRoutes().size());
    }

    public void stop() {
        if (httpServer != null) {
            httpServer.stop(1);
            System.out.println("[MiniWebServer] Stopped");
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String method  = exchange.getRequestMethod();
        String uri     = exchange.getRequestURI().toString();
        String path    = uri.contains("?") ? uri.substring(0, uri.indexOf('?')) : uri;
        String query   = exchange.getRequestURI().getQuery();

        System.out.println("[MiniWebServer] " + method + " " + uri);

        List<RouteDefinition> matched = routeRegistry.findRoute(method, path);

        if (matched.isEmpty()) {
            respond(exchange, 404, "{\"error\":\"Not Found: " + path + "\"}");
            return;
        }
        if (matched.size() > 1) {
            respond(exchange, 500, "{\"error\":\"Ambiguous route: " + path + "\"}");
            return;
        }

        RouteDefinition route = matched.get(0);
        try {
            Map<String, String> pathVars    = route.extractPathVariables(path);
            Map<String, String> queryParams = paramResolver.parseQueryString(query);
            String body = new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            Object[] args = paramResolver.resolve(
                    route.getHandlerMethod(), pathVars, queryParams, body);

            route.getHandlerMethod().setAccessible(true);
            Object result = route.getHandlerMethod()
                    .invoke(route.getControllerInstance(), args);

            String responseBody = result == null ? "{}" : result.toString();
            respond(exchange, 200, responseBody);

        } catch (InvocationTargetException e) {
            Throwable cause = e.getTargetException();
            System.err.println("[MiniWebServer] Handler error: " + cause.getMessage());
            respond(exchange, 500,
                    "{\"error\":\"" + escape(cause.getMessage()) + "\"}");
        } catch (Exception e) {
            System.err.println("[MiniWebServer] Error: " + e.getMessage());
            respond(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"");
    }
}
