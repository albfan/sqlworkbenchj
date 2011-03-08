/*
 * LnFHelper.java
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Set;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import workbench.gui.components.TabbedPaneUIFactory;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * Initialize some gui elements during startup.
 *
 * @author Thomas Kellerer
 */
public class LnFHelper
{
	private boolean isWindowsClassic;

	private final Set<String> fontProperties = CollectionUtil.treeSet(
		"Button.font",
		"CheckBox.font",
		"CheckBoxMenuItem.font",
		"ColorChooser.font",
		"ComboBox.font",
		"EditorPane.font",
		"FileChooser.font",
		"Label.font",
		"List.font",
		"Menu.font",
		"MenuBar.font",
		"MenuItem.font",
		"OptionPane.font",
		"Panel.font",
		"PasswordField.font",
		"PopupMenu.font",
		"ProgressBar.font",
		"RadioButton.font",
		"RadioButtonMenuItem.font",
		"TabbedPane.font",
		"TextArea.font",
		"TextField.font",
		"TextPane.font",
		"TitledBorder.font",
		"ToggleButton.font",
		"ToolBar.font",
		"ToolTip.font",
		"Tree.font",
		"ViewPort.font");

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
			for (String property : fontProperties)
			{
				def.put(property, stdFont);
			}
		}
		else if (isWindowsLookAndFeel())
		{
			// The default Windows look and feel does not scale the fonts properly
			scaleDefaultFonts();
		}

		Font dataFont = settings.getDataFont();
		if (dataFont != null)
		{
			def.put("Table.font", dataFont);
			def.put("TableHeader.font", dataFont);
		}

		String cls = TabbedPaneUIFactory.getTabbedPaneUIClass();
		if (cls != null) def.put("TabbedPaneUI", cls);

		if (settings.getBoolProperty("workbench.gui.adjustgridcolor", true))
		{
			Color c = settings.getColor("workbench.table.gridcolor", new Color(215,215,215));
			def.put("Table.gridColor", c);
		}
	}

	private boolean isWindowsLookAndFeel()
	{
		String lnf = UIManager.getLookAndFeel().getClass().getName();
		return lnf.indexOf("plaf.windows") > -1;
	}

	private void scaleDefaultFonts()
	{
		if (!Settings.getInstance().getScaleFonts()) return;
		UIDefaults def = UIManager.getDefaults();
		FontScaler scaler = new FontScaler();
		for (String property : fontProperties)
		{
			Font base = def.getFont(property);
			if (base != null)
			{
				Font scaled = scaler.scaleFont(base);
				def.put(property, scaled);
			}
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
