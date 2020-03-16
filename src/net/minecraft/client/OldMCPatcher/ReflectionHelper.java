package net.minecraft.client.OldMCPatcher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionHelper {
    public static Method getDeclaredMethod(Class clazz, String name, Class... param) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(name, param);
        method.setAccessible(true);
        return method;
    }

    public static Method getDeclaredMethod(String clazz, String name, Class... param) throws ClassNotFoundException, NoSuchMethodException {
        return getDeclaredMethod(ReflectionHelper.class.getClassLoader().loadClass(name), name, param);
    }

    public static Object waitAndInvoke(Method method, Object obj, Object... param) throws InvocationTargetException, IllegalAccessException {
        Object result;
        while((result = method.invoke(obj, param))==null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }}
        return result;
    }

    public static Field getDeclaredField(Class clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static Field getField(Class clazz, String name) throws NoSuchFieldException {
        Field field = clazz.getField(name);
        field.setAccessible(true);
        return field;
    }

    public static Object waitAndGet(Field field, Object obj) throws IllegalAccessException {
        Object result;
        while((result = field.get(obj))==null) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static void defineClass(ClassLoader classLoader, String className, byte[] bytes) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = getDeclaredMethod(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class);
        method.invoke(classLoader, className, bytes, 0, bytes.length);
    }

    public static void defineClass(ClassLoader classLoader, String className, String resourceName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream stream = ReflectionHelper.class.getResourceAsStream(resourceName);
        byte[] buff = new byte[128];
        int len;
        while((len = stream.read(buff))!=-1) {
            baos.write(buff, 0, len);
        }
        stream.close();
        defineClass(classLoader, className, baos.toByteArray());
        baos.close();
    }
}
