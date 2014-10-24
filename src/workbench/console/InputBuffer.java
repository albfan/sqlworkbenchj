/*
 * InputBuffer.java
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
package workbench.console;

import workbench.db.DbMetadata;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ParserType;
import workbench.sql.ScriptParser;

/**
 * A buffer that collects pieces of text entered by the user until
 * it is terminated with a {@link workbench.sql.DelimiterDefinition}
 *
 * @author Thomas Kellerer
 */
public class InputBuffer
{
	private StringBuilder script;
	private DelimiterDefinition delimiter;
	private boolean checkMySQLComments;
	private ScriptParser parser;
	private ParserType parserType = ParserType.Standard;

	public InputBuffer()
	{
		this.delimiter = DelimiterDefinition.STANDARD_DELIMITER;
		script = new StringBuilder(1000);
		parser = new ScriptParser(parserType);
	}

	public void setDbId(String dbid)
	{
		ParserType type = ParserType.getTypeFromDBID(dbid);
		if (type != this.parserType)
		{
			parser = new ScriptParser(type);
			parserType = type;
		}
		checkMySQLComments = DbMetadata.DBID_MYSQL.equals(dbid);
	}

	public String getScript()
	{
		return script.toString();
	}

	public int getLength()
	{
		return script.length();
	}

	public void clear()
	{
		script.setLength(0);
	}

	public DelimiterDefinition getDelimiter()
	{
		return delimiter;
	}

	public void setDelimiter(DelimiterDefinition delim)
	{
		this.delimiter = delim;
	}

	public boolean addLine(String line)
	{
		script.append('\n');
		script.append(line);
		return isComplete();
	}

	public boolean isComplete()
	{
		String sql = script.toString();
		if (parser.supportsMixedDelimiter())
		{
			if (parser.isSingleLineCommand(sql))
			{
				return true;
			}
		}
		return delimiter.terminatesScript(script.toString(), checkMySQLComments);
	}
}
