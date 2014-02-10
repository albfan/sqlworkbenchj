/*
 * ResourceMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
 *//*
 * ResourceMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.resource;

import java.awt.Image;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

import workbench.log.LogMgr;

import workbench.util.StringUtil;
import workbench.util.VersionNumber;

/**
 * A class to manage the ResourceBundle for the localization.
 *
 * @author Thomas Kellerer.kellerer
 */
public class ResourceMgr
{
	public static final String TXT_PRODUCT_NAME = "SQL Workbench/J";

	public static final String TXT_OK = "LblOK";
	public static final String TXT_CANCEL = "LblCancel";

	public static final String IMG_SAVE = "Save";

	public static final String MNU_TXT_WORKSPACE = "MnuTxtWorkspace";
	public static final String MNU_TXT_FILE = "MnuTxtFile";
	public static final String MNU_TXT_MACRO = "MnuTxtMacro";
	public static final String MNU_TXT_SQL = "MnuTxtSQL";
	public static final String MNU_TXT_EDIT = "MnuTxtEdit";
	public static final String MNU_TXT_DATA = "MnuTxtData";
	public static final String MNU_TXT_COPY_SELECTED = "MnuTxtCopySelected";

	public static final String MNU_TXT_VIEW = "MnuTxtView";
	public static final String MNU_TXT_TOOLS = "MnuTxtTools";
	public static final String MNU_TXT_HELP = "MnuTxtHelp";
	public static final String MNU_TXT_OPTIONS = "MnuTxtOptions";

	private static ResourceBundle resources;
	private static final String PROP_CHANGE_LOCALE = "workbench.gui.setdefaultlocale";
	private static boolean useLargeIcons;

	private ResourceMgr()
	{
	}

	public static String getBuildInfo()
	{
		return getString("TxtBuild") + " " + getBuildNumber().toString() + " (" + getString("TxtBuildDate") + ")";
	}

	public static String replaceModifierText(String msg)
	{
		msg = StringUtil.replace(msg, "%shift%", KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK));
		msg = StringUtil.replace(msg, "%control%", KeyEvent.getKeyModifiersText(PlatformShortcuts.getDefaultModifier()));
		return msg;
	}

	public static java.util.Date getBuildDate()
	{
		String builddate = getString("TxtBuildDate");
		// running from the dev environment --> build date is now!
		if ("@BUILD_DATE@".equals(builddate)) return new java.util.Date();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		java.util.Date result = null;
		try
		{
			result = format.parse(builddate);
		}
		catch (Exception e)
		{
			LogMgr.logError("ResourceMgr.getBuildDate()", "Err when parsing build date!", e);
			result = new java.util.Date();
		}
		return result;
	}

	public static boolean isDevBuild()
	{
		String nr = getString("TxtBuildNumber");
		char c = nr.charAt(0);
		return (c == '[' || c == '@');
	}

	public static VersionNumber getBuildNumber()
	{
		String nr = getString("TxtBuildNumber");
		return new VersionNumber(nr);
	}

	public static void setWindowIcons(Window window, List<File> iconFiles)
	{
		if (iconFiles == null) return;
		try
		{
			List<Image> icons = new ArrayList<Image>(iconFiles.size());
			for (File f : iconFiles)
			{
				URL url = f.toURI().toURL();
				ImageIcon img = new ImageIcon(url);
				icons.add(img.getImage());
			}
			window.setIconImages(icons);
		}
		catch (Throwable ex)
		{
			LogMgr.logError("ResourceMgr.setWindowIcons()", "Could not set icons!", ex);
			setWindowIcons(window, "workbench");
		}
	}

	public static void setWindowIcons(Window window, String baseName)
	{
		List<Image> icons = new ArrayList<Image>(2);
		ImageIcon image16 = retrieveImage(baseName + "16.png");
		if (image16 != null)
		{
			icons.add(image16.getImage());
		}
		ImageIcon image32 = retrieveImage(baseName + "32.png");
		if (image32 != null)
		{
			icons.add(image32.getImage());
		}
		if (icons.size() > 0)
		{
			window.setIconImages(icons);
		}
	}

	public static String getDefaultTabLabel()
	{
		return Settings.getInstance().getProperty("workbench.gui.tabs.defaultlabel", getString("LblTabStatement"));
	}

	public static String getFormattedString(String key, Object ... values)
	{
		return MessageFormat.format(getString(key), values);
	}

	public static String getDynamicString(String baseKey, String option, String dbid)
	{
		if (baseKey == null || option == null) return null;

		String key = null;
		if (dbid != null)
		{
			key = baseKey + "." + dbid + "." + option.toLowerCase();
			if (!getResources().containsKey(key))
			{
				key = baseKey + "." + option.toLowerCase();
			}
		}
		else
		{
			key = baseKey + "." + option.toLowerCase();
		}

		if (getResources().containsKey(key))
		{
			return getResources().getString(key);
		}
		return null;
	}

	public static String getString(String aKey)
	{
		return getString(aKey, false);
	}

	public static String getString(String aKey, boolean replaceModifiers)
	{
		try
		{
			String value = getResources().getString(aKey);
			if (replaceModifiers)
			{
				return replaceModifierText(value);
			}
			return value;
		}
		catch (MissingResourceException e)
		{
			LogMgr.logWarning("ResourceMgr", "String with key=" + aKey + " not found in resource file!", e);
			return aKey;
		}
	}

	/**
	 * Returns the resource string for the given key with
	 * all occurances of &amp; removed.
	 * @param aKey
	 */
	public static String getPlainString(String aKey)
	{
		String value = getString(aKey).replace("&", "");
		return value;
	}

	/**
	 * Returns the description (tooltip) for the specified resource key.
	 * The description is retrieved from the resourcebundle by addind a d_ to the
	 * passed key.
	 *
	 * @param aKey
	 * @see #getString(java.lang.String)
	 */
	public static String getDescription(String aKey)
	{
		return getDescription(aKey, false);
	}

	/**
	 *    Returns the description associcate with the given key.
	 *    This is used for Tooltips which are associated with a
	 *    certain menu text etc.
	 */
	public static String getDescription(String aKey, boolean replaceModifiers)
	{
		String value = getString("d_" + aKey);
		if (replaceModifiers)
		{
			value = replaceModifierText(value);
		}
		return value;
	}

	public static InputStream getDefaultSettings()
	{
		InputStream in = ResourceMgr.class.getResourceAsStream("default.properties");

		return in;
	}

	/**
	 * Returns an empty 16x16 gif image
	 */
	public static ImageIcon getBlankImage()
	{
		return retrieveImage("blank16.gif");
	}

	public static ImageIcon getActionIcon(String baseKey, boolean isPng)
	{
		if (isPng)
		{
			return retrieveImage(baseKey + (useLargeIcons ? "32.png" : "16.png"));
		}
		else
		{
			return retrieveImage(baseKey + (useLargeIcons ? "32.gif" : "16.gif"));
		}
	}

	/**
	 * Retrieves a GIF Image.
	 */
	public static ImageIcon getGifIcon(String aKey)
	{
		return retrieveImage(aKey + (useLargeIcons ? "32.gif" : "16.gif"));
	}

	/**
	 * Retrieves an image specified by the full name
	 */
	public static ImageIcon getImageByName(String fname)
	{
		return retrieveImage(fname);
	}

	/**
	 * Retrieves a GIF image with no size specified
	 */
	public static ImageIcon getPicture(String aName)
	{
		return retrieveImage(aName + ".gif");
	}

	public static ImageIcon getPngIcon(String aName)
	{
		return retrieveImage(aName + (useLargeIcons ? "32.png" : "16.png"));
	}

	/**
	 * Retrieves a PNG image with no size specified
	 * @param aName the base name of the icon
	 * @return the ImageIcon
	 */
	public static ImageIcon getPng(String aName)
	{
		return retrieveImage(aName + ".png");
	}

	private static ImageIcon retrieveImage(String fname)
	{
		ImageIcon result = null;
		URL imageIconUrl = ResourceMgr.class.getClassLoader().getResource("workbench/resource/images/" + fname);
		if (imageIconUrl != null)
		{
			result = new ImageIcon(imageIconUrl);
		}
		else
		{
			imageIconUrl = ResourceMgr.class.getClassLoader().getResource(fname);
			if (imageIconUrl != null)
			{
				result = new ImageIcon(imageIconUrl);
			}
		}
		return result;
	}

	/**
	 * For testing purposes
	 */
	static ResourceBundle getResourceBundle(Locale l)
	{
		return ResourceBundle.getBundle("language/wbstrings", l);
	}

	public static ResourceBundle getResources()
	{
		if (resources == null)
		{
			Locale l = Settings.getInstance().getLanguage();
			resources = getResourceBundle(l);

			boolean setDefaultLocale = Settings.getInstance().getBoolProperty(PROP_CHANGE_LOCALE, true);
			if (setDefaultLocale)
			{
				LogMgr.logInfo("ResourceMgr.getResources()", "Setting default locale to: " + l.toString());
				Locale.setDefault(l);
			}
			else
			{
				Locale def = Locale.getDefault();
				LogMgr.logInfo("ResourceMgr.getResources()", "Default locale is : " + def.toString());
			}
		}
		return resources;
  }

}
