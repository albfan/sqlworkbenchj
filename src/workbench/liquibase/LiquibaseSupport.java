/*
 * LiquibaseSupport.java
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
package workbench.liquibase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import workbench.log.LogMgr;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ParserType;
import workbench.sql.ScriptParser;

import workbench.util.MessageBuffer;
import workbench.util.WbFile;

/**
 * A class to extract SQL from a Liquibase changeset.
 *
 * @author Thomas Kellerer
 */
public class LiquibaseSupport
{
	private WbFile changeLog;
	private String fileEncoding;
	private DelimiterDefinition alternateDelimiter;
	private MessageBuffer messages = new MessageBuffer();
	private ParserType parserType;

	public LiquibaseSupport(WbFile xmlFile)
	{
		this(xmlFile, null);
	}

	public LiquibaseSupport(WbFile xmlFile, String encoding)
	{
		this.changeLog = xmlFile;
		this.fileEncoding = encoding;
	}

	public void setParserType(ParserType type)
	{
		this.parserType = type;
	}

	public void setAlternateDelimiter(DelimiterDefinition delimiter)
	{
		alternateDelimiter = delimiter;
	}

	public List<String> getSQLFromChangeSet(ChangeSetIdentifier ... ids)
	{
		return getSQLFromChangeSet(ids == null ? null : Arrays.asList(ids));
	}

	/**
	 * Return the statements stored in a <sql> or <createProcedure> tag
	 * of a Liquibase Changeset
	 *
	 * @param ids the list of changeSet IDs to extract
	 * @return null if no supported tag was found, all stored SQL scripts otherwise
	 */
	public List<String> getSQLFromChangeSet(List<ChangeSetIdentifier> ids)
	{
		LiquibaseParser parser = new LiquibaseParser(changeLog, fileEncoding, messages);
		List<String> result = null;
		try
		{
			List<LiquibaseTagContent> content = parser.getContentFromChangeSet(ids);
			result = new ArrayList<>(content.size());
			for (LiquibaseTagContent tag : content)
			{
				if (tag.getSplitStatements())
				{
					ScriptParser p = new ScriptParser(parserType);
					p.setAlternateDelimiter(alternateDelimiter);
					p.setScript(tag.getContent());
					p.startIterator();
					while (p.hasNext())
					{
						String sql = p.getNextCommand();
						result.add(sql);
					}
				}
				else
				{
					result.add(tag.getContent());
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("LiquibaseSupport.getSQLFromChangeSet()", "Error parsing XML", e);
		}

		return result;
	}

	public MessageBuffer getWarnings()
	{
		return messages;
	}
}
