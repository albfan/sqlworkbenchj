/*
 * LnFLoader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import workbench.WbManager;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A class to manage Look and feels that can be loaded at runtime.
 * @author support@sql-workbench.net
 */
public class LnFLoader
{
	private LnFDefinition lnfDef;
	private String[] liblist;
	
	public LnFLoader(LnFDefinition definition)
		throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException
	{
		this.lnfDef = definition;
		if (!lnfDef.isBuiltInLnF())
		{
			String libs = definition.getLibrary();
			if (libs != null)
			{
				liblist = splitLibraries(libs);
			}
		}
	}

	public static String[] splitLibraries(String libList)
	{
		String[] result = null;
		if (libList.indexOf(StringUtil.getPathSeparator()) > -1)
		{
			result = libList.split(StringUtil.getPathSeparator());
		}
		else if (libList.indexOf(LnFDefinition.LNF_PATH_SEPARATOR) > -1)
		{
			result = libList.split(StringUtil.getPathSeparator());
		}
		else
		{
			// Assuming a single library
			result = new String[] { libList };
		}
		return result;
	}

	public boolean isAvailable()
	{
		if (this.lnfDef.isBuiltInLnF()) return true;
		try
		{
			ClassLoader loader = createLoader();
			String resName = this.lnfDef.getClassName().replace('.', '/') + ".class";
			URL cl = loader.getResource(resName);
			return (cl != null);
		}
		catch (Exception e)
		{
			return false;
		}
	}
	
	private ClassLoader createLoader()
		throws MalformedURLException
	{
		if (this.liblist != null)
		{
			URL[] url = new URL[this.liblist.length];
			for (int i=0; i < this.liblist.length; i++)
			{
				String fname = Settings.getInstance().replaceLibDirKey(liblist[i]);
				File f = new File(fname);
				if (!f.isAbsolute())
				{
					f = new File(WbManager.getInstance().getJarPath(), this.liblist[i]);
				}
				
				url[i] = f.toURL();
			}
			ClassLoader loader = new URLClassLoader(url, this.getClass().getClassLoader());
			return loader;
		}
		else
		{
			return null;
		}
	}
	
	public Class loadClass()
		throws ClassNotFoundException
	{
		Class lnfClass = null;
		try
		{
			ClassLoader loader = createLoader();
			if (loader != null)
			{
				// Tell the LNF class which classloader to use!
				// This is important otherwise, the LnF will no
				// initialize correctly
				UIManager.getDefaults().put("ClassLoader", loader);
				Thread.currentThread().setContextClassLoader(loader);
				lnfClass = loader.loadClass(this.lnfDef.getClassName());
			}
			else
			{
				// If no library is specified we assume the class
				// is available through the system classloader
				// My tests showed that the property is not set initially
				// so I assume this means "use system classloader"
				UIManager.getDefaults().put("ClassLoader", null);
				lnfClass = Class.forName(this.lnfDef.getClassName());
			}
		}
		catch (Exception e)
		{
			throw new ClassNotFoundException("Could not load class " + this.lnfDef.getClassName(),e);
		}
		return lnfClass;
	}

	public LookAndFeel getLookAndFeel()
		throws ClassNotFoundException
	{
		try
		{
			Class lnf = loadClass();
			return (LookAndFeel)lnf.newInstance();
		} 
		catch (Exception e)
		{
			throw new ClassNotFoundException("Could not load class " + this.lnfDef.getClassName(),e);
		}
	}
}

