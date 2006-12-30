/*
 * HexBlobFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.Blob;
import java.sql.SQLException;

/**
 * @author support@sql-workbench.net
 */
public class HexBlobFormatter
	implements BlobLiteralFormatter
{
	
	private String prefix;
	private String suffix;
	private boolean upperCase = false;
	
	public HexBlobFormatter()
	{
	}

	public void setUseUpperCase(boolean flag) { this.upperCase = flag; }
	public void setPrefix(String p) { this.prefix = p; }
	public void setSuffix(String s) { this.suffix = s; }
	
	public String getBlobLiteral(Object value)
		throws SQLException
	{
		if (value == null) return null;

		int addSpace = (prefix != null ? prefix.length() : 0);
		addSpace += (suffix != null ? suffix.length() : 0);
		
		StringBuilder result = null; 
		
		if (value instanceof byte[])
		{
			byte[] buffer = (byte[])value;
			result = new StringBuilder(buffer.length * 2 + addSpace);
			if (prefix != null) result.append(prefix);
			appendArray(result, buffer);
		}
		else if (value instanceof Blob)
		{
			Blob b = (Blob)value;
			int len = (int)b.length();
			result = new StringBuilder(len*2 + addSpace);
			if (prefix != null) result.append(prefix);
			for (int i = 0; i < len; i++)
			{
				byte[] byteBuffer = b.getBytes(i, 1);
				appendArray(result, byteBuffer);
			}
		}
		else
		{
			result = new StringBuilder(100+addSpace);
			if (prefix != null) result.append(prefix);
			result.append(value.toString());
		}
		if (suffix != null) result.append(suffix);
		return result.toString();
	}

	private void appendArray(StringBuilder result, byte[] buffer)
	{
		for (int i = 0; i < buffer.length; i++)
		{
			int c = (buffer[i] < 0 ? 256 + buffer[i] : buffer[i]);
			String s = Integer.toHexString(c);
			if (s.length() == 1) result.append('0');
			if (upperCase)
				result.append(s.toUpperCase());
			else
				result.append(s);
		}
	}	
}
