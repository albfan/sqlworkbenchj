/*
 * LnFLoader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import workbench.WbManager;
import workbench.resource.Settings;

/**
 * A class to manage Look and feels that can be loaded at runtime.
 * @author Thomas Kellerer
 */
public class LnFLoader
{
	private LnFDefinition lnfDef;
	private List<String> liblist;

	public LnFLoader(LnFDefinition definition)
		throws ClassNotFoundException, MalformedURLException, InstantiationException, IllegalAccessException
	{
		this.lnfDef = definition;
		if (!lnfDef.isBuiltInLnF())
		{
			liblist = lnfDef.getLibraries();
		}
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
			URL[] url = new URL[this.liblist.size()];
			for (int i=0; i < this.liblist.size(); i++)
			{
				String fname = Settings.getInstance().replaceLibDirKey(liblist.get(i));
				File f = new File(fname);
				if (!f.isAbsolute())
				{
					f = new File(WbManager.getInstance().getJarPath(), this.liblist.get(i));
				}
				url[i] = f.toURI().toURL();
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
		return loadClass(this.lnfDef.getClassName());
	}

	public Class loadClass(String lnfClassName)
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
				lnfClass = loader.loadClass(lnfClassName);
			}
			else
			{
				// If no library is specified we assume the class
				// is available through the system classloader
				// My tests showed that the property is not set initially
				// so I assume this means "use system classloader"
				UIManager.getDefaults().put("ClassLoader", null);
				lnfClass = Class.forName(lnfClassName);
			}
		}
		catch (Exception e)
		{
			throw new ClassNotFoundException("Could not load class " + lnfClassName,e);
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

