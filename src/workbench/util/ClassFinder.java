/*
 * ClassFinder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class to search a list of jar files for an implementatioin of a specific interface.
 *
 * @see workbench.gui.profiles.DriverEditorPanel
 * @see workbench.gui.settings.LnFDefinitionPanel
 * 
 * @author Thomas Kellerer
 */
public class ClassFinder
{
	private Class toFind;

	public ClassFinder(Class clz)
	{
		toFind = clz;
	}

	/**
	 * Search all files for an implementation of java.sql.Driver.
	 * <br/>
	 * The first match will be returned.
	 *
	 * @param jarFiles
	 * @return the first classname found to implement java.sql.Driver
	 * @throws java.io.IOException
	 */
	public List<String> findImplementations(List<String> jarFiles)
		throws IOException
	{
		List<String> result = new ArrayList<String>();
		ClassLoader loader = buildClassLoader(jarFiles);

		for (String file : jarFiles)
		{
			File f = new File(file);
			if (f.isFile())
			{
				List<String> drivers = processJarFile(file, loader);
				result.addAll(drivers);
			}
		}
		return result;
	}

	private List<String> processJarFile(String archive, ClassLoader loader)
		throws IOException
	{
		List<String> result = new ArrayList<String>();

		JarFile jarFile = new JarFile(archive);
		try
		{
			Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements())
			{
				JarEntry entry = entries.nextElement();
				String name = entry.getName();
				if (!name.endsWith(".class")) continue;
				if (name.indexOf('$') > -1) continue;

				// An entry in a jar file is returned like e.g. org/postgresql/Driver.class
				// we need to convert this to a format that can be used as a "real" classname
				// it's important to replace the .class "extension" with an empty string first
				// because after all slashes have been replaced with a dot something like
				// somepackage.classprefix.SomeClass could exist
				String clsName = name.replace(".class", "").replace("/", ".");

				try
				{
					Class clz = loader.loadClass(clsName);
					if (toFind.isAssignableFrom(clz))
					{
						result.add(clsName);
					}
				}
				catch (Throwable cnf)
				{
					// ignore
				}
			}
		}
		finally
		{
			jarFile.close();
		}

		return result;
	}

	private ClassLoader buildClassLoader(List<String> files)
		throws MalformedURLException
	{
		if (files == null) return null;

		URL[] url = new URL[files.size()];

		for (int i=0; i < files.size(); i++)
		{
			File f = new File(files.get(i));
			url[i] = f.toURI().toURL();
		}

		ClassLoader classLoader = new URLClassLoader(url, ClassLoader.getSystemClassLoader());
		return classLoader;
	}

	// Taken from http://snippets.dzone.com/posts/show/4831
	
	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static List<Class> getClasses(String packageName)
		throws ClassNotFoundException, IOException
	{
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements())
		{
			URL resource = resources.nextElement();
			String fileName = resource.getFile();
			String fileNameDecoded = URLDecoder.decode(fileName, "UTF-8");
			dirs.add(new File(fileNameDecoded));
		}
		ArrayList<Class> classes = new ArrayList<Class>();
		for (File directory : dirs)
		{
			classes.addAll(findClasses(directory, packageName));
		}
		return classes;
	}

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param directory   The base directory
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     * @throws ClassNotFoundException
     */
	@SuppressWarnings("unchecked")
	private static List<Class> findClasses(File directory, String packageName)
		throws ClassNotFoundException
	{
		List<Class> classes = new ArrayList<Class>();
		if (!directory.exists())
		{
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files)
		{
			String fileName = file.getName();
			if (file.isDirectory())
			{
				assert !fileName.contains(".");
				classes.addAll(findClasses(file, packageName + "." + fileName));
			}
			else if (fileName.endsWith(".class") && !fileName.contains("$"))
			{
				Class _class;
				try
				{
					_class = Class.forName(packageName + '.' + fileName.substring(0, fileName.length() - 6));
					classes.add(_class);
				}
				catch (Exception cnf)
				{
				}
			}
		}
		return classes;
	}
}
