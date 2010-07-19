/*
 * PostgresBlobFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.sql.SQLException;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 * A class to format a byte[] array to be used as a literal in a SQL
 * statement for PostgreSQL.
 * <br/>
 * The literal is constructed using the decode() function and a hex representation
 * of the value (as that is the most compact form)
 * <br/>
 * The actual format is controlled through the configuration property workbench.db.postgresql.blobformat
 * valid values are <tt>decode</tt>, and <tt>escape</tt>
 * For escape an binary "escape" syntax is used, e.g.: E'\\001'::bytea
 * 
 * See also: http://www.postgresql.org/docs/current/static/datatype-binary.html
 *
 * @author Thomas Kellerer
 */
public class PostgresBlobFormatter
	implements BlobLiteralFormatter
{

	private boolean useEscape;

	public PostgresBlobFormatter()
	{
		String type = Settings.getInstance().getProperty("workbench.db.postgresql.blobformat", "decode");
		useEscape = "escape".equalsIgnoreCase(type);
	}
	
	public PostgresBlobFormatter(boolean useEscapedOctal)
	{
		useEscape = useEscapedOctal;
	}

	public PostgresBlobFormatter(BlobLiteralType mode)
	{
		useEscape = (mode == BlobLiteralType.pgEscape);
	}

	public CharSequence getBlobLiteral(Object value)
		throws SQLException
	{
		if (useEscape) return getEscapeString(value);
		return getDecodeString(value);
	}
	
	private CharSequence getDecodeString(Object value)
	{
		if (value == null) return null;
		if (value instanceof byte[])
		{
			byte[] buffer = (byte[])value;
			StringBuilder result = new StringBuilder(buffer.length * 2 + 20);
			result.append("decode('");
			for (int i = 0; i < buffer.length; i++)
			{
				int c = (buffer[i] < 0 ? 256 + buffer[i] : buffer[i]);
				result.append(StringUtil.hexString(c, 2));
			}
			result.append("', 'hex')");
			return result;
		}
		return value.toString();
	}

	private CharSequence getEscapeString(Object value)
	{
		if (value == null) return null;
		if (value instanceof byte[])
		{
			byte[] buffer = (byte[])value;
			StringBuilder result = new StringBuilder(buffer.length * 5 + 10);
			result.append("E'");
			for (int i = 0; i < buffer.length; i++)
			{
				result.append("\\\\");
				int c = (buffer[i] < 0 ? 256 + buffer[i] : buffer[i]);
				String s = Integer.toOctalString(c);
				int l = s.length();
				if (l == 1)
				{
					result.append("00");
				}
				else if (l == 2)
				{
					result.append('0');
				}
				result.append(s);
			}
			result.append("'::bytea");
			return result;
		}
		return value.toString();
	}

	@Override
	public BlobLiteralType getType()
	{
		return BlobLiteralType.octal;
	}

}

