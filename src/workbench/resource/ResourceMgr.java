/*
 * ResourceMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.awt.event.KeyEvent;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

import workbench.log.LogMgr;
import workbench.util.StringUtil;

/**
 * @author  thomas.kellerer
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

	private static ResourceBundle resources = ResourceBundle.getBundle("language/wbstrings");
	private static HashMap images = new HashMap();

	private static String BUILD_INFO;

	private ResourceMgr()
	{
	}

	public static String getBuildInfo()
	{
		if (BUILD_INFO == null)
		{
			BUILD_INFO = getString("TxtBuild") + " " + getString("TxtBuildNumber") + " (" + getString("TxtBuildDate") + ")";
		}
		return BUILD_INFO;
	}

	private static final String shiftText = KeyEvent.getKeyModifiersText(KeyEvent.SHIFT_MASK);
	private static final String ctrlText = KeyEvent.getKeyModifiersText(KeyEvent.CTRL_MASK);
	
	public static String replaceModifierText(String msg)
	{
		msg = StringUtil.replace(msg, "%shift%", shiftText);
		msg = StringUtil.replace(msg, "%control%", ctrlText);
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

	public static String getBuildId()
	{
		String nr = getString("TxtBuildNumber");
		if ("@BUILD_NUMBER@".equals(nr)) return "Dev";
		return nr;
	}
	public static int getBuildNumber()
	{
		String nr = getString("TxtBuildNumber");
		if ("@BUILD_NUMBER@".equals(nr)) return Integer.MAX_VALUE;
		if (nr.startsWith("[")) return -1;

		int result = -1;

		try
		{
			if (nr.indexOf('.') > -1)
			{
				nr = nr.substring(0,nr.indexOf('.'));
			}
			result = Integer.parseInt(nr);
		}
		catch (Exception e)
		{
			result = -1;
		}
		return result;
	}

	public static String getDefaultTabLabel()
	{
		return getString("LblTabStatement");
	}
	
	public static String getString(String aKey)
	{
		try
		{
			String value = resources.getString(aKey);

			return value;
		}
		catch (MissingResourceException e)
		{
			LogMgr.logWarning("ResourceMgr", "String with key=" + aKey + " not found in resource file!", e);
			return aKey;
		}
	}
	
	public static String getPlainString(String aKey)
	{
		String value = getString(aKey).replaceAll("\\&", "");
		return value;
	}

	public static String getAcceleratorChar(String aKey)
	{
		try
		{
			String label = getString(aKey);
			int pos = label.indexOf('&');
			if (pos == -1) return null;
			
			char c = label.charAt(pos + 1);
			StringBuffer b = new StringBuffer(1);
			b.append(c);
			return b.toString();
		}
		catch (MissingResourceException e)
		{
			return null;
		}
		catch (Exception e)
		{
			return  null;
		}
	}
	
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

	public static ImageIcon getLargeImage(String aKey)
	{
		return retrieveImage(aKey + "24");
	}

	public static ImageIcon getBlankImage()
	{
		return retrieveImage("blank16");
	}
	
	public static ImageIcon getImage(String aKey)
	{
		return retrieveImage(aKey + "16");
	}

	public static ImageIcon getPicture(String aName)
	{
		return retrieveImage(aName);
	}

	private static ImageIcon retrieveImage(String aKey)
	{
		Object    value = images.get(aKey.toUpperCase());
		ImageIcon result = null;
		if (value == null)
		{
			URL imageIconUrl = ResourceMgr.class.getClassLoader().getResource("workbench/resource/images/" + aKey + ".gif");
			if (imageIconUrl != null)
			{
				result = new ImageIcon(imageIconUrl);
				images.put(aKey.toUpperCase(), result);

				return result;
			}
		}
		else
		{
			result = (ImageIcon)value;
		}

		return result;
	}

}
