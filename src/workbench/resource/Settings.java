/*
 * Settings.java
 *
 * Created on December 1, 2001, 7:00 PM
 */
package workbench.resource;

import java.awt.Dimension;
import java.util.Properties;
import java.awt.Font;
import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import workbench.log.LogMgr;
import java.util.NoSuchElementException;
import javax.swing.JFrame;
import workbench.util.StringUtil;

/**
 *
 *	@author  thomas.kellerer@web.de
 */
public class Settings
{
	
	private Properties props;
	private Font standardFont;
	private Font editorFont;
	private Font msgLogFont;
	private Font dataFont;
	private String filename;
	
	public Settings()
	{
		this.props = new Properties();
		this.filename = System.getProperty("wb.settings.file", "workbench.settings");
		
		try
		{
			FileInputStream in = new FileInputStream(this.filename);
			this.props.load(in);
			in.close();
		}
		catch (IOException e)
		{
			LogMgr.logInfo(this, "Settings file '" + filename + "' not found! ");
			LogMgr.logInfo(this, "Using defaults");
			fillDefaults();
		}
	}
	
	public void saveSettings()
	{
		try
		{
			Object[] keys = this.props.keySet().toArray();
			Arrays.sort(keys);
			BufferedWriter bw = new BufferedWriter(new FileWriter(this.filename));
			String value = null;
			String lastKey = null;
			String key = null;
			for (int i=0; i < keys.length; i++)
			{
				key = (String)keys[i];
				
				if (lastKey != null)
				{
					if (!lastKey.substring(0, lastKey.indexOf('.')).equals(key.substring(0,key.indexOf('.'))))
					{
						bw.newLine();
					}
				}
				value = StringUtil.replace(this.props.get(key).toString(), "\\", "\\\\");
				bw.write(key + "=" + value);
				bw.newLine();
				lastKey = key;
			}
			bw.close();
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Error saving Settings file '" + filename + "'", e);
		}
	}
	
	private void fillDefaults()
	{
		try
		{
			this.props.load(ResourceMgr.getDefaultSettings());
		}
		catch (IOException e)
		{
			LogMgr.logError(this, "Could not read default settings", e);
		}
	}
	
	public Font getStandardFont()
	{
		if (this.standardFont == null)
		{
			this.standardFont = this.getFont("standard");
		}
		return this.standardFont;
	}
	
	public Font getEditorFont()
	{
		if (this.editorFont == null)
		{
			this.editorFont = this.getFont("editor");
			LogMgr.logInfo(this, "Using editor font=" + this.editorFont);
		}
		return editorFont;
	}
	
	public Font getMsgLogFont()
	{
		if (this.msgLogFont == null)
		{
			this.msgLogFont = this.getFont("msglog");
		}
		return this.msgLogFont;
	}
	
	public Font getDataFont()
	{
		if (this.dataFont == null)
		{
			this.dataFont = this.getFont("data");
		}
		return this.dataFont;
	}

	/**
	 *	Returns the font configured for this keyword
	 */
	private Font getFont(String aFontName)
	{
		Font result;
		
		String baseKey = new StringBuffer("wb.font.").append(aFontName).toString();
		String name = this.props.getProperty(baseKey + ".name", "Dialog");
		String sizeS = this.props.getProperty(baseKey + ".size", "11");
		String type = this.props.getProperty(baseKey + ".style", "Plain");
		int style = Font.PLAIN;
		int size = 11;
		if ("bold".equalsIgnoreCase(type)) style = Font.BOLD;
		else if ("italic".equalsIgnoreCase(type)) style = Font.ITALIC;
		try
		{
			size = Integer.parseInt(sizeS);
		}
		catch (NumberFormatException e)
		{
			size = 11;
		}
		result = new Font(name, style, size);
		return result;
	}

	public String toString()
	{
		return "[Settings]";
	}
	/**
	 *	Returns the name of the driver with the given number
	 *	@see #getDriverClass(int)
	 */
	public String getDriverName(int aNr)
	 throws NoSuchElementException
	{
		String name = this.props.getProperty("driver" + aNr + ".name");
		if (name == null) throw new NoSuchElementException();
		return name;
	}
	
	/**
	 *	Return the driver class associated with the given driver
	 *	definition.
	 *	@throws NoSuchElementException
	 */
	public String getDriverClass(int aNr)
		throws NoSuchElementException
	{
		String classname = this.props.getProperty("driver" + aNr + ".class");
		if (classname == null) throw new NoSuchElementException();
		return classname;
	}

	/**
	 *	Returns the name of the jar file for the driver number
	 *	@see #getDriverName(int)
	 *	@see #getDriverClass(int)
	 *	@param	int - The number of the driver
	 */
	public String getDriverLibrary(int aNr)
		throws NoSuchElementException
	{
		String lib = this.props.getProperty("driver" + aNr + ".library");
		if (lib == null) throw new NoSuchElementException();
		return lib;
	}
	
	/**
	 *	Returns the number of JDBC drivers configured in the settings
	 *	file.
	 *	@see #getDriverClass(int)
	 *	@see #getDriverName(int)
	 *	@see #getDriverLibrary(int)
	 *	@return the number of drivers as defined in driver.count
	 */
	public int getDriverCount()
	{
		String value = this.props.getProperty("driver.count", "0");
		return StringUtil.getIntValue(value, 0);
	}
	
	public void storeWindowPosition(JFrame target)
	{
		int x,y,w,h;
		Point p = target.getLocation();
		Dimension d = target.getSize();
		this.setWindowPosition(p.x, p.y, d.width, d.height);
	}
	
	public void setWindowPosition(int x, int y, int width, int height)
	{
		this.props.setProperty("window.x", Integer.toString(x));
		this.props.setProperty("window.y", Integer.toString(y));
		this.props.setProperty("window.width", Integer.toString(width));
		this.props.setProperty("window.height", Integer.toString(height));
	}

	public void setDividerLocation(int y)
	{
		this.props.setProperty("window.divider", Integer.toString(y));
	}
	
	public int getDividerLocation()
	{
		return StringUtil.getIntValue(this.props.getProperty("window.divider", "-1"));
	}
	
	public int getWindowPosX()
	{
		return StringUtil.getIntValue(this.props.getProperty("window.x", "0"));
	}
	
	public int getWindowPosY()
	{
		return StringUtil.getIntValue(this.props.getProperty("window.y", "0"));
	}
	
	public int getWindowWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("window.width", "0"));
	}
	
	public int getWindowHeight()
	{
		return StringUtil.getIntValue(this.props.getProperty("window.height", "0"));
	}
	
	public int getEditorTabWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("editor.tabwidth", "4"));
	}
	
	public void setEditorTabWidth(int aWidth)
	{
		this.props.setProperty("editor.tabwidth", Integer.toString(aWidth));
	}

	public String getConnectionDriver(int anId)
	{
		return this.props.getProperty("connection" + anId + ".driverclass", "");
	}
	
	public String getConnectionUrl(int anId)
	{
		return this.props.getProperty("connection" + anId + ".url", "");
	}
	
	public String getConnectionUsername(int anId)
	{
		return this.props.getProperty("connection" + anId + ".username", "");
	}
	
	public String getConnectionPassword(int anId)
	{
		return this.props.getProperty("connection" + anId + ".password", "");
	}
	
	public int getConnectionCount()
	{
		return StringUtil.getIntValue(this.props.getProperty("connection.count"));
	}
	
	public int getLastConnection()
	{
		return StringUtil.getIntValue(this.props.getProperty("connection.last"));
	}		
	
}
