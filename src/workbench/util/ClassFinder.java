/*
 * ClassFinder.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * A class to search a list of jar files for an implementatioin of an JDBC driver.
 *
 * @author Thomas Kellerer
 */
public class ClassFinder
{
	private Class toFind;

	public ClassFinder()
	{
		this(java.sql.Driver.class);
	}

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
	public List<String> findClass(List<String> jarFiles)
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
				if (name.indexOf("$") > -1) continue;

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
			jarFile.close();
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
}
