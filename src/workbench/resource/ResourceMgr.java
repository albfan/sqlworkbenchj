/*
 * ResourceMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

import workbench.log.LogMgr;
import workbench.util.StringUtil;
import workbench.util.VersionNumber;

/**
 * @author support@sql-workbench.net.kellerer
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

	public static String getDefaultTabLabel()
	{
		return getString("LblTabStatement");
	}

	public static String getFormattedString(String key, Object ... values)
	{
		return MessageFormat.format(getString(key), values);
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
		return retrieveImage("blank16", ".gif");
	}

	/**
	 * Retrieves a 16x16 GIF image
	 */
	public static ImageIcon getImage(String aKey)
	{
		return retrieveImage(aKey + "16", ".gif");
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
		return retrieveImage(aName, ".gif");
	}

	/**
	 * Retrieves a PNG image with no size specified
	 */
	public static ImageIcon getPng(String aName)
	{
		return retrieveImage(aName, ".png");
	}

	private static ImageIcon retrieveImage(String filename, String extension)
	{
		return retrieveImage(filename + extension);
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
			boolean setDefaultLocale = Settings.getInstance().getBoolProperty("workbench.gui.setdefaultlocale", true);
			if (setDefaultLocale)
			{
				Locale.setDefault(l);
			}
		}
		return resources;
  }

}
