/*
 * DefaultBlobFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.Blob;
import java.sql.SQLException;
import workbench.util.Base64;
import workbench.util.NumberStringCache;
import workbench.util.StringUtil;

/**
 * @author support@sql-workbench.net
 */
public class DefaultBlobFormatter
	implements BlobLiteralFormatter
{
	private String prefix;
	private String suffix;
	private boolean upperCase = false;
	private BlobLiteralType literalType = BlobLiteralType.hex;
	
	public void setLiteralType(BlobLiteralType type)
	{
		this.literalType = (type == null ? BlobLiteralType.hex : type);
	}
	
	public void setUseUpperCase(boolean flag)
	{
		this.upperCase = flag;
	}

	public void setPrefix(String p)
	{
		this.prefix = p;
	}

	public void setSuffix(String s)
	{
		this.suffix = s;
	}
	
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
			result = new StringBuilder(len * 2 + addSpace);
			if (prefix != null) result.append(prefix);
			for (int i = 0; i < len; i++)
			{
				byte[] byteBuffer = b.getBytes(i, 1);
				appendArray(result, byteBuffer);
			}
		}
		else
		{
			String s = value.toString();
			result = new StringBuilder(s.length() + addSpace);
			if (prefix != null) result.append(prefix);
			result.append(s);
		}
		if (suffix != null) result.append(suffix);
		return result.toString();
	}

	private void appendArray(StringBuilder result, byte[] buffer)
	{
		if (literalType == BlobLiteralType.base64)
		{
			result.append(Base64.encodeBytes(buffer));
			return;
		}
		for (int i = 0; i < buffer.length; i++)
		{
			int c = (buffer[i] < 0 ? 256 + buffer[i] : buffer[i]);
			CharSequence s = null;
			if (literalType == BlobLiteralType.octal)
			{
				result.append("\\");
				s = StringUtil.getOctalString(c);
			}
			else
			{
				s = NumberStringCache.getHexString(c);
			}

			if (upperCase)
			{
				result.append(s.toString().toUpperCase());
			}
			else
			{
				result.append(s);
			}
		}
	}
	
}
