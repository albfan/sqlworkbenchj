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


/**
 *
 * @author  thomas
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
	public static final String TXT_SQL_EXCUTE_OK = "StatementOK";
	
	public static final String TXT_COPY = "Copy";
	public static final String TXT_CUT = "Cut";
	public static final String TXT_PASTE = "Paste";
	public static final String TXT_CLEAR = "Clear";
	public static final String TXT_SELECTALL = "SelectAll";
	
	public static final String TXT_EXECUTE_SEL = "ExecuteSel";
	public static final String TXT_EXECUTE_ALL = "ExecuteAll";
	
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
			return aKey;
		}
	}
	
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
