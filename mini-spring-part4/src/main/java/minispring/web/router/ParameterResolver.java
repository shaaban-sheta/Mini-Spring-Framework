package minispring.web.router;

import minispring.web.annotation.PathVariable;
import minispring.web.annotation.RequestBody;
import minispring.web.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Resolves the argument array for a handler method from HTTP request data.
 *
 * Supported parameter annotations:
 *   @PathVariable("name")          — from URL path variables
 *   @RequestParam("name")          — from query string
 *   @RequestBody                   — raw request body string
 *
 * Type coercion: String, int/Integer, long/Long, double/Double, boolean/Boolean.
 */
public class ParameterResolver {

    public Object[] resolve(Method method,
                            Map<String, String> pathVars,
                            Map<String, String> queryParams,
                            String body) {

        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];

            PathVariable pathVar  = param.getAnnotation(PathVariable.class);
            RequestParam reqParam = param.getAnnotation(RequestParam.class);
            RequestBody  reqBody  = param.getAnnotation(RequestBody.class);

            if (pathVar != null) {
                args[i] = convertType(pathVars.get(pathVar.value()), param.getType());
            } else if (reqParam != null) {
                String raw = queryParams.getOrDefault(reqParam.value(), reqParam.defaultValue());
                args[i] = convertType(raw.isBlank() ? null : raw, param.getType());
            } else if (reqBody != null) {
                args[i] = body;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    public Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isBlank()) return params;

        for (String pair : query.split("&")) {
            if (pair.isBlank()) continue;
            String[] kv = pair.split("=", 2);
            String key   = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1
                    ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            params.put(key, value);
        }
        return params;
    }

    private Object convertType(String value, Class<?> target) {
        if (value == null) {
            if (target == int.class)    return 0;
            if (target == long.class)   return 0L;
            if (target == double.class) return 0.0;
            if (target == boolean.class) return false;
            return null;
        }
        if (target == String.class)                    return value;
        if (target == int.class || target == Integer.class) return Integer.parseInt(value);
        if (target == long.class || target == Long.class)   return Long.parseLong(value);
        if (target == double.class || target == Double.class) return Double.parseDouble(value);
        if (target == boolean.class || target == Boolean.class) return Boolean.parseBoolean(value);
        throw new RuntimeException("Cannot convert '" + value
                + "' to unsupported type: " + target.getName());
    }
}
