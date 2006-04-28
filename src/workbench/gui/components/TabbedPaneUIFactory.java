/*
 * TabbedPaneUIFactory.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
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
 * @author  support@sql-workbench.net
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

	public static String getTabbedPaneUIClass()
	{
		LookAndFeel lnf = UIManager.getLookAndFeel();
		String lnfClass = lnf.getClass().getName();
		if (lnfClass.equals("javax.swing.plaf.metal.MetalLookAndFeel"))
		{
			return "workbench.gui.components.BorderLessMetalTabbedPaneUI";
		}
		else if (lnfClass.startsWith("com.sun.java.swing.plaf.windows.Windows"))
		{
			return "workbench.gui.components.BorderLessWindowsTabbedPaneUI";
		}
		else if (lnfClass.equals("com.sun.java.swing.plaf.motif.MotifLookAndFeel"))
		{
			return "workbench.gui.components.BorderLessMotifTabbedPaneUI";
		}
		else if (lnfClass.startsWith("com.jgoodies.looks.plastic.Plastic"))
		{
			if ("Metal".equals(System.getProperty("Plastic.tabStyle")))
			{
				return "workbench.gui.components.BorderLessMetalTabbedPaneUI";
			}
		}
		return null;// UIManager.getDefaults().getString("TabbedPaneUI");
	}
	
	public static TabbedPaneUI getBorderLessUI()
	{
		String uiClass = getTabbedPaneUIClass();
		if (uiClass == null) return null;
		return getClassInstance(uiClass);
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
			JTabbedPane pane = new JTabbedPane();
			ui = (TabbedPaneUI)UIManager.getUI(pane);
			UIManager.getDefaults().put("TabbedPaneUI", ui.getClass().getName());
		}
		return ui;
	}
}
