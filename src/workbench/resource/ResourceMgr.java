/*
 * ResourceMgr.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.ImageIcon;

import workbench.log.LogMgr;

/**
 * @author  thomas.kellerer
 */
public class ResourceMgr
{
	public static final String ERROR_DISCONNECT = "ErrorOnDisconnect";
	public static final String ERROR_NO_CONNECTION_AVAIL = "ErrorNoConnectionAvailable";
	public static final String TAB_LABEL_RESULT = "LabelTabResult";
	public static final String TAB_LABEL_MSG = "LabelTabMessages";
	public static final String TXT_PRODUCT_NAME = "SQL Workbench/J";
	public static final String TXT_ERROR_MSG_DATA = "ErrorMessageData";
	public static final String TXT_ERROR_MSG_TITLE = "ErrorMessageTitle";

	public static final String TXT_COPY = "MnuTxtCopy";
	public static final String TXT_CUT = "MnuTxtCut";
	public static final String TXT_PASTE = "MnuTxtPaste";
	public static final String TXT_CLEAR = "MnuTxtClear";
	public static final String TXT_SELECTALL = "MnuTxtSelectAll";
	public static final String TXT_EXECUTE_SEL = "MnuTxtExecuteSel";
	public static final String TXT_EXECUTE_ALL = "MnuTxtExecuteAll";
	public static final String TXT_STOP_STMT = "MnuTxtStopStmt";
	public static final String TXT_DB_DRIVER = "LabelDriver";
	public static final String TXT_DB_USERNAME = "LabelUsername";
	public static final String TXT_DB_URL = "LabelDbURL";
	public static final String TXT_DB_PASSWORD = "LabelPassword";
	public static final String TXT_SELECT_PROFILE = "LabelSelectProfile";
	public static final String TXT_SAVE_PROFILE = "LabelSaveProfile";
	public static final String TXT_SAVE = "LabelSave";
	public static final String TXT_OK = "LabelOK";
	public static final String TXT_CANCEL = "LabelCancel";

	public static final String IMG_COPY = "Copy";
	public static final String IMG_CUT = "Cut";
	public static final String IMG_PASTE = "Paste";
	public static final String IMG_EXEC_SEL = "ExecuteSel";
	public static final String IMG_EXEC_ALL = "ExecuteAll";
	public static final String IMG_STOP = "Stop";
	public static final String IMG_SAVE = "Save";
	public static final String IMG_SAVE_AS = "SaveAs";
	public static final String IMG_NEW = "New";
	public static final String IMG_DELETE = "Delete";
	public static final String IMG_UP = "Up";
	public static final String IMG_DOWN = "Down";
	public static final String IMG_FIND = "Find";

	public static final String MNU_TXT_WORKSPACE = "MnuTxtWorkspace";
	public static final String MNU_TXT_FILE = "MnuTxtFile";
	public static final String MNU_TXT_MACRO = "MnuTxtMacro";
	public static final String MNU_TXT_SQL = "MnuTxtSQL";
	public static final String MNU_TXT_EDIT = "MnuTxtEdit";
	public static final String MNU_TXT_DATA = "MnuTxtData";
	public static final String MNU_TXT_COPY_SELECTED = "MnuTxtCopySelected";
		
	public static final String MNU_TXT_CONNECT = "MnuTxtConnect";
	public static final String MNU_TXT_EXIT = "MnuTxtExit";
	public static final String MNU_TXT_VIEW = "MnuTxtView";
	public static final String MNU_TXT_TOOLS = "MnuTxtTools";
	public static final String MNU_TXT_HELP = "MnuTxtHelp";
	public static final String MNU_TXT_OPTIONS = "MnuTxtOptions";

	public static final String MSG_EXEC_SQL = "MsgExecutingSql";
	public static final String MSG_WARN_NO_RESULT = "MsgWarningNoResultSet";
	public static final String MSG_SQL_EXCUTE_OK = "MsgStatementOK";
	public static final String MSG_ROWS_AFFECTED = "MsgRowsAffected";

	public static final String STAT_READY = "MsgReady";
	public static final String ERR_DRIVER_NOT_FOUND = "ErrorDriverNotFound";
	public static final String ERR_CONNECTION_ERROR = "ErrorConnectionError";

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
			LogMgr.logError("ResourceMgr.getBuildDate()", "Error when parsing build date!", e);
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

	public static int getBuildNumber()
	{
		String nr = getString("TxtBuildNumber");
		if ("@BUILD_NUMBER@".equals(nr)) return Integer.MAX_VALUE;
		if (nr.startsWith("[")) return -1;

		int result = -1;

		try
		{
			result = Integer.parseInt(nr);
		}
		catch (Exception e)
		{
			result = -1;
		}
		return result;
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
			LogMgr.logWarning("ResourceMgr", "String with key=" + aKey + " not found in resource file!");

			return aKey;
		}
	}

	/**
	 *    Returns the description associcate with the given key.
	 *    This is used for Tooltips which are associated with a
	 *    certain menu text etc.
	 */
	public static String getDescription(String aKey)
	{
		try
		{
			String value = resources.getString("Desc_" + aKey);
			return value;
		}
		catch (MissingResourceException e)
		{
			LogMgr.logDebug("ResourceMgr", "No description for key=" + aKey + " found in resource file!", e);
			return "";
		}
	}

	public static InputStream getDefaultSettings()
	{
		InputStream in = ResourceMgr.class.getResourceAsStream("guidefaults.properties");

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

	public static void main(String args[])
	{
		InputStream in = null;
		try
		{
			in = ClassLoader.getSystemResourceAsStream("META-INF/test.txt");
			BufferedReader b = new BufferedReader(new InputStreamReader(in));
			String line = b.readLine();
			while (line != null)
			{
				System.out.println("Line: " + line);
				line = b.readLine();
			}
			/*
			Manifest mf = new Manifest(in);
			Attributes attr = mf.getMainAttributes();
			Iterator itr = attr.keySet().iterator();
			while (itr.hasNext()) System.out.println("key=" + itr.next());
			String build = attr.getValue("WbBuild-Date");
			System.out.println("Build-date" + build);
			 **/
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { in.close(); } catch (Exception ignore) {}
		}
	}

}
