/*
 * SourceTableArgument.java
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
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.DbSearchPath;
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
	private List<String> missingTables = new ArrayList<>();
	private List<TableIdentifier> tables = new ArrayList<>();
	private boolean wildcardsPresent;
	private boolean schemaAsCatalog;

	public SourceTableArgument(String includeTables, WbConnection dbConn)
		throws SQLException
	{
		if (dbConn == null) return;
		schemaAsCatalog = !dbConn.getDbSettings().supportsSchemas();
		initTableList(includeTables, null, null, dbConn.getMetadata().getTableTypesArray(), dbConn);
	}

	/**
	 *
	 * @param includeTables the parameter value to include tables
	 * @param excludeTables the parameter value to exclude tables
	 * @param schema        the schema to look in
	 * @param dbConn        the connection to use
	 * <p/>
	 * @throws SQLException
	 */
	public SourceTableArgument(String includeTables, String excludeTables, String schema, WbConnection dbConn)
		throws SQLException
	{
		if (dbConn == null) return;

		String[] types = dbConn.getMetadata().getTablesAndViewTypes();
		schemaAsCatalog = !dbConn.getDbSettings().supportsSchemas();
		initTableList(includeTables, excludeTables, schema, types, dbConn);
	}

	/**
	 *
	 * @param includeTables  the tables to include may be null if a schema name is supplied
	 * @param excludeTables  tablename to exclude
	 * @param schema         the schema to use. May be null if table names are supplied
	 *                       if no table names are supplied, "%" or "*" can be used to return all tables from all schemas
	 * @param types          the object types to retrieve
	 * @param dbConn         the connection
	 *
	 * @throws SQLException
	 */
	public SourceTableArgument(String includeTables, String excludeTables, String schema, String[] types, WbConnection dbConn)
		throws SQLException
	{
		if (StringUtil.isEmptyString(includeTables) && StringUtil.isEmptyString(schema)) return;
		if (dbConn == null) return;
		schemaAsCatalog = !dbConn.getDbSettings().supportsSchemas();

		initTableList(includeTables, excludeTables, schema, types, dbConn);
	}

	private void initTableList(String includeTables, String excludeTables, String schema, String[] types, WbConnection dbConn)
		throws SQLException
	{
		missingTables.clear();
		List<TableIdentifier> toAdd = retrieveObjects(includeTables, schema, true, types, dbConn);
		tables.addAll(toAdd);

		if (StringUtil.isNonBlank(excludeTables))
		{
			List<TableIdentifier> toRemove = retrieveObjects(excludeTables, schema, false, null, dbConn);
			tables.removeAll(toRemove);
		}
	}

	public static String[] parseTypes(String types, WbConnection conn)
	{
		if (StringUtil.isBlank(types)) return conn.getMetadata().getSelectableTypes();

		if ("%".equals(types) || "*".equals(types)) return null;

		List<String> typeList = StringUtil.stringToList(types.toUpperCase());

		if (typeList.isEmpty()) return conn.getMetadata().getSelectableTypes();

		String[] result = new String[typeList.size()];

		return typeList.toArray(result);
	}

	private List<TableIdentifier> retrieveObjects(String arg, String schema, boolean checkWildcard, String[] types, WbConnection dbConn)
		throws SQLException
	{
		List<String> args = getObjectNames(arg);

		List<TableIdentifier> result = CollectionUtil.arrayList();

		if (args.size() <= 0 && StringUtil.isBlank(schema)) return result;

    boolean useSearchPath = DbSearchPath.Factory.getSearchPathHandler(dbConn).isRealSearchPath() && StringUtil.isBlank(schema);

		String schemaToUse;

    if (useSearchPath)
    {
      schemaToUse = null;
    }
    else if (StringUtil.isBlank(schema))
		{
      if (schemaAsCatalog)
      {
        schemaToUse = dbConn.getMetadata().getCurrentCatalog();
      }
      else
      {
        schemaToUse = dbConn.getMetadata().getCurrentSchema();
      }
		}
		else if (schema.equals("*") || schema.equals("%"))
		{
			// all tables from all schemas
			schemaToUse = null;
			args = CollectionUtil.arrayList("*");
		}
		else
		{
			schemaToUse =  dbConn.getMetadata().adjustSchemaNameCase(schema);
		}

		if (args.isEmpty() && schemaToUse != null)
		{
      // find all objects of the given schema
			List<TableIdentifier> l = null;

			if (schemaAsCatalog)
			{
				l = dbConn.getMetadata().getObjectList(null, schemaToUse, null, types);
			}
			else
			{
				l = dbConn.getMetadata().getObjectList(null, null, schemaToUse, types);
			}
			result.addAll(l);
		}
		else
		{
			for (String searchName : args)
			{
				if (searchName.indexOf('*') > -1 || searchName.indexOf('%') > -1)
				{
					if (checkWildcard) this.wildcardsPresent = true;
          
					TableIdentifier tbl = new TableIdentifier(searchName);
          adjustTableSchema(tbl, schemaToUse);
					tbl.adjustCase(dbConn);
					List<TableIdentifier> l = null;
					if (schemaAsCatalog)
					{
						l = dbConn.getMetadata().getObjectList(tbl.getTableName(), tbl.getSchema(), null, types);
					}
					else
					{
						l = dbConn.getMetadata().getObjectList(tbl.getTableName(), null, tbl.getSchema(), types);
					}
					result.addAll(l);
				}
				else
				{
					TableIdentifier toSearch = new TableIdentifier(searchName, dbConn);
          adjustTableSchema(toSearch, schemaToUse);
					TableIdentifier tbl = dbConn.getMetadata().searchObjectOnPath(toSearch, types);

					if (tbl != null)
					{
						result.add(tbl);
					}
					else
					{
						missingTables.add(searchName);
						LogMgr.logDebug("SourceTableArgument.retrieveObjects()", "Table " + searchName + " not found!");
					}
				}
			}
		}
		return result;
	}

  private void adjustTableSchema(TableIdentifier tbl, String schemaToUse)
  {
    if (tbl.getSchema() == null && StringUtil.isNonEmpty(schemaToUse))
    {
      if (schemaAsCatalog)
      {
        tbl.setCatalog(schemaToUse);
      }
      else
      {
        tbl.setSchema(schemaToUse);
      }
    }
  }

  /**
   * If multiple tables have been specified, return those names that could not be found.
   *
   * @return  the list of names that could not be found. Never null.
   */
	public List<String> getMissingTables()
	{
		return missingTables;
	}

	/**
	 * Returns all DB Object names from the comma separated list.
	 * This is different to {@link StringUtil#stringToList(String)} as it keeps any quotes that
	 * are present in the list.
	 *
	 * @param list a comma separated list of elements (optionally with quotes)
	 * @return a List of Strings as defined by the input string
	 * @see StringUtil#stringToList(java.lang.String)
	 */
	List<String> getObjectNames(String list)
	{
		if (StringUtil.isEmptyString(list)) return Collections.emptyList();
		WbStringTokenizer tok = new WbStringTokenizer(list, ",");
		tok.setDelimiterNeedsWhitspace(false);
		tok.setCheckBrackets(false);
		tok.setKeepQuotes(true);
		List<String> result = new LinkedList<>();
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

	public boolean wasWildcardArgument()
	{
		return this.wildcardsPresent;
	}
}
