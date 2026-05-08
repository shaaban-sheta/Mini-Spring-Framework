package minispring.web.router;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.*;

/**
 * Describes a single registered HTTP route.
 *
 * Holds the HTTP method (GET/POST), the URL path template (/users/{id}),
 * a compiled regex for matching, the handler Method, and the controller instance.
 */
public class RouteDefinition {

    private final String  httpMethod;
    private final String  pathTemplate;
    private final Pattern pathPattern;
    private final List<String> pathVariableNames;
    private final Method  handlerMethod;
    private final Object  controllerInstance;

    public RouteDefinition(String httpMethod, String pathTemplate,
                           Method handlerMethod, Object controllerInstance) {
        this.httpMethod         = httpMethod.toUpperCase();
        this.pathTemplate       = pathTemplate;
        this.handlerMethod      = handlerMethod;
        this.controllerInstance = controllerInstance;

        // Extract named variables: /users/{id} -> ["id"]
        this.pathVariableNames = new ArrayList<>();
        Matcher varMatcher = Pattern.compile("\\{([^/]+)\\}").matcher(pathTemplate);
        while (varMatcher.find()) {
            pathVariableNames.add(varMatcher.group(1));
        }

        // Compile to named-capture-group regex: /users/{id} -> ^/users/(?<id>[^/]+)$
        String regex = pathTemplate.replaceAll("\\{([^/]+)\\}", "(?<$1>[^/]+)");
        this.pathPattern = Pattern.compile("^" + regex + "$");
    }

    public boolean matches(String method, String path) {
        if (!this.httpMethod.equalsIgnoreCase(method)) return false;
        String cleanPath = stripQuery(path);
        return pathPattern.matcher(cleanPath).matches();
    }

    public Map<String, String> extractPathVariables(String path) {
        String cleanPath = stripQuery(path);
        Matcher m = pathPattern.matcher(cleanPath);
        Map<String, String> vars = new LinkedHashMap<>();
        if (m.matches()) {
            for (String name : pathVariableNames) {
                vars.put(name, m.group(name));
            }
        }
        return vars;
    }

    private String stripQuery(String path) {
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    public String getHttpMethod()         { return httpMethod; }
    public String getPathTemplate()       { return pathTemplate; }
    public Method getHandlerMethod()      { return handlerMethod; }
    public Object getControllerInstance() { return controllerInstance; }

    @Override
    public String toString() {
        return httpMethod + " " + pathTemplate + " -> "
                + handlerMethod.getDeclaringClass().getSimpleName()
                + "." + handlerMethod.getName() + "()";
    }
}
