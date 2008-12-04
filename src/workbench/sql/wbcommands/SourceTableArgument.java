/*
 * SourceTableArgument.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 * Evaluate table arguments that may contain wildcards.
 * 
 * @author support@sql-workbench.net
 */
public class SourceTableArgument
{
	private List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
	private boolean wildcardsPresent = false;
	
	public SourceTableArgument(String argument, WbConnection dbConn)
		throws SQLException
	{
		if (StringUtil.isEmptyString(argument)) return;
		if (dbConn == null) return;
		
		List<String> args = getObjectNames(argument);
		int argCount = args.size();

		if (argCount <= 0) return;
		
		for (String t : args)
		{
			if (t.indexOf('*') > -1 || t.indexOf('%') > -1)
			{
				this.wildcardsPresent = true;
				TableIdentifier tbl = new TableIdentifier(t);
				if (tbl.getSchema() == null)
				{
					tbl.setSchema(dbConn.getMetadata().getSchemaToUse());
				}
				tbl.adjustCase(dbConn);
				List<TableIdentifier> l = dbConn.getMetadata().getTableList(tbl.getTableName(), tbl.getSchema());
				this.tables.addAll(l);
			}
			else
			{
				tables.add(new TableIdentifier(t));
			}
		}
	}


	/**
	 * Returns all DB Object names from the comma separated list.
	 * This is different to stringToList() as it keeps any quotes that 
	 * are present in the list.
	 * 
	 * @param list a comma separated list of elements (optionally with quotes)
	 * @return a List of Strings as defined by the input string
	 */
	public List<String> getObjectNames(String list)
	{
		if (StringUtil.isEmptyString(list)) return Collections.emptyList();
		WbStringTokenizer tok = new WbStringTokenizer(list, ",");
		tok.setDelimiterNeedsWhitspace(false);
		tok.setCheckBrackets(false);
		tok.setKeepQuotes(true);
		List<String> result = new LinkedList<String>();
		while (tok.hasMoreTokens())
		{
			String element = tok.nextToken();
			if (element == null) continue;
			element = element.trim();
			if (element.length() > 0)
			{
				result.add(element);
			}
		}
		return result;
	}
	
	public List<TableIdentifier> getTables()
	{
		return this.tables;
	}

	public boolean wasWildCardArgument() 
	{
		return this.wildcardsPresent;
	}
}
