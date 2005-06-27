/*
 * WbProperties.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

/**
 *
 * @author  thomas
 */
public class WbProperties
	extends Properties
{
	private int distinctSections = 2;

	private ArrayList changeListeners = new ArrayList();
	
	public WbProperties()
	{
	}
	
	public WbProperties(int num)
	{
		this.distinctSections = num;
	}

	public void saveToFile(String filename)
		throws IOException
	{
		FileOutputStream out = new FileOutputStream(filename);
		this.save(out);
		out.close();
	}
	
	public void save(OutputStream out)
		throws IOException
	{
		Object[] keys = this.keySet().toArray();
		Arrays.sort(keys);
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
		String value = null;
		String lastKey = null;
		String key = null;
		for (int i=0; i < keys.length; i++)
		{
			key = (String)keys[i];

			if (lastKey != null)
			{
				String k1 = null;
				String k2 = null;
				k1 = getSections(lastKey, this.distinctSections); //getFirstTwoSections(lastKey);
				k2 = getSections(key, this.distinctSections); //getFirstTwoSections(key);
				if (!k1.equals(k2))
				{
					bw.newLine();
				}
			}
			Object v = this.get(key);
			if (v != null)
			{
				value = v.toString();
				value = StringUtil.replace(value, "\\", "\\\\");
				if (value.indexOf('\n') > -1)
				{
					value = value.replaceAll("\n", "\\\\n");
				}
				if (value.length() > 0)
				{
					bw.write(key + "=" + value);
					bw.newLine();
				}
				else
				{
					bw.write(key + "=");
					bw.newLine();
				}
			}
			else
			{
				bw.write(key + "=");
				bw.newLine();
			}
			lastKey = key;
		}
		bw.flush();
	}

	public int getIntProperty(String property, int defaultValue)
	{
		String value = this.getProperty(property, null);
		if (value == null) return defaultValue;
		return StringUtil.getIntValue(value, defaultValue);
	}
	
	public boolean getBoolProperty(String property, boolean defaultValue)
	{
		String value = this.getProperty(property, null);
		if (value == null) return defaultValue;
		return StringUtil.stringToBool(value);
	}
	
	public void setProperty(String property, int value)
	{
		this.setProperty(property, Integer.toString(value));
	}
	
	public void setProperty(String property, boolean value)
	{
		this.setProperty(property, Boolean.toString(value));
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
	
	private String getSections(String aString, int aNum)
	{
		int pos = aString.indexOf(".");
		String result = null;
		for (int i=1; i < aNum; i++)
		{
			int pos2 = aString.indexOf('.', pos + 1);
			if (pos2 > -1)
			{
				pos = pos2;
			}
			else
			{
				if (i == (aNum - 1))
				{
					pos = aString.length();
				}
			}
		}
		result = aString.substring(0, pos);
		return result;
	}

	public void addPropertyChangeListener(PropertyChangeListener aListener)
	{
		this.changeListeners.add(aListener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener aListener)
	{
		this.changeListeners.remove(aListener);
	}
	
	private void firePropertyChanged(String name, String oldValue, String newValue)
	{
		int count = this.changeListeners.size();
		if (count == 0) return;
		PropertyChangeEvent evt = new PropertyChangeEvent(this, name, oldValue, newValue);
		for (int i=0; i < count; i++)
		{
			PropertyChangeListener l = (PropertyChangeListener)this.changeListeners.get(i);
			if (l != null) l.propertyChange(evt);
		}
	}
	
	public Object setProperty(String name, String value)
	{
		String oldValue = (String)super.setProperty(name, value);
		
		// Only fire propertyChanged event if something has changed!
		if ( oldValue == null && value == null) return oldValue;
		
		if ( (oldValue == null && value != null) || 
			   (oldValue != null && value == null) ||
			   !oldValue.equals(value))
		{
			this.firePropertyChanged(name, oldValue, value);
		}
		return oldValue;
	}

	/**
	 *	Adds a property definition in the form key=value
	 *	Lines starting with # are ignored
	 *	Lines that do not contain a = character are ignored
	 *  Any text after a # sign in the value is ignored
	 */
	public void addPropertyDefinition(String line)
	{
		if (line == null) return;
		if (line.trim().length() == 0) return;
		if (line.startsWith("#")) return;
		int pos = line.indexOf("=");
		if (pos == -1) return;
		String key = line.substring(0, pos);
		String value = line.substring(pos + 1);
		pos = value.indexOf('#');
		if (pos > -1)
		{
			value = value.substring(0, pos);
		}
		this.setProperty(key, value.trim());
	}
	
	/**
	 *	Read the content of the file without replacing the 
	 *	usual Java escape sequences. This method does not 
	 *  support Unicode sequences nor line continuation!
	 */
	public void loadTextFile(String filename)
		throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(filename));
		String line = in.readLine();
		while (line != null)
		{
			this.addPropertyDefinition(line);
			line = in.readLine();
		}
	}
	
}
