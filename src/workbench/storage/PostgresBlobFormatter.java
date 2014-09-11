/*
 * PostgresBlobFormatter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.storage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

import workbench.db.exporter.BlobMode;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

/**
 * A class to format a byte[] array to be used as a literal in a SQL statement for PostgreSQL.
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
	private BlobLiteralType blobLiteral;

	public PostgresBlobFormatter()
	{
		String type = Settings.getInstance().getProperty("workbench.db.postgresql.blobformat", "decode");
		if ("escape".equalsIgnoreCase(type))
		{
			blobLiteral = BlobLiteralType.pgEscape;
		}
		else
		{
			blobLiteral = BlobLiteralType.pgDecode;
		}
	}

	public PostgresBlobFormatter(BlobMode mode)
	{
		switch (mode)
		{
			case pgEscape:
				blobLiteral = BlobLiteralType.pgEscape;
				break;
			case pgHex:
				blobLiteral = BlobLiteralType.pgHex;
				break;
			default:
				blobLiteral = BlobLiteralType.pgDecode;
		}
	}

	public PostgresBlobFormatter(BlobLiteralType mode)
	{
		this.blobLiteral = mode;
	}


	@Override
	public CharSequence getBlobLiteral(Object value)
		throws SQLException
	{
		switch (blobLiteral)
		{
			case pgEscape:
				return getEscapeString(value);
			case pgHex:
				return getHexString(value);
			default:
				return getDecodeString(value);

		}
	}

	private CharSequence getDecodeString(Object value)
	{
		if (value == null) return null;
		byte[] buffer = getBytes(value);
		if (buffer == null) return value.toString();

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

	private CharSequence getHexString(Object value)
	{
		if (value == null) return null;
		byte[] buffer = getBytes(value);
		if (buffer == null) return value.toString();

		StringBuilder result = new StringBuilder(buffer.length * 2 + 5);
		result.append("\\\\x");
		for (int i = 0; i < buffer.length; i++)
		{
			int c = (buffer[i] < 0 ? 256 + buffer[i] : buffer[i]);
			result.append(StringUtil.hexString(c, 2));
		}
		return result;
	}

	private CharSequence getEscapeString(Object value)
	{
		if (value == null) return null;
		byte[] buffer = getBytes(value);
		if (buffer == null) return value.toString();

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

	private byte[] getBytes(Object value)
	{
		if (value instanceof byte[])
		{
			return (byte[])value;
		}

		if (value instanceof InputStream)
		{
			// When doing an export the Blobs might be returned as InputStreams
			InputStream in = (InputStream)value;
			try
			{
				return FileUtil.readBytes(in);
			}
			catch (IOException io)
			{
				LogMgr.logError("PostgresBlobFormatter.getBytes()", "Could not read input stream", io);
			}
		}
		return null;
	}

	@Override
	public BlobLiteralType getType()
	{
		return blobLiteral;
	}

}

