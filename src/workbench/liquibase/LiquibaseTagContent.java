/*
 * LbTagContent
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.liquibase;

/**
 * @author Thomas Kellerer
 */
public class LiquibaseTagContent
{
	private String tagContent;
	private boolean splitStatements;

	public LiquibaseTagContent(String content)
	{
		this(content, false);
	}

	public LiquibaseTagContent(String content, boolean splitStatements)
	{
		this.tagContent = content;
		this.splitStatements = splitStatements;
	}

	public String getContent()
	{
		return tagContent;
	}

	public boolean getSplitStatements()
	{
		return splitStatements;
	}
}
