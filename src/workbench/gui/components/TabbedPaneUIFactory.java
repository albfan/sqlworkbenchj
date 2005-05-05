/*
 * TabbedPaneUIFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.components;

import java.awt.Insets;

import javax.swing.JTabbedPane;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.TabbedPaneUI;

/**
 *
 * @author  info@sql-workbench.net
 */
public class TabbedPaneUIFactory
{
	private static Insets topInsets = new Insets(2,1,1,1);
	private static Insets bottomInsets = new Insets(1,1,2,1);
	private static Insets leftInsets = new Insets(1,3,1,1);
	private static Insets rightInsets = new Insets(1,1,1,3);
	private static Insets defaultInsets = new Insets(1,1,1,1);

	static Insets getBorderLessInsets(int tabPlacement)
	{
		switch (tabPlacement)
		{
			case JTabbedPane.TOP:
				return topInsets;
			case JTabbedPane.BOTTOM:
				return bottomInsets;
			case JTabbedPane.LEFT:
				return leftInsets;
			case JTabbedPane.RIGHT:
				return rightInsets;
			default:
				return defaultInsets;
		}
	}

	public static TabbedPaneUI getBorderLessUI()
	{
		LookAndFeel lnf = UIManager.getLookAndFeel();
		String lnfClass = lnf.getClass().getName();
		if (lnfClass.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
		{
			return getClassInstance("workbench.gui.components.BorderLessMetalTabbedPaneUI");
		}
		else if (lnfClass.equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel") ||
		         lnfClass.equals("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"))
		{
			return getClassInstance("workbench.gui.components.BorderLessWindowsTabbedPaneUI");
		}
		else if (lnfClass.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
		{
			return getClassInstance("workbench.gui.components.BorderLessMotifTabbedPaneUI");
		}
		else
		{
			TabbedPaneUI uiInstance = null;
			String uiClass = (String)UIManager.getDefaults().get("TabbedPaneUI");
			try
			{
				Class ui = Class.forName(uiClass);
				uiInstance = (TabbedPaneUI)ui.newInstance();
			}
			catch (Throwable e)
			{
				JTabbedPane pane = new JTabbedPane();
				uiInstance = (TabbedPaneUI)UIManager.getUI(pane);
			}
			return uiInstance;
		}
	}

	private static TabbedPaneUI getClassInstance(String className)
	{
		TabbedPaneUI ui = null;
		try
		{
			Class cls = Class.forName(className);
			ui = (TabbedPaneUI)cls.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			JTabbedPane pane = new JTabbedPane();
			ui = (TabbedPaneUI)UIManager.getUI(pane);
		}
		return ui;
	}
}