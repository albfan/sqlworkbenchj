/*
 * Settings.java
 *
 * Created on December 1, 2001, 7:00 PM
 */
package workbench.resource;

import java.util.Properties;
import java.awt.Font;
import java.io.FileInputStream;
import java.io.IOException;

import workbench.log.LogMgr;

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
	
	public Settings()
	{
		this.props = new Properties();
		String filename = System.getProperty("wb.settings.file", "workbench.settings");
		
		try
		{
			FileInputStream in = new FileInputStream(filename);
			this.props.load(in);
		}
		catch (IOException e)
		{
			LogMgr.logInfo(this, "Settings file '" + filename + "' not found! ", e);
			LogMgr.logInfo(this, "Using defaults");
			fillDefaults();
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
}
