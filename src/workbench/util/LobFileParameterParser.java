/*
 * LobFileParameterParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class to analyze the {$blobfile= } and {$clobfile= }
 * parameters in a SQL statement. This class supports INSERT and UPDATE
 * statements. To retrieve a blob from the database {@link workbench.sql.wbcommands.WbSelectBlob}
 * has to be used.
 * @author Thomas Kellerer
 */
public class LobFileParameterParser
{
	private final String MARKER = "\\{\\$[cb]lobfile=";
	private final Pattern MARKER_PATTERN = Pattern.compile(MARKER, Pattern.CASE_INSENSITIVE);
	private LobFileParameter[] parameters;
	private int parameterCount = 0;

	public LobFileParameterParser(String sql)
		throws FileNotFoundException
	{
		Matcher m = MARKER_PATTERN.matcher(sql);
		if (!m.find())
		{
			return;
		}

		// Calculate number of parameters
		parameterCount++;
		while (m.find())
		{
			parameterCount++;
		}
		m.reset();
		parameters = new LobFileParameter[parameterCount];
		int index = 0;
		WbStringTokenizer tok = new WbStringTokenizer(" \t", false, "\"'", false);

		while (m.find())
		{
			int start = m.start();
			int end = sql.indexOf('}', start + 1);
			if (end > -1)
			{
				String parm = sql.substring(start + 2, end);
				tok.setSourceString(parm);
				parameters[index] = new LobFileParameter();
				while (tok.hasMoreTokens())
				{
					String s = tok.nextToken();
					String arg = null;
					String value = null;
					int pos = s.indexOf('=');
					if (pos > -1)
					{
						arg = s.substring(0, pos);
						value = s.substring(pos + 1);
					}
					if ("encoding".equals(arg))
					{
						parameters[index].setEncoding(value);
					}
					else
					{
						parameters[index].setFilename(value);
						parameters[index].setBinary("blobfile".equals(arg));
					}
				}
			}
			index++;
		}
	}

	public LobFileParameter[] getParameters()
	{
		return parameters;
	}
}
