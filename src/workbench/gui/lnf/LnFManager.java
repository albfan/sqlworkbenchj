/*
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import workbench.resource.Settings;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * Retrieve and store LnF definitions in the the Settings object.
 *
 * @author Thomas Kellerer
 */
public class LnFManager
{
	private List<LnFDefinition> lnfList = new ArrayList<>();

	public LnFManager()
	{
		Settings set = Settings.getInstance();

		int count = set.getIntProperty("workbench.lnf.count", 0);
		for (int i = 0; i < count; i++)
		{
			String clz = set.getProperty("workbench.lnf." + i + ".class", "");
			String name = set.getProperty("workbench.lnf." + i + ".name", clz);
			String libs = set.getProperty("workbench.lnf." + i + ".classpath", "");
			List<String> liblist = null;
			if (libs.contains(LnFDefinition.LNF_PATH_SEPARATOR))
			{
				liblist = StringUtil.stringToList(libs, LnFDefinition.LNF_PATH_SEPARATOR);
			}
			else
			{
				liblist = StringUtil.stringToList(libs, StringUtil.getPathSeparator());
			}
			if (clz != null && CollectionUtil.isNonEmpty(liblist))
			{
				LnFDefinition lnf = new LnFDefinition(name, clz, liblist);
				lnfList.add(lnf);
			}
		}

		// The Liquid Look & Feel "installs" itself as a System L&F and if
		// activated is returned in getInstalledLookAndFeels(). To avoid
		// a duplicate entry we check this before adding a "system" look and feel
		LookAndFeelInfo[] systemLnf = UIManager.getInstalledLookAndFeels();

		for (LookAndFeelInfo lnfInfo : systemLnf)
		{
			LnFDefinition lnf = new LnFDefinition(lnfInfo.getName(), lnfInfo.getClassName());
			if (!lnfList.contains(lnf))
			{
				lnfList.add(lnf);
			}
		}
		Comparator<LnFDefinition> nameComp = new Comparator<LnFDefinition>()
		{
			@Override
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
				String libs = StringUtil.listToString(lnf.getLibraries(), LnFDefinition.LNF_PATH_SEPARATOR, false);
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
