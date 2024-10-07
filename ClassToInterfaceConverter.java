package karunia.motor;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public final class ClassToInterfaceConverter {
    private final Map<String, Object> objectMap = new HashMap<>();

    public <T> T convert(final Class<T> interfaceClass, final List<URL> urlList, final String className, final Object... args) throws Exception {
        final ClassLoader classLoader = ClassLoaderUtil.createClassLoader(urlList);
        final Object object = ClassLoaderUtil.loadClass(classLoader, className, args);
        return convertObjectToInterface(interfaceClass, classLoader, object);
    }

    public <T> T convertObjectToInterface(final Class<T> interfaceClass, final ClassLoader classLoader, final Object objectToConvert) {
        final Object newProxyInstance = Proxy.newProxyInstance(
            interfaceClass.getClassLoader(), new Class[] {interfaceClass},
            (proxy, iMethod, iMethodArgs) -> handleInterfaceMethodCall(classLoader, objectToConvert, iMethod, iMethodArgs));

        objectMap.put(newProxyInstance.toString(), objectToConvert);
        return interfaceClass.cast(newProxyInstance);
    }

    private Object handleInterfaceMethodCall(final ClassLoader classLoader, final Object objectToConvert, final Method iMethod, final Object... iMethodArgs) throws Exception {
        final Map<String, List<Method>> methodMap = Arrays.stream(objectToConvert.getClass().getMethods()).collect(Collectors.groupingBy(Method::getName));
        final List<Method> methodList = methodMap.getOrDefault(iMethod.getName(), new ArrayList<>());

        for(final Method method : methodList) {
            if (ClassLoaderUtil.checkNonEqualArgs(classLoader, iMethod.getParameters(), method.getParameters())) {
                continue;
            }

            final Class<?> methodReturnType = iMethod.getReturnType();

            final Object[] parsedInterfaceArgs = parseInterfaceArgs(classLoader, iMethodArgs);

            method.setAccessible(true);
            final Object result = method.invoke(objectToConvert, parsedInterfaceArgs);

            if (isPrimitiveType(result)) {
                return result;
            }

            final Type type = iMethod.getGenericReturnType();

            if (type.getClass().toString().equals("class sun.reflect.generics.reflectiveObjects.TypeVariableImpl")) {
                for(int index = 0; index < iMethod.getParameters().length; index++) {
                    final Parameter parameter = iMethod.getParameters()[index];
                    final String parameterType = parameter.getParameterizedType().getTypeName().replaceAll("^java.lang.Class<", "").replaceAll(">", "");
                    if (type.toString().equals(parameterType)) {
                        return convertObjectToInterface((Class<?>) iMethodArgs[index], classLoader, result);
                    }
                }
            }

            if (!methodReturnType.isInterface()) {
                return result;
            }

            return convertObjectToInterface(methodReturnType, classLoader, result);
        }

        throw new Exception("");
    }

    private Object[] parseInterfaceArgs(final ClassLoader classLoader, Object... iMethodArgs) throws ClassNotFoundException {
        final List<Object> objectList = new ArrayList<>();

        if (iMethodArgs == null) {
            return objectList.toArray();
        }

        for (final Object iMethodArg : iMethodArgs) {

            final Object arg = objectMap.get(iMethodArg.toString());

            if (isClassType(iMethodArg)) {
                objectList.add(classLoader.loadClass(iMethodArg.toString().split(" ")[1]));
                continue;
            }

            if (arg == null || isPrimitiveType(arg)) {
                objectList.add(iMethodArg);
                continue;
            }

            objectList.add(arg);
        }

        return objectList.toArray();
    }

    private boolean isPrimitiveType(final Object object) {
        return object.getClass().toString().toLowerCase().startsWith("class java.lang.");
    }

    private boolean isClassType(final Object object) {
        return object.getClass().toString().toLowerCase().startsWith("class java.lang.class");
    }
}
