package hl.common;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ClassLoaderUtil {
	
	public static Object newClassInstance(String aClassName) throws ClassNotFoundException, InstantiationException
	{
		return newClassInstance((ClassLoader)null, aClassName);
	}
	
	public static Object newClassInstance(Class aClass, String aClassName) throws ClassNotFoundException, InstantiationException
	{
		if(aClass==null)
			aClass = ClassLoaderUtil.class;
		return newClassInstance(aClass.getClassLoader(), aClassName);
	}
	public static Object newClassInstance(ClassLoader aClassLoader, String aClassName) throws ClassNotFoundException, InstantiationException
	{
		if(aClassLoader==null)
			aClassLoader = ClassLoaderUtil.class.getClassLoader();
		
		Class<?> clazz = aClassLoader.loadClass(aClassName);
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (IllegalAccessException e) {
			throw new InstantiationException(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new InstantiationException(e.getMessage());
		} catch (InvocationTargetException e) {
			throw new InstantiationException(e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new InstantiationException(e.getMessage());
		}
	}
	
	public static Object getSingletonInstance(String aClassName, String aMethodName) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException
	{
		Class<?> clazz = Class.forName(aClassName);
		if(clazz!=null)
		{
			Method method = clazz.getDeclaredMethod(aMethodName);
			try {
				return method.invoke(null);
			} catch (IllegalAccessException e) {
				throw new InvocationTargetException(e);
			} 
		}
		return null;
	}
	
	/**
     * Scans the directory for .jar files and creates a URLClassLoader.
     */
    public static ClassLoader createCustomClassLoader(
    		Class aCallerClass, String aFolderPath) throws MalformedURLException {
    	
    	if(aCallerClass==null)
    		aCallerClass = ClassLoaderUtil.class;
    	
        File libDir = new File(aFolderPath);
        
        // Ensure directory exists
        if (!libDir.exists() || !libDir.isDirectory()) {
            throw new IllegalArgumentException("Library directory does not exist: " + libDir.getAbsolutePath());
        }

        // Filter for only .jar files
        File[] jarFiles = libDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        
        if (jarFiles == null || jarFiles.length == 0) {
            System.out.println("No JAR files found in: " + libDir.getAbsolutePath());
            return new URLClassLoader(new URL[0]);
        }

        // Convert File objects to URLs
        URL[] urls = new URL[jarFiles.length+1];
        for (int i = 0; i < jarFiles.length; i++) {
            // Use toURI().toURL() - standard File.toURL() is deprecated
            urls[i] = jarFiles[i].toURI().toURL(); 
        }
        urls[jarFiles.length+1] = libDir.toURI().toURL(); // Add the directory itself to the classpath

        // Create the classloader. 
        // We pass PluginManager.class.getClassLoader() as the parent so the plugin 
        // shares core classes with the main application.
        return new URLClassLoader(urls, aCallerClass.getClassLoader());
    }
	
}
