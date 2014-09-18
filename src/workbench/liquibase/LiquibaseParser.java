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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptParser;

import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.ExceptionUtil;
import workbench.util.FileUtil;
import workbench.util.MessageBuffer;
import workbench.util.StringUtil;
import workbench.util.WbFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Thomas Kellerer
 */
public class LiquibaseParser
{
	private WbFile changeLog;
	private String fileEncoding;
	private MessageBuffer messages;
	private final String TAG_CHANGESET = "changeSet";
	private final String TAG_SQLFILE = "sqlFile";
	private final String TAG_SQL = "sql";
	private final String TAG_CREATEPROC = "createProcedure";

	public LiquibaseParser(WbFile xmlFile, String encoding, MessageBuffer buffer)
	{
		changeLog = xmlFile;
		fileEncoding = encoding;
		messages = buffer;
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
		List<LiquibaseTagContent> result = new ArrayList<>();
		try
		{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(changeLog);

			doc.getDocumentElement().normalize();
			NodeList elements = doc.getElementsByTagName(TAG_CHANGESET);
			int size = elements.getLength();
			for (int i=0; i < size; i++)
			{
				Node item = elements.item(i);
				ChangeSetIdentifier cs = getChangeSetId(item);
				if (isChangeSetIncluded(changeSetIds, cs) && item instanceof Element)
				{
					List<LiquibaseTagContent> content = getContent((Element)item);
					if (content != null)
					{
						result.addAll(content);
					}
				}
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("LiquibaseParser.getContentFromChangeSet()", "Could not parse file: " + changeLog.getFullPath(), ex);
		}
		return result;
	}

	private List<LiquibaseTagContent> getContent(Element item)
	{
		List<LiquibaseTagContent> result = new ArrayList<>();

		NodeList nodes = item.getChildNodes();
		int size = nodes.getLength();
		for (int i=0; i<size; i++)
		{
			Node node = nodes.item(i);
			if (node.getNodeType() != Node.ELEMENT_NODE) continue;

			Element element = (Element)node;
			String tagName = element.getTagName();

			if (TAG_SQL.equals(tagName) || TAG_CREATEPROC.equals(tagName))
			{
				boolean splitStatements = false;
				if (TAG_SQL.equals(tagName))
				{
					splitStatements = getSplitAttribute(element);
				}
				result.add(new LiquibaseTagContent(element.getTextContent(), splitStatements));
			}

			if (TAG_SQLFILE.equals(tagName))
			{
				result.addAll(getContentFromSqlFile(element));
			}

		}
		return result;
	}

	private List<LiquibaseTagContent> getContentFromSqlFile(Element element)
	{
		String path = element.getAttribute("path");
		List<LiquibaseTagContent> result = new ArrayList<>();

		if (StringUtil.isEmptyString(path)) return result;

		String encoding = element.getAttribute("encoding");
		if (StringUtil.isEmptyString(encoding)) encoding = fileEncoding;
		if (StringUtil.isEmptyString(encoding))	encoding = EncodingUtil.getDefaultEncoding();
		String delimiter = element.getAttribute("endDelimiter");
		boolean relative = StringUtil.stringToBool(element.getAttribute("relativeToChangelogFile"));
		boolean split = getSplitAttribute(element);
		WbFile include;

		if (relative)
		{
			include = new WbFile(changeLog.getParentFile(), path);
		}
		else
		{
			include = new WbFile(path);
		}

		if (include.exists())
		{
			List<String> statements = readSqlFile(include, delimiter, encoding, split);
			for (String sql : statements)
			{
				result.add(new LiquibaseTagContent(sql, false));
			}
		}
		else
		{
			String msg = ResourceMgr.getFormattedString("ErrFileNotFound", path);
			messages.append(msg);
			LogMgr.logError("LiquibaseParser.getContent()", "sqlFile=\"" + path + "\" not found!", null);
		}

		return result;
	}

	private boolean getSplitAttribute(Element element)
	{
		String split = element.getAttribute("splitStatements");
		if (StringUtil.isBlank(split)) return true;
		return Boolean.parseBoolean(split);
	}

	private ChangeSetIdentifier getChangeSetId(Node item)
	{
		NamedNodeMap attributes = item.getAttributes();
		String author = attributes.getNamedItem("author").getTextContent();
		String id = attributes.getNamedItem("id").getTextContent();
		ChangeSetIdentifier cs = new ChangeSetIdentifier(author, id);
		if (item instanceof Element)
		{
			NodeList comments = ((Element)item).getElementsByTagName("comment");
			if (comments != null && comments.getLength() == 1)
			{
				String comment = comments.item(0).getTextContent();
				cs.setComment(comment);
			}
		}
		return cs;
	}

	public List<ChangeSetIdentifier> getChangeSets()
	{
		List<ChangeSetIdentifier> result = new ArrayList<>();
		try
		{
			DocumentBuilderFactory factor = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factor.newDocumentBuilder();
			Document doc = builder.parse(changeLog);

			doc.getDocumentElement().normalize();
			NodeList elements = doc.getElementsByTagName(TAG_CHANGESET);
			int size = elements.getLength();
			for (int i=0; i < size; i++)
			{
				Node item = elements.item(i);
				ChangeSetIdentifier id = getChangeSetId(item);
				result.add(id);
			}
		}
		catch (Exception ex)
		{
			LogMgr.logError("LiquibaseParser.getChangeSets()", "Could not parse file: " + changeLog.getFullPath(), ex);
		}
		return result;
	}

	private boolean isChangeSetIncluded(List<ChangeSetIdentifier> toRead, ChangeSetIdentifier toCheck)
	{
		if (toCheck == null) return false;
		if (CollectionUtil.isEmpty(toRead)) return true;

		for (ChangeSetIdentifier id : toRead)
		{
			if (toCheck.isEqualTo(id)) return true;
		}

		return false;
	}

	private List<String> readSqlFile(WbFile include, String delimiter, String encoding, boolean splitStatements)
	{
		List<String> result = new ArrayList<>();
		try
		{
			if (splitStatements)
			{
				ScriptParser parser = new ScriptParser(include, encoding);
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
				String script = FileUtil.readFile(include, encoding);
				result.add(script);
			}
		}
		catch (Exception ex)
		{
			messages.append(ExceptionUtil.getDisplay(ex));
			LogMgr.logError("LiquibaseParser.getContent()", "Could not read sqlFile=\"" + include.getFullPath() + "\"", ex);
		}
		return result;
	}

}
