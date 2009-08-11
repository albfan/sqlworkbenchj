/*
 * SourceTableArgument.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
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
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.WbStringTokenizer;

/**
 * Evaluate table arguments that may contain wildcards.
 *
 * @author Thomas Kellerer
 */
public class SourceTableArgument
{
	private List<TableIdentifier> tables = new ArrayList<TableIdentifier>();
	private boolean wildcardsPresent;

	public SourceTableArgument(String includeTables, WbConnection dbConn)
		throws SQLException
	{
		this(includeTables, null, dbConn);
	}

	public SourceTableArgument(String includeTables, String excludeTables, WbConnection dbConn)
		throws SQLException
	{
		if (StringUtil.isEmptyString(includeTables)) return;
		if (dbConn == null) return;

		tables.addAll(parseArgument(includeTables, true, dbConn));
		
		if (StringUtil.isNonBlank(excludeTables))
		{
			tables.removeAll(parseArgument(excludeTables, false, dbConn));
		}
	}

	private List<TableIdentifier> parseArgument(String arg, boolean checkWildcard, WbConnection dbConn)
		throws SQLException
	{
		List<String> args = getObjectNames(arg);
		int argCount = args.size();

		List<TableIdentifier> result = CollectionUtil.arrayList();
		
		if (argCount <= 0) return result;

		for (String t : args)
		{
			if (t.indexOf('*') > -1 || t.indexOf('%') > -1)
			{
				if (checkWildcard) this.wildcardsPresent = true;
				TableIdentifier tbl = new TableIdentifier(t);
				if (tbl.getSchema() == null)
				{
					tbl.setSchema(dbConn.getMetadata().getSchemaToUse());
				}
				tbl.adjustCase(dbConn);
				List<TableIdentifier> l = dbConn.getMetadata().getSelectableObjectsList(tbl.getTableName(), tbl.getSchema());
				result.addAll(l);
			}
			else
			{
				result.add(new TableIdentifier(t));
			}
		}
		return result;
	}
	
	/**
	 * Returns all DB Object names from the comma separated list.
	 * This is different to stringToList() as it keeps any quotes that
	 * are present in the list.
	 *
	 * @param list a comma separated list of elements (optionally with quotes)
	 * @return a List of Strings as defined by the input string
	 */
	List<String> getObjectNames(String list)
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
