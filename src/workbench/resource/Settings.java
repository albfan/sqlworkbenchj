/*
 * Settings.java
 *
 * Created on December 1, 2001, 7:00 PM
 */
package workbench.resource;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Properties;
import java.awt.Font;
import java.awt.Point;
import java.io.BufferedInputStream;
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
		this.filename = System.getProperty("workbench.settings.file", "workbench.settings");
		
		try
		{
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(this.filename));
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
					String k1, k2;
					k1 = getFirstTwoSections(lastKey);
					k2 = getFirstTwoSections(key);
					if (!k1.equals(k2))
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

	private String getFirstTwoSections(String aString)
	{
		int pos1 = aString.indexOf(".");
		String result;
		if (pos1 > -1) 
		{
			int pos2 = aString.indexOf(".", pos1 + 1);
			if (pos2 > -1) 
			{
				result = aString.substring(0, pos2);
			}
			else
			{
				result = aString.substring(0, pos1);
			}
			return result;
		}
		else
		{
			return aString;
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
		
		String baseKey = new StringBuffer("workbench.font.").append(aFontName).toString();
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
	
	public String getLastExportDir()
	{
		return this.props.getProperty("workbench.export.lastdir","");
	}
	
	public void setLastExportDir(String aDir)
	{
		this.props.setProperty("workbench.export.lastdir", aDir);
	}


	public String toString()
	{
		return "[Settings]";
	}
	
	public void storeWindowPosition(Component target)
	{
		Point p = target.getLocation();
		String id = target.getClass().getName();
		this.setWindowPosition(id, p.x, p.y);
	}
	
	public void storeWindowSize(Component target)
	{
		Dimension d = target.getSize();
		String id = target.getClass().getName();
		this.setWindowSize(id, d.width, d.height);
	}
	
	public void setWindowPosition(String windowClass, int x, int y)
	{
		this.props.setProperty(windowClass + ".x", Integer.toString(x));
		this.props.setProperty(windowClass + ".y", Integer.toString(y));
	}

	public void setWindowSize(String windowClass, int width, int height)
	{
		this.props.setProperty(windowClass + ".width", Integer.toString(width));
		this.props.setProperty(windowClass + ".height", Integer.toString(height));
	}
	
	public boolean restoreWindowSize(Component target)
	{
		boolean result = false;
		String id = target.getClass().getName();
		int w = this.getWindowWidth(id);
		int h = this.getWindowHeight(id);
		if (w > 0 && h > 0) 
		{
			target.setSize(new Dimension(w, h));
			result = true;
		}
		return result;
	}

	public boolean restoreWindowPosition(Component target)
	{
		boolean result = false;
		String id = target.getClass().getName();
		int x = this.getWindowPosX(id);
		int y = this.getWindowPosY(id);
		if (x > 0 && y > 0) 
		{
			target.setLocation(new Point(x, y));
			result = true;
		}
		return result;
	}
	
	
	public void setSqlDividerLocation(int y)
	{
		this.props.setProperty("workbench.gui.sql.divider", Integer.toString(y));
	}
	
	public int getSqlDividerLocation()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.gui.sql.divider", "-1"));
	}
	
	public int getWindowPosX(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".x", "0"));
	}
	
	public int getWindowPosY(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".y", "0"));
	}
	
	public int getWindowWidth(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".width", "0"));
	}
	
	public int getWindowHeight(String windowClass)
	{
		return StringUtil.getIntValue(this.props.getProperty(windowClass + ".height", "0"));
	}
	
	public int getEditorTabWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("editortabwidth", "4"));
	}
	
	public void setEditorTabWidth(int aWidth)
	{
		this.props.setProperty("editor.tabwidth", Integer.toString(aWidth));
	}
	
	public String getLastConnection()
	{
		return this.props.getProperty("connection.last");
	}		

	public void setLastConnection(String aName)
	{
		if (aName == null) aName = "";
		this.props.setProperty("connection.last", aName);
	}

	public String getLastLibraryDir()
	{
		return this.props.getProperty("drivers.lastlibdir", "");
	}
	public void setLastLibraryDir(String aDir)
	{
		this.props.setProperty("drivers.lastlibdir", aDir);
	}
	
	public int getMaxHistorySize()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.historysize", "15"));
	}
	
	public int getDefaultTabCount()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.defaulttabcount", "4"));
	}

	public void setDefaultTabCount(int aCount)
	{
		this.props.setProperty("workbench.sql.defaulttabcount", Integer.toString(aCount));
	}

	public void setLookAndFeelClass(String aClassname)
	{
		this.props.setProperty("workbench.gui.lookandfeelclass", aClassname);
	}
	
	public String getLookAndFeelClass()
	{
		return this.props.getProperty("workbench.gui.lookandfeelclass", "");
	}

	public int getPreferredColumnWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.preferredcolwidth", "80"));
	}
	public void setPreferredColumnWidth(int aWidth)
	{
		this.props.setProperty("workbench.sql.preferredcolwidth", Integer.toString(aWidth));
	}
	
	public int getMaxColumnWidth()
	{
		return StringUtil.getIntValue(this.props.getProperty("workbench.sql.maxcolwidth", "500"));
	}
	public void setMaxColumnWidth(int aWidth)
	{
		this.props.setProperty("workbench.sql.maxcolwidth", Integer.toString(aWidth));
	}
}
