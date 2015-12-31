/*
 * LiquibaseParser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.sql.DelimiterDefinition;
import workbench.sql.parser.ParserType;
import workbench.sql.parser.ScriptParser;

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
 * A class to read information from a Liquibase ChangeLog.
 *
 * @author Thomas Kellerer
 */
public class LiquibaseParser
{
	private final WbFile changeLog;
	private String fileEncoding;
	private MessageBuffer messages;
	private final String TAG_CHANGESET = "changeSet";
	private final String TAG_SQLFILE = "sqlFile";
	private final String TAG_SQL = "sql";
	private final String TAG_CREATEPROC = "createProcedure";
	private final ParserType sqlParserType;

	public LiquibaseParser(WbFile xmlFile)
	{
		this(xmlFile, "UTF-8", new MessageBuffer(), ParserType.Standard);
	}

	public LiquibaseParser(WbFile xmlFile, String encoding, MessageBuffer buffer, ParserType parserType)
	{
		changeLog = xmlFile;
		fileEncoding = encoding;
		messages = buffer;
		sqlParserType = parserType;
	}

	public List<String> getContentFromChangeSet(ChangeSetIdentifier ... ids)
		throws IOException, SAXException
	{
		return getContentFromChangeSet(ids == null ? null : Arrays.asList(ids));
	}

	/**
	 * Return the text stored in all <sql> or <createProcedure> tags
	 * for the given changeset id.
	 *
	 * The SQL statements are already split according to the <tt>splitStatements</tt> attribute
	 * of the <tt>sql</tt> or <tt>sqlFile</tt> tags respecting the <tt>endDelimiter</tt> attribute as well.
	 *
	 * @param changeSetIds a list of changeSetIds to use. If this is null, all changesets are returned
	 *
	 * @return null if no supported tag was found, all stored SQL scripts otherwise
	 */
	public List<String> getContentFromChangeSet(List<ChangeSetIdentifier> changeSetIds)
		throws IOException, SAXException
	{
		List<String> result = new ArrayList<>();
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
					List<String> content = getContent((Element)item);
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

	/**
	 * Return a list of all ChangeSet ids defined in the ChangeLog file.
	 *
	 * Files included using the <tt>include</tt> will not be evaluated.
	 *
	 * @return  all changesets from the changelog file.
	 */
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

	private List<String> getContent(Element item)
	{
		List<String> result = new ArrayList<>();

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
				DelimiterDefinition delim = getDelimiter(element);
				if (splitStatements)
				{
					result.addAll(splitStatements(element.getTextContent(), delim));
				}
				else
				{
					result.add(element.getTextContent());
				}
			}

			if (TAG_SQLFILE.equals(tagName))
			{
				result.addAll(getContentFromSqlFile(element));
			}

		}
		return result;
	}

	private List<String> splitStatements(String content, DelimiterDefinition delimiter)
	{
		List<String> result = new ArrayList<>();
		ScriptParser parser = new ScriptParser(content, sqlParserType);
		parser.setAlternateDelimiter(delimiter);
		int count = parser.getSize();

		for (int c = 0; c < count; c++)
		{
			result.add(parser.getCommand(c));
		}
		return result;
	}

	private List<String> getContentFromSqlFile(Element element)
	{
		String path = element.getAttribute("path");
		List<String> result = new ArrayList<>();

		if (StringUtil.isEmptyString(path)) return result;

		String encoding = element.getAttribute("encoding");
		if (StringUtil.isEmptyString(encoding)) encoding = fileEncoding;
		if (StringUtil.isEmptyString(encoding))	encoding = EncodingUtil.getDefaultEncoding();
		DelimiterDefinition delimiter = getDelimiter(element);
		boolean relative = StringUtil.stringToBool(element.getAttribute("relativeToChangelogFile"));
		boolean split = getSplitAttribute(element);
		WbFile pathFile = new WbFile(path);
		WbFile include;

		if (relative || !pathFile.isAbsolute())
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
			result.addAll(statements);
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

	/**
	 * Return the delimiter defined through the attribute <tt>endDelimiter</tt> from the DOM node.
	 *
	 * @param node the tag to use
	 * @return null if no endDelimiter was defined or if it was defined as ;
	 */
	private DelimiterDefinition getDelimiter(Element node)
	{
		String delimiter = node.getAttribute("endDelimiter");
		if (StringUtil.isBlank(delimiter)) return null;
		DelimiterDefinition delim = new DelimiterDefinition(delimiter);
		if (delim.isStandard()) return null;
		return delim;
	}

	/**
	 * Extract the complete changeSet ID from a changeSet tag.
	 *
	 * @param item  the DOM node
	 * @return the id of the tag
	 */
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

	private List<String> readSqlFile(WbFile include, DelimiterDefinition delimiter, String encoding, boolean splitStatements)
	{
		List<String> result = new ArrayList<>();
		try
		{
			if (splitStatements)
			{
				ScriptParser parser = new ScriptParser(sqlParserType);
				parser.setDelimiter(delimiter);
				parser.setFile(include, encoding);
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
