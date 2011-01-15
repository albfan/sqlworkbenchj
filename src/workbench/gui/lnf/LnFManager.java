/*
 * LnFManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * Retrieve and store LnF definitions in the the Settings object
 * @author Thomas Kellerer
 */
public class LnFManager
{
	private List<LnFDefinition> lnfList = new ArrayList<LnFDefinition>();

	public LnFManager()
	{
		Settings set = Settings.getInstance();

		int count = set.getIntProperty("workbench.lnf.count", 0);
		for (int i = 0; i < count; i++)
		{
			String clz = set.getProperty("workbench.lnf." + i + ".class", "");
			String name = set.getProperty("workbench.lnf." + i + ".name", clz);
			String libs = set.getProperty("workbench.lnf." + i + ".classpath", "");
			libs = libs.replace(LnFDefinition.LNF_PATH_SEPARATOR, StringUtil.getPathSeparator());
			if (clz != null && libs != null)
			{
				LnFDefinition lnf = new LnFDefinition(name, clz, libs);
				lnfList.add(lnf);
			}
		}

		// The Liquid Look & Feel "installs" itself as a System L&F and if
		// activated is returned in getInstalledLookAndFeels(). To avoid
		// a duplicate entry we check this before adding a "system" look and feel
		LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();

		for (int i = 0; i < info.length; i++)
		{
			LnFDefinition lnf = new LnFDefinition(info[i].getName(), info[i].getClassName());
			if (!lnfList.contains(lnf))
			{
				lnfList.add(lnf);
			}
		}
		Comparator<LnFDefinition> nameComp = new Comparator<LnFDefinition>()
		{
			public int compare(LnFDefinition first, LnFDefinition second)
			{
				return StringUtil.compareStrings(first.getName(), second.getName(), true);
			}
		};
		Collections.sort(lnfList, nameComp);
	}

	public void removeDefinition(LnFDefinition lnf)
	{
		if (lnf == null) return;
		if (!lnf.isBuiltInLnF())
		{
			this.lnfList.remove(lnf);
		}
	}

	public LnFDefinition getCurrentLnF()
	{
		LookAndFeel lnf = UIManager.getLookAndFeel();
		String lnfClass = lnf.getClass().getName();
		return findLookAndFeel(lnfClass);
	}

	public int addDefinition(LnFDefinition lnf)
	{
		this.lnfList.add(lnf);
		return lnfList.size() - 1;
	}

	public void saveLookAndFeelDefinitions()
	{
		Settings set = Settings.getInstance();
		removeLnFEntries();
		int lnfCount = 0;
		for (LnFDefinition lnf : lnfList)
		{
			if (!lnf.isBuiltInLnF())
			{
				String libs = lnf.getLibrary();
				libs = StringUtil.replace(libs, StringUtil.getPathSeparator(), LnFDefinition.LNF_PATH_SEPARATOR);
				set.setProperty("workbench.lnf." + lnfCount + ".classpath", libs);
				set.setProperty("workbench.lnf." + lnfCount + ".class", lnf.getClassName());
				set.setProperty("workbench.lnf." + lnfCount + ".name", lnf.getName());
				lnfCount++;
			}
		}
		set.setProperty("workbench.lnf.count", lnfCount);
	}

	private void removeLnFEntries()
	{
		Settings set = Settings.getInstance();
		int count = set.getIntProperty("workbench.lnf.count", 0);
		for (int i = 0; i < count; i++)
		{
			set.removeProperty("workbench.lnf." + i + ".classpath");
			set.removeProperty("workbench.lnf." + i + ".class");
			set.removeProperty("workbench.lnf." + i + ".name");
		}
	}


	/**
	 * returns all LnFs defined in the system. This is
	 * a combined list of built-in LnFs and user-defined
	 * LnFs
	 *
	 * @return all available Look and Feels
	 * @see workbench.gui.lnf.LnFDefinition#isBuiltInLnF()
	 */
	public List<LnFDefinition> getAvailableLookAndFeels()
	{
		return Collections.unmodifiableList(lnfList);
	}

	public LnFDefinition findLookAndFeel(String className)
	{
		if (className == null) return null;

		for (LnFDefinition lnf : lnfList)
		{
			if (lnf.getClassName().equals(className))
			{
				return lnf;
			}
		}
		return null;
	}
}
