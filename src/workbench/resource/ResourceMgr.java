/*
 * ResourceMgr.java
 *
 * Created on November 25, 2001, 4:47 PM
 */

package workbench.resource;

import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.InputStream;
import java.util.HashMap;
import javax.swing.ImageIcon;
import workbench.log.LogMgr;


/**
 *	
 * @author  thomas.kellerer@inline-skate.com
 * @version
 */
public class ResourceMgr
{
	private static ResourceMgr theInstance = new ResourceMgr();
	private static ClassLoader loader = theInstance.getClass().getClassLoader();
	
	public static final String ERROR_DISCONNECT = "ErrorOnDiscconect";
	public static final String ERROR_NO_CONNECTION_AVAIL = "ErrorNoConnectionAvailable";
	
	public static final String TAB_LABEL_RESULT = "Result";
	public static final String TAB_LABEL_MSG = "Messages";
	
	public static final String TXT_PRODUCT_NAME = "SQL Workbench/J";
	public static final String TXT_ERROR_MSG_DATA = "ErrorMessageData";
	public static final String TXT_ERROR_MSG_TITLE = "ErrorMessageTitle";
	
	// Messages from the ResultPanel
	public static final String TXT_WARN_NO_RESULT = "WarningNoResultSet";
	public static final String TXT_SQL_EXCUTE_OK = "StatementOK";
	public static final String TXT_ROWS_AFFECTED = "RowsAffected";
	public static final String TXT_EXECUTING_SQL = "ExecutingSql";
	
	public static final String TXT_COPY = "Copy";
	public static final String TXT_CUT = "Cut";
	public static final String TXT_PASTE = "Paste";
	public static final String TXT_CLEAR = "Clear";
	public static final String TXT_SELECTALL = "SelectAll";
	
	public static final String TXT_EXECUTE_SEL = "ExecuteSel";
	public static final String TXT_EXECUTE_ALL = "ExecuteAll";
	public static final String TXT_STOP_STMT = "StopStmt";
	
	public static final String TXT_DB_DRIVER = "Driver";
	public static final String TXT_DB_USERNAME = "Username";
	public static final String TXT_DB_URL = "DbURL";
	public static final String TXT_DB_PASSWORD = "Password";
	
	public static final String TXT_SELECT_PROFILE = "SelectProfile";
	public static final String TXT_SAVE_PROFILE = "SaveProfile";
	
	public static final String IMG_COPY_16 = "Copy16";
	public static final String IMG_COPY_24 = "Copy24";
	public static final String IMG_CUT_16 = "Cut16";
	public static final String IMG_CUT_24 = "Cut24";
	public static final String IMG_PASTE_16 = "Paste16";
	public static final String IMG_PASTE_24 = "Paste24";
	public static final String IMG_EXEC_SEL_16 = "ExecuteSel16";
	public static final String IMG_EXEC_SEL_24 = "ExecuteSel24";
	public static final String IMG_EXEC_ALL_16 = "ExecuteAll16";
	public static final String IMG_EXEC_ALL_24 = "ExecuteAll24";
	public static final String IMG_STOP_16 = "Stop16.gif";
	public static final String IMG_STOP_24 = "Stop24.gif";
	
	public static final String MNU_TXT_FILE = "File";
	public static final String MNU_TXT_SQL = "SQL";
	public static final String MNU_TXT_EDIT = "Edit";
	public static final String MNU_TXT_DATA = "Data";
	
	public static final String MNU_TXT_CONNECT = "Connect";
	public static final String MNU_TXT_EXIT = "Exit";
	
	public static final String TXT_OK = "OK";
	public static final String TXT_CANCEL = "Cancel";
	
	public static final String STAT_READY = "Ready";
	
	public static final String ERR_DRIVER_NOT_FOUND = "ErrDriverNotFound";
	public static final String ERR_CONNECTION_ERROR = "ErrConnectionError";
	
	private static ResourceBundle resources = ResourceBundle.getBundle("workbench/resource/wbstrings");
	private static HashMap images = new HashMap();

	
	private ResourceMgr()
	{
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
	
	/**
	 *	Returns the description associcate with the given key.
	 *	This is used for Tooltips which are associated with a 
	 *	certain menu text etc.
	 */
	public static String getDescription(String aKey)
	{
		return getString("Desc_" + aKey);
	}
	
	public static InputStream getDefaultSettings()
	{
		InputStream in = theInstance.getClass().getResourceAsStream("guidefaults.properties");
		return in;
	}
	
	public static ImageIcon getImage(String aKey)
	{
		Object value = images.get(aKey.toUpperCase());
		ImageIcon result = null;
		
		if (value == null)
		{
			java.net.URL imageIconUrl = loader.getResource("workbench/resource/images/" + aKey+ ".gif");
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
