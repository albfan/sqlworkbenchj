/*
 * LiquibaseParser
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.liquibase;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import workbench.log.LogMgr;
import workbench.util.CollectionUtil;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

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
	private Set<String> tagsToRead = CollectionUtil.hashSet("sql", "createProcedure");
	private List<LiquibaseTagContent> resultTags = new ArrayList<LiquibaseTagContent>();
	private List<ChangeSetIdentifier> idsToRead;

	private boolean captureContent;
	private static final String CHANGESET_TAG = "changeSet";
	private StringBuilder currentContent;
	private boolean currentSplitValue;

	public LiquibaseParser(WbFile xmlFile)
	{
		this(xmlFile, "UTF-8");
	}

	public LiquibaseParser(WbFile xmlFile, String encoding)
	{
		this.changeLog = xmlFile;
		xmlEncoding = encoding;
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
	 * for the given changeset id. Each tag will be
	 *
	 * @param changeSetIds a list of changeSetIds to use. If this is null, all changesets are used
	 * @return null if no supported tag was found, all stored SQL scripts otherwise
	 */
	public List<LiquibaseTagContent> getContentFromChangeSet(List<ChangeSetIdentifier> changeSetIds)
		throws IOException, SAXException
	{
		idsToRead = changeSetIds == null ? null : new ArrayList<ChangeSetIdentifier>(changeSetIds);
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

	public void characters(char[] buf, int offset, int len)
		throws SAXException
	{
		if (currentContent != null)
		{
			this.currentContent.append(buf, offset, len);
		}
	}

}
