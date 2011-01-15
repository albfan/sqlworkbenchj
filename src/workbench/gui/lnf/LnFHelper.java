/*
 * LnFHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.lnf;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * Initialize some gui elements during startup.
 *
 * @author Thomas Kellerer
 */
public class LnFHelper
{
	private boolean isWindowsClassic;

	public boolean isWindowsClassic()
	{
		return isWindowsClassic;
	}

	public void initUI()
	{
		initializeLookAndFeel();

		Settings settings = Settings.getInstance();
		UIDefaults def = UIManager.getDefaults();

		Font stdFont = settings.getStandardFont();
		if (stdFont != null)
		{
			def.put("Button.font", stdFont);
			def.put("CheckBox.font", stdFont);
			def.put("CheckBoxMenuItem.font", stdFont);
			def.put("ColorChooser.font", stdFont);
			def.put("ComboBox.font", stdFont);
			def.put("EditorPane.font", stdFont);
			def.put("FileChooser.font", stdFont);
			def.put("Label.font", stdFont);
			def.put("List.font", stdFont);
			def.put("Menu.font", stdFont);
			def.put("MenuBar.font", stdFont);
			def.put("MenuItem.font", stdFont);
			def.put("OptionPane.font", stdFont);
			def.put("Panel.font", stdFont);
			def.put("PasswordField.font", stdFont);
			def.put("PopupMenu.font", stdFont);
			def.put("ProgressBar.font", stdFont);
			def.put("RadioButton.font", stdFont);
			def.put("RadioButtonMenuItem.font", stdFont);
			def.put("TabbedPane.font", stdFont);
			def.put("TextArea.font", stdFont);
			def.put("TextField.font", stdFont);
			def.put("TextPane.font", stdFont);
			def.put("TitledBorder.font", stdFont);
			def.put("ToggleButton.font", stdFont);
			def.put("ToolBar.font", stdFont);
			def.put("ToolTip.font", stdFont);
			def.put("Tree.font", stdFont);
			def.put("ViewPort.font", stdFont);
		}

		Font dataFont = settings.getDataFont();
		if (dataFont != null)
		{
			def.put("Table.font", dataFont);
			def.put("TableHeader.font", dataFont);
		}

			// use our own classes for some GUI elements
//		if (!"Nimbus".equals(UIManager.getLookAndFeel().getName()))
//		{
//			def.put("SplitPaneUI", "workbench.gui.components.WbSplitPaneUI");
//		}
//
		String cls = TabbedPaneUIFactory.getTabbedPaneUIClass();
		if (cls != null) def.put("TabbedPaneUI", cls);

		if (settings.getBoolProperty("workbench.gui.adjustgridcolor", true))
		{
			Color c = settings.getColor("workbench.table.gridcolor", new Color(215,215,215));
			def.put("Table.gridColor", c);
		}
	}

	public static boolean isJGoodies()
	{
		String lnf = UIManager.getLookAndFeel().getClass().getName();
		return lnf.startsWith("com.jgoodies.looks.plastic");
	}

	protected void initializeLookAndFeel()
	{
		String className = GuiSettings.getLookAndFeelClass();
		try
		{
			if (StringUtil.isEmptyString(className))
			{
				className = UIManager.getSystemLookAndFeelClassName();
			}
			LnFManager mgr = new LnFManager();
			LnFDefinition def = mgr.findLookAndFeel(className);

			if (def == null)
			{
				LogMgr.logError("LnFHelper.initializeLookAndFeel()", "Specified Look & Feel " + className + " not available!", null);
				setSystemLnF();
			}
			else
			{
				// JGoodies Looks settings
				UIManager.put("jgoodies.useNarrowButtons", Boolean.FALSE);
				UIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);

				// I hate the bold menu font in the Metal LnF
				UIManager.put("swing.boldMetal", Boolean.FALSE);

				// Remove Synthetica's own window decorations
				UIManager.put("Synthetica.window.decoration", Boolean.FALSE);

				// Remove the extra icons for read only text fields and
				// the "search bar" in the main menu for the Substance Look & Feel
				System.setProperty("substancelaf.noExtraElements", "");

				LnFLoader loader = new LnFLoader(def);
				LookAndFeel lnf = loader.getLookAndFeel();

				UIManager.setLookAndFeel(lnf);
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("LnFHelper.initializeLookAndFeel()", "Could not set look and feel to [" + className + "]. Look and feel will be ignored", e);
			setSystemLnF();
		}

		checkWindowsClassic(UIManager.getLookAndFeel().getClass().getName());
	}

	private void setSystemLnF()
	{
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception ex)
		{
			// should not ahppen
		}
	}
	
	private void checkWindowsClassic(String clsname)
	{
		try
		{
			if (clsname.indexOf("com.sun.java.swing.plaf.windows") > -1)
			{
				String osVersion = System.getProperty("os.version", "1.0");
				Float version = Float.valueOf(osVersion);
				if (version.floatValue() <= 5.0)
				{
					isWindowsClassic = true;
				}
				else
				{
					isWindowsClassic = (clsname.indexOf("WindowsClassicLookAndFeel") > -1);
					if (!isWindowsClassic)
					{
						Toolkit toolkit = Toolkit.getDefaultToolkit();
						Boolean themeActive = (Boolean) toolkit.getDesktopProperty("win.xpstyle.themeActive");
						if (themeActive != null)
						{
							isWindowsClassic = !themeActive.booleanValue();
						}
						else
						{
							isWindowsClassic = true;
						}
					}
				}
			}
		}
		catch (Throwable e)
		{
			isWindowsClassic = false;
		}

	}

}
