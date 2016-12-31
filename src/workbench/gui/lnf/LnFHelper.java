/*
 * LnFHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.Set;

import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.Settings;

import workbench.gui.components.TabbedPaneUIFactory;

import workbench.util.CollectionUtil;
import workbench.util.PlatformHelper;
import workbench.util.StringUtil;


/**
 * Initialize some gui elements during startup.
 *
 * @author Thomas Kellerer
 */
public class LnFHelper
{
	public static final String MENU_FONT_KEY = "MenuItem.font";
	public static final String LABEL_FONT_KEY = "Label.font";
	public static final String TREE_FONT_KEY = "Tree.font";

	private boolean isWindowsClassic;

	// Font properties that are automatically scaled by Java
	private final Set<String> noScale = CollectionUtil.treeSet(
		"Menu.font",
		"MenuBar.font",
		"MenuItem.font",
		"PopupMenu.font",
		"CheckBoxMenuItem.font");

	private final Set<String> fontProperties = CollectionUtil.treeSet(
		"Button.font",
		"CheckBox.font",
		"CheckBoxMenuItem.font",
		"ColorChooser.font",
		"ComboBox.font",
		"EditorPane.font",
		"FileChooser.font",
		LABEL_FONT_KEY,
		"List.font",
		"Menu.font",
		"MenuBar.font",
		MENU_FONT_KEY,
		"OptionPane.font",
		"Panel.font",
		"PasswordField.font",
		"PopupMenu.font",
		"ProgressBar.font",
		"RadioButton.font",
		"RadioButtonMenuItem.font",
		"ScrollPane.font",
		"Slider.font",
		"Spinner.font",
		"TabbedPane.font",
		"TextArea.font",
		"TextField.font",
		"TextPane.font",
		"TitledBorder.font",
		"ToggleButton.font",
		"ToolBar.font",
		"ToolTip.font",
		TREE_FONT_KEY,
		"ViewPort.font");

	public boolean isWindowsClassic()
	{
		return isWindowsClassic;
	}

	public static int getMenuFontHeight()
	{
		return getFontHeight(MENU_FONT_KEY);
	}

	public static int getLabelFontHeight()
	{
		return getFontHeight(LABEL_FONT_KEY);
	}

	private static int getFontHeight(String key)
	{
		UIDefaults def = UIManager.getDefaults();
		double factor = Toolkit.getDefaultToolkit().getScreenResolution() / 72.0;
		Font font = def.getFont(key);
		if (font == null) return 18;
		return (int)Math.ceil((double)font.getSize() * factor);
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

    def.put("Button.showMnemonics", Boolean.valueOf(GuiSettings.getShowMnemonics()));
    UIManager.put("Synthetica.extendedFileChooser.rememberLastDirectory", false);
	}

  public static boolean isGTKLookAndFeel()
  {
		String lnf = UIManager.getLookAndFeel().getClass().getName();
		return lnf.contains("GTKLookAndFeel");
  }
  
	public static boolean isWindowsLookAndFeel()
	{
		String lnf = UIManager.getLookAndFeel().getClass().getName();
		return lnf.contains("plaf.windows");
	}

  public static boolean isNonStandardLookAndFeel()
  {
    String lnf = UIManager.getLookAndFeel().getClass().getName();
    return (lnf.startsWith("com.sun.java") == false && lnf.startsWith("javax.swing.plaf") == false);
  }

	private void scaleDefaultFonts()
	{
		FontScaler scaler = new FontScaler();
		scaler.logSettings();
		if (!Settings.getInstance().getScaleFonts()) return;

		LogMgr.logInfo("LnFHelper.scaleDefaultFonts()", "Scaling default fonts by: " + scaler.getScaleFactor());

		UIDefaults def = UIManager.getDefaults();

    // when the user configures a scale factor, don't check the menu fonts
    boolean checkJavaFonts = Settings.getInstance().getScaleFactor() < 0;

		for (String property : fontProperties)
		{
      if (checkJavaFonts && noScale.contains(property)) continue;
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
				UIManager.put("FileChooser.useSystemIcons", Boolean.TRUE);

				// I hate the bold menu font in the Metal LnF
				UIManager.put("swing.boldMetal", Boolean.FALSE);

				// Remove Synthetica's own window decorations
				UIManager.put("Synthetica.window.decoration", Boolean.FALSE);

				// Remove the extra icons for read only text fields and
				// the "search bar" in the main menu for the Substance Look & Feel
				System.setProperty("substancelaf.noExtraElements", "");

        if (className.startsWith("org.jb2011.lnf.beautyeye"))
        {
          UIManager.put("RootPane.setupButtonVisible", false);
        }

				LnFLoader loader = new LnFLoader(def);
				LookAndFeel lnf = loader.getLookAndFeel();

				UIManager.setLookAndFeel(lnf);
				PlatformHelper.installGtkPopupBugWorkaround();
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
			if (clsname.contains("com.sun.java.swing.plaf.windows"))
			{
				String osVersion = System.getProperty("os.version", "1.0");
				Float version = Float.valueOf(osVersion);
				if (version <= 5.0)
				{
					isWindowsClassic = true;
				}
				else
				{
					isWindowsClassic = clsname.contains("WindowsClassicLookAndFeel");
					if (!isWindowsClassic)
					{
						Toolkit toolkit = Toolkit.getDefaultToolkit();
						Boolean themeActive = (Boolean) toolkit.getDesktopProperty("win.xpstyle.themeActive");
						if (themeActive != null)
						{
							isWindowsClassic = !themeActive;
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
