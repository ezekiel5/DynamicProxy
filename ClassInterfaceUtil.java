package karunia.motor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public final class ClassInterfaceUtil {
    private static final Map<String, Object> objectMap = new HashMap<>();
    private ClassInterfaceUtil() {
        // Hide constructor
    }

    public static <T> T convertUrlJarsToInterface(final Class<T> interfaceClass, final List<URL> urlList, final String className, final Object... args) throws Exception {
        final ClassLoader classLoader = ClassLoaderUtil.createClassLoader(urlList);
        final Object object = ClassLoaderUtil.loadClass(classLoader, className, args);
        return convertObjectToInterface(interfaceClass, classLoader, object);
    }

    public static <T> T convertObjectToInterface(final Class<T> interfaceClass, final ClassLoader classLoader, final Object objectToConvert) {
        objectMap.put(objectToConvert.toString(), objectToConvert);
        final Object newProxyInstance = Proxy.newProxyInstance(
            interfaceClass.getClassLoader(), new Class[] {interfaceClass},
            (proxy, iMethod, iMethodArgs) -> handleInterfaceMethodCall(classLoader, objectToConvert, iMethod, iMethodArgs));

        return interfaceClass.cast(newProxyInstance);
    }

    private static Object handleInterfaceMethodCall(final ClassLoader classLoader, final Object objectToConvert, final Method iMethod, final Object... iMethodArgs) throws Exception {
        final Map<String, List<Method>> methodMap = Arrays.stream(objectToConvert.getClass().getMethods()).collect(Collectors.groupingBy(Method::getName));
        final List<Method> methodList = methodMap.getOrDefault(iMethod.getName(), new ArrayList<>());

        for(final Method method : methodList) {
            if (ClassLoaderUtil.checkNonEqualArgs(classLoader, iMethod.getParameters(), method.getParameters())) {
                continue;
            }

            final Class<?> methodReturnType = iMethod.getReturnType();

            final Object[] parsedInterfaceArgs = parseInterfaceArgs(classLoader, method, iMethodArgs);

            method.setAccessible(true);
            final Object result = method.invoke(objectToConvert, parsedInterfaceArgs);

            if (isPrimitiveType(result)) {
                return result;
            }

            return convertObjectToInterface(methodReturnType, classLoader, result);
        }

        throw new Exception("");
    }

    private static Object[] parseInterfaceArgs(final ClassLoader classLoader, Method method, Object... iMethodArgs) throws ClassNotFoundException {
        final List<Object> objectList = new ArrayList<>();

        if (iMethodArgs == null) {
            return objectList.toArray();
        }

        for(int index = 0; index < iMethodArgs.length; index++) {
            final Parameter parameter = method.getParameters()[index];

            final Object arg = objectMap.get(iMethodArgs[index].toString());
            if (arg == null || isPrimitiveType(arg)) {
                objectList.add(iMethodArgs[index]);
                continue;
            }

            final Class<?> interfaceClass = ClassLoaderUtil.getParameterClass(classLoader, parameter);
            objectList.add(convertObjectToInterface(interfaceClass, classLoader, arg));
        }

        return objectList.toArray();
    }

    private static boolean isPrimitiveType(final Object object) {
        return object.getClass().toString().toLowerCase().startsWith("class java.lang.");
    }
}
