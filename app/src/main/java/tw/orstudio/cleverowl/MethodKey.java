package tw.orstudio.cleverowl;

import java.lang.reflect.Method;

/** Exact DEX method identity, including overload and return type. */
public final class MethodKey {
    private MethodKey() {}
    public static String type(Class<?> type) {
        if (type.isArray()) return type.getName().replace('.', '/');
        if (!type.isPrimitive()) return "L" + type.getName().replace('.', '/') + ";";
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        throw new IllegalArgumentException("Unsupported type");
    }
    public static String of(Method method) {
        StringBuilder key = new StringBuilder(type(method.getDeclaringClass())).append("->").append(method.getName()).append('(');
        for (Class<?> parameter : method.getParameterTypes()) key.append(type(parameter));
        return key.append(')').append(type(method.getReturnType())).toString();
    }
}
