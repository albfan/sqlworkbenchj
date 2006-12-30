/*
 * LnFManager.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.util.ArrayList;
import java.util.List;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import workbench.resource.Settings;

/**
 *
 * @author support@sql-workbench.net
 */
public class LnFManager
{
	private List lnfList = new ArrayList();
	
	public LnFManager()
	{
		LookAndFeelInfo[] info = UIManager.getInstalledLookAndFeels();

		for (int i = 0; i < info.length; i++)
		{
			LnFDefinition lnf = new LnFDefinition(info[i].getName(), info[i].getClassName());
			lnfList.add(lnf);
		}

		Settings set = Settings.getInstance();
		int count = set.getIntProperty("workbench.lnf.count", 0);
		for (int i = 0; i < count; i++)
		{
			String clz = set.getProperty("workbench.lnf." + i + ".class", null);
			String name = set.getProperty("workbench.lnf." + i + ".name", clz);
			String libs = set.getProperty("workbench.lnf." + i + ".classpath", null);
			if (clz != null && libs != null)
			{
				LnFDefinition lnf = new LnFDefinition(name, clz, libs);
				lnfList.add(lnf);
			}
		}
	}
	
	public void removeDefinition(int index)
	{
		LnFDefinition def = (LnFDefinition)lnfList.get(index);
		if (!def.isBuiltInLnF())
		{
			this.lnfList.remove(index);
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
		int count = this.lnfList.size();
		int lnfCount = 0;
		for (int i = 0; i < count; i++)
		{
			LnFDefinition lnf = (LnFDefinition)lnfList.get(i);
			if (!lnf.isBuiltInLnF())
			{
				set.setProperty("workbench.lnf." + lnfCount + ".classpath", lnf.getLibrary());
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
	
	public List getAvailableLookAndFeels()
	{
		return lnfList;
	}
	
	public LnFDefinition findLookAndFeel(String className)
	{
		if (className == null) return null;
		List allLnf = getAvailableLookAndFeels();
		
		for (int i = 0; i < allLnf.size(); i++)
		{
			LnFDefinition lnf = (LnFDefinition)allLnf.get(i);
			if (lnf.getClassName().equals(className))
			{
				return lnf;
			}
		}
		
		
		return null;
	}
}
