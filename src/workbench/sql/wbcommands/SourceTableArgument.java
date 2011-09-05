/*
 * SourceTableArgument.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
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
import workbench.log.LogMgr;
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
		if (dbConn == null) return;
		initTableList(includeTables, null, null, dbConn.getMetadata().getTableTypesArray(), dbConn);
	}

	public SourceTableArgument(String includeTables, String excludeTables, String types, WbConnection dbConn)
		throws SQLException
	{
		this(includeTables, excludeTables, null, types, dbConn);
	}

	/**
	 *
	 * @param includeTables the parameter value to include tables
	 * @param excludeTables the parameter value to exclude tables
	 * @param types the parameter value for the table types
	 * @param dbConn the connection to use
	 * @throws SQLException
	 */
	public SourceTableArgument(String includeTables, String excludeTables, String schema, String[] types, WbConnection dbConn)
		throws SQLException
	{
		if (StringUtil.isEmptyString(includeTables)) return;
		if (dbConn == null) return;

		initTableList(includeTables, excludeTables, schema, types, dbConn);
	}

	public SourceTableArgument(String includeTables, String excludeTables, String schema, String types, WbConnection dbConn)
		throws SQLException
	{
		if (StringUtil.isEmptyString(includeTables)) return;
		if (dbConn == null) return;

		String[] typeList = parseTypes(types, dbConn);
		initTableList(includeTables, excludeTables, schema, typeList, dbConn);
	}

	private void initTableList(String includeTables, String excludeTables, String schema, String[] types, WbConnection dbConn)
		throws SQLException
	{
		tables.addAll(parseArgument(includeTables, schema, true, types, dbConn));

		if (StringUtil.isNonBlank(excludeTables))
		{
			List<TableIdentifier> toRemove = parseArgument(excludeTables, schema, false, null, dbConn);
			tables.removeAll(toRemove);
		}
	}

	private String[] parseTypes(String types, WbConnection conn)
	{
		if (StringUtil.isBlank(types)) return conn.getMetadata().getTableTypesArray();

		if ("%".equals(types) || "*".equals(types)) return null;

		List<String> typeList = StringUtil.stringToList(types.toUpperCase());

		if (typeList.isEmpty()) return conn.getMetadata().getTableTypesArray();

		String[] result = new String[typeList.size()];

		return typeList.toArray(result);
	}

	private List<TableIdentifier> parseArgument(String arg, String schema, boolean checkWildcard, String[] types, WbConnection dbConn)
		throws SQLException
	{
		List<String> args = getObjectNames(arg);
		int argCount = args.size();

		List<TableIdentifier> result = CollectionUtil.arrayList();

		if (argCount <= 0) return result;

		String schemaToUse = StringUtil.isBlank(schema) ? dbConn.getMetadata().getSchemaToUse() : schema;
		for (String t : args)
		{
			if (t.indexOf('*') > -1 || t.indexOf('%') > -1)
			{
				if (checkWildcard) this.wildcardsPresent = true;
				TableIdentifier tbl = new TableIdentifier(t);
				if (tbl.getSchema() == null && !(t.equals("*") || t.equals("%")))
				{
					tbl.setSchema(schemaToUse);
				}
				tbl.adjustCase(dbConn);
				List<TableIdentifier> l = dbConn.getMetadata().getObjectList(tbl.getTableName(), tbl.getSchema(), types, false);
				result.addAll(l);
			}
			else
			{
				TableIdentifier tbl = dbConn.getMetadata().findTable(new TableIdentifier(t));
				if (tbl != null)
				{
					result.add(tbl);
				}
				else
				{
					LogMgr.logDebug("SourceTableArgument.parseArgument()", "Table " + t + " not found!");
				}
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
