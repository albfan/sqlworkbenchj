/*
 * WbProperties.java
 *
 * Created on June 3, 2003, 6:07 PM
 */

package workbench.util;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Properties;
import workbench.log.LogMgr;

/**
 *
 * @author  thomas
 */
public class WbProperties
	extends Properties
{
	
	/** Creates a new instance of WbProperties */
	public WbProperties()
	{
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
				String k1, k2;
				k1 = getFirstTwoSections(lastKey);
				k2 = getFirstTwoSections(key);
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
	
}
