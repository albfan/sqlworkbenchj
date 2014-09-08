/*
 * LiquibaseParser.java
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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.MessageBuffer;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author Thomas Kellerer
 */
public class LiquibaseParser
	extends DefaultHandler
{
	private WbFile changeLog;
	private String xmlEncoding;
	private SAXParser saxParser;
	private Set<String> tagsToRead = CollectionUtil.treeSet("sql", "createProcedure");
	private List<LiquibaseTagContent> resultTags = new ArrayList<>();
	private List<ChangeSetIdentifier> idsToRead;

	private boolean captureContent;
	private final String CHANGESET_TAG = "changeSet";
	private final String SQL_FILE_TAG = "sqlFile";
	private StringBuilder currentContent;
	private boolean currentSplitValue;
	private final MessageBuffer warnings;

	public LiquibaseParser(WbFile xmlFile, MessageBuffer buffer)
	{
		this(xmlFile, "UTF-8", buffer);
	}

	public LiquibaseParser(WbFile xmlFile, String encoding, MessageBuffer buffer)
	{
		changeLog = xmlFile;
		xmlEncoding = encoding;
		warnings = buffer;
    SAXParserFactory factory = SAXParserFactory.newInstance();
    factory.setValidating(false);
    try
    {
      saxParser = factory.newSAXParser();
    }
    catch (Exception e)
    {
      // should not happen!
      LogMgr.logError("XmlDataFileParser.<init>", "Error creating XML parser", e);
    }
	}

	/**
	 * Return the text stored in all <sql> or <createProcedure> tags
	 * for the given changeset id.
	 *
	 * @param changeSetIds a list of changeSetIds to use. If this is null, all changesets are used
	 * @return null if no supported tag was found, all stored SQL scripts otherwise
	 */
	public List<LiquibaseTagContent> getContentFromChangeSet(List<ChangeSetIdentifier> changeSetIds)
		throws IOException, SAXException
	{
		idsToRead = changeSetIds == null ? null : new ArrayList<>(changeSetIds);
		Reader in = EncodingUtil.createReader(changeLog, xmlEncoding);
		try
		{
			InputSource source = new InputSource(in);
			saxParser.parse(source, this);
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
		return resultTags;
	}

	private boolean isChangeSetIncluded(ChangeSetIdentifier toCheck)
	{
		if (CollectionUtil.isEmpty(idsToRead)) return true;
		for (ChangeSetIdentifier id : idsToRead)
		{
			if (id == null) continue;

			boolean idsEqual = StringUtil.equalString(toCheck.getId(), id.getId());
			if (id.getAuthor() == null && idsEqual) return true;

			boolean authorsEqual = StringUtil.equalString(toCheck.getAuthor(), id.getAuthor());
			if (idsEqual && authorsEqual) return true;

			if (authorsEqual && id.getId().equals("*")) return true;
		}
		return false;
	}

	@Override
	public void startElement(String namespaceURI, String sName, String tagName, Attributes attrs)
		throws SAXException
	{
		if (tagName.equals(CHANGESET_TAG))
		{
			ChangeSetIdentifier id = new ChangeSetIdentifier(attrs.getValue("author"), attrs.getValue("id"));
			if (isChangeSetIncluded(id))
			{
				captureContent = true;
			}
		}
		else if (tagName.equals(SQL_FILE_TAG) && attrs.getValue("path") != null)
		{
			String path = attrs.getValue("path");
			String delim = attrs.getValue("endDelimiter");
			boolean split = Boolean.parseBoolean(attrs.getValue("splitStatements"));

			WbFile file = new WbFile(path);
			File parent = changeLog.getParentFile();

			if (!file.exists() && !file.isAbsolute())
			{
				file = new WbFile(parent, path);
			}

			if (file.exists())
			{
				List<String> statements = readSqlFile(file, delim, split);
				for (String sql : statements)
				{
					LiquibaseTagContent tag = new LiquibaseTagContent(sql, false);
					resultTags.add(tag);
				}
			}
			else
			{
				String msg = ResourceMgr.getFormattedString("ErrFileNotFound", path);
				warnings.append(msg);
				LogMgr.logError("LiquibaseParser.startElement()", "sqlFile=\"" + path + "\" not found!", null);
			}
		}
		else if (captureContent && tagsToRead.contains(tagName))
		{
			currentContent = new StringBuilder(500);
			if (tagName.equals("sql"))
			{
				String split = attrs.getValue("splitStatements");
				if (StringUtil.isBlank(split)) split = "true";
				currentSplitValue = StringUtil.stringToBool(split);
			}
			else
			{
				currentSplitValue = false;
			}
		}
	}

	@Override
	public void endElement(String namespaceURI, String sName, String tagName)
		throws SAXException
	{
		if (tagName.equals(CHANGESET_TAG))
		{
			captureContent = false;
		}
		if (currentContent != null && tagsToRead.contains(tagName))
		{
			LiquibaseTagContent tag = new LiquibaseTagContent(currentContent.toString(), currentSplitValue);
			resultTags.add(tag);
			currentContent = null;
		}
	}

	@Override
	public void characters(char[] buf, int offset, int len)
		throws SAXException
	{
		if (currentContent != null)
		{
			this.currentContent.append(buf, offset, len);
		}
	}

	private List<String> readSqlFile(WbFile include, String delimiter, boolean splitStatements)
		throws SAXException
	{
		List<String> result = new ArrayList<>();
		try
		{
			if (splitStatements)
			{
				ScriptParser parser = new ScriptParser(include, xmlEncoding);
				if (StringUtil.isNonBlank(delimiter))
				{
					DelimiterDefinition delim = new DelimiterDefinition(delimiter);
					if (!delim.isStandard())
					{
						parser.setAlternateDelimiter(delim);
					}
				}
				int count = parser.getSize();
				for (int i=0; i < count; i++)
				{
					result.add(parser.getCommand(i));
				}
			}
			else
			{
				String script = FileUtil.readFile(include, xmlEncoding);
				result.add(script);
			}
		}
		catch (IOException io)
		{
			throw new SAXException(io);
		}
		return result;
	}

}
