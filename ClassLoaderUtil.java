package karunia.motor;

import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ClassLoaderUtil {
    public static ClassLoader classLoader;
    private ClassLoaderUtil() {
        // Hide constructor
    }

    public static ClassLoader createClassLoader(final List<URL> urlList) {
        return new URLClassLoader(urlList.toArray(new URL[0]), null);
    }

    public static Object loadClass(final ClassLoader classLoader, final String className, final Object... args) throws Exception {
        final Class<?> clazz = classLoader.loadClass(className);
        final List<Constructor<?>> constructorList = Arrays.stream(clazz.getConstructors()).collect(Collectors.toList());
        for(final Constructor<?> constructor : constructorList) {
            if (checkNonEqualArgs(classLoader, args, constructor.getParameters())) {
                continue;
            }

            return constructor.newInstance(args);
        }

        throw new Exception("");
    }

    private static List<Object> convertToList(final Object[] objectArray) {
        return objectArray == null ? new ArrayList<>() : Arrays.stream(objectArray).collect(Collectors.toList());
    }

    public static boolean checkNonEqualArgs(final ClassLoader classLoader, final Object[] args1Array, final Object[] args2Array) throws ClassNotFoundException {
        return checkNonEqualArgs(classLoader, convertToList(args1Array), convertToList(args2Array));
    }

    public static boolean checkNonEqualArgs(final ClassLoader classLoader, final List<Object> args1List, final List<Object> args2List) throws ClassNotFoundException {
        if (args1List.size() != args2List.size()) {
            return true;
        }

        if (args1List.toString().equals(args2List.toString())) {
            return false;
        }

        for(int index = 0; index < args1List.size(); index++) {
            final Object args1 = args1List.get(index);
            final Object args2 = args2List.get(index);
            if (getArgumentClass(classLoader, args1).equals(getArgumentClass(classLoader, args2))) {
                return false;
            }

            System.out.println(args1.getClass().isInterface());
        }

        return true;
    }

    private static Class<?> getArgumentClass(final ClassLoader classLoader, final Object object) throws ClassNotFoundException {
        return object instanceof Parameter ? getParameterClass(classLoader, (Parameter) object) : object.getClass();
    }

    public static Class<?> getParameterClass(final ClassLoader classLoader, final Parameter parameter) throws ClassNotFoundException {
        return classLoader.loadClass(parameter.getParameterizedType().getTypeName());
    }
}
