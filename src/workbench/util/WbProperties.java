/*
 * WbProperties.java
 *
 * Created on June 3, 2003, 6:07 PM
 */

package workbench.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
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
				value = StringUtil.replace(v.toString(), "\\", "\\\\");
				if (value.length() > 0)
				{
					bw.write(key + "=" + value);
					bw.newLine();
				}
			}
			lastKey = key;
		}
		bw.flush();
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

	public void addChangeListener(PropertyChangeListener aListener)
	{
		this.changeListeners.add(aListener);
	}
	
	public void removeChangeListener(PropertyChangeListener aListener)
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
		Object oldValue = super.setProperty(name, value);
		this.firePropertyChanged(name, (String)oldValue, value);
		return oldValue;
	}
	
	
	public static void main(String args[])
	{
		try
		{
			WbProperties props = new WbProperties(1);
			System.out.println(props.getSections("first.second.third.fourth", 2));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("done.");
		System.exit(0);
	}
}
