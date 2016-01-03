/*
 * ObjectSourceSearcher.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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
package workbench.db.search;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;

import workbench.db.DbObject;
import workbench.db.ProcedureDefinition;
import workbench.db.ProcedureReader;
import workbench.db.TableIdentifier;
import workbench.db.TriggerDefinition;
import workbench.db.TriggerReader;
import workbench.db.TriggerReaderFactory;
import workbench.db.WbConnection;

import workbench.storage.RowActionMonitor;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 * A class to search the source code of database objects for a specific string.
 * <br/>
 * Only the sourcecode that can be retrieved through {@link workbench.db.DbMetadata} is searched.
 *
 * @author Thomas Kellerer
 */
public class ObjectSourceSearcher
{
	private List<String> schemas;
	private Set<String> types;
	private List<String> names;
	private WbConnection connection;

	private List<DbObject> searchResult;
	private RowActionMonitor monitor;
	private boolean cancelSearch;
	private boolean isRunning;
	private int numSearched;
	private Set<String> searchedObjects;

	public ObjectSourceSearcher(WbConnection con)
	{
		connection = con;
		schemas = CollectionUtil.arrayList();
		names = CollectionUtil.arrayList();
		types = CollectionUtil.caseInsensitiveSet("trigger", "procedure", "function", "view");
    if (con.getMetadata().supportsMaterializedViews())
    {
      types.add(con.getMetadata().getMViewTypeName().toLowerCase());
    }
		searchedObjects = CollectionUtil.caseInsensitiveSet();
	}

	public int getNumberOfObjectsSearched()
	{
		return numSearched;
	}

	public void setRowMonitor(RowActionMonitor mon)
	{
		monitor = mon;
	}

	public void cancelSearch()
	{
		cancelSearch = true;
	}

	public boolean isRunning()
	{
		return isRunning;
	}

	public int getSearchSchemaCount()
	{
		return schemas.size();
	}

	/**
	 * Sets the given types to the list of types to be searched
	 * <br/>
	 * This will override any previously defined search types.
	 * @param searchTypes
	 */
	public void setTypesToSearch(List<String> searchTypes)
	{
		if (CollectionUtil.isEmpty(searchTypes)) return;
		types.clear();
		types.addAll(searchTypes);
	}

	public void setSchemasToSearch(List<String> searchSchemas)
	{
		if (CollectionUtil.isEmpty(searchSchemas)) return;
		schemas.clear();
		for (String schema : searchSchemas)
		{
			String s = connection.getMetadata().adjustSchemaNameCase(schema);
			schemas.add(s);
		}
	}

	public void setNamesToSearch(List<String> searchNames)
	{
		if (CollectionUtil.isEmpty(searchNames)) return;
		names.clear();

		for (String name : searchNames)
		{
			String n = connection.getMetadata().adjustObjectnameCase(name);
			names.add(n);
		}
	}

	private boolean typeIncluded(String toCheck, Collection<String> types)
	{
		if (types.contains("%")) return true;
		if (types.contains("*")) return true;
		return types.contains(toCheck);
	}

	/**
	 * Searches all objects for the given search string(s).
	 *
	 * If multiple search strings are given, the parameter matchAll
	 * defines if they all of them have to match or at least one
	 *
	 * @param searchValues the patterns to be searched in all object sources. This can be a regular expression
	 * @param matchAll     if true all patterns must be found in a single source
	 * @param ignoreCase   if false, the patterns must match exactly
	 *
	 * @return a list of objects where the searchvalue has been found
	 */
	public synchronized List<DbObject> searchObjects(List<String> searchValues, boolean matchAll, boolean ignoreCase, boolean useRegex)
	{
		searchedObjects.clear();
		cancelSearch = false;
		isRunning = true;
		numSearched = 0;
		try
		{
			searchResult = CollectionUtil.sizedArrayList(50);

			if (CollectionUtil.isEmpty(schemas))
			{
				schemas = CollectionUtil.arrayList(connection.getCurrentSchema());
			}

			if (CollectionUtil.isEmpty(names))
			{
				names = CollectionUtil.arrayList("%");
			}

			Set<String> typesToRetrieve = CollectionUtil.caseInsensitiveSet();
			typesToRetrieve.addAll(types);

			if (typeIncluded("trigger", typesToRetrieve))
			{
				List<DbObject> trigger = retrieveTriggers();
				if (cancelSearch) return null;
				typesToRetrieve.remove("trigger");
				searchList(trigger, searchValues, matchAll, ignoreCase, useRegex);
			}

			if (cancelSearch) return null;

			if (typeIncluded("procedure", typesToRetrieve) || typeIncluded("function", typesToRetrieve))
			{
				List<DbObject> procs = retrieveProcedures();
				if (cancelSearch) return null;
				typesToRetrieve.remove("procedure");
				typesToRetrieve.remove("function");
				searchList(procs, searchValues, matchAll, ignoreCase, useRegex);
			}

			if (cancelSearch) return null;

			if (typesToRetrieve.size() > 0)
			{
				if (typesToRetrieve.contains("*") || typesToRetrieve.contains("%"))
				{
					typesToRetrieve.clear();
					typesToRetrieve.addAll(connection.getMetadata().getObjectTypes());
				}
				List<DbObject> objects = retrieveObjects(typesToRetrieve);
				searchList(objects, searchValues, matchAll, ignoreCase, useRegex);
			}
		}
		catch (SQLException sql)
		{
			LogMgr.logError("ObjectSourceSearcher.searchObjects()", "Error retrieving objects", sql);
		}
		finally
		{
			isRunning = false;
		}
		return searchResult;
	}

	private void searchList(List<DbObject> toSearch, List<String> searchValues, boolean matchAll, boolean ignoreCase, boolean useRegex)
	{
		if (monitor != null)
		{
			monitor.setMonitorType(RowActionMonitor.MONITOR_PROCESS);
		}
		int total = toSearch.size();
		int current = 1;

		for (DbObject object : toSearch)
		{
			numSearched ++;
			if (cancelSearch) return;

			if (monitor != null)
			{
				monitor.setCurrentObject(object.getObjectName(), current, total);
			}

			try
			{
				CharSequence source = null;
				if (connection.getMetadata().isTableType(object.getObjectType()))
				{
					((TableIdentifier)object).setRetrieveFkSource(true);
				}

				ProcedureDefinition def = null;
				if (object instanceof ProcedureDefinition)
				{
					def = (ProcedureDefinition)object;
				}

				String key = getObjectKey(object);
				if (!searchedObjects.contains(key))
				{
					source = object.getSource(connection);
					if (StringUtil.isBlank(source))
					{
						LogMgr.logWarning("ObjectSourceSearcher.searchObjects()", "Empty source returned for " + object.toString());
					}

					if (StringUtil.containsWords(source, searchValues, matchAll, ignoreCase, useRegex))
					{
						searchResult.add(object);
					}
					searchedObjects.add(key);
				}
			}
			catch (SQLException sql)
			{
				LogMgr.logError("ObjectSourceSearcher.searchObjects()", "Error retrieving object source", sql);
			}
			current ++;
		}
	}

	private List<DbObject> retrieveProcedures()
		throws SQLException
	{
		if (this.monitor != null)
		{
			monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			monitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingProcedures"), -1, -1);
		}
		List<DbObject> result = CollectionUtil.sizedArrayList(50);
		ProcedureReader reader = connection.getMetadata().getProcedureReader();
		if (reader == null) return result;

		for (String schema : schemas)
		{
			for (String name : names)
			{
				if (cancelSearch) return null;
				List<ProcedureDefinition> procs = reader.getProcedureList(null, schema, name);
				result.addAll(procs);
			}
		}
		return result;
	}

	private List<DbObject> retrieveTriggers()
		throws SQLException
	{
		if (this.monitor != null)
		{
			monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			monitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingTriggers"), -1, -1);
		}
		TriggerReader trgReader = TriggerReaderFactory.createReader(connection);
		List<DbObject> result = CollectionUtil.sizedArrayList(50);
		for (String schema : schemas)
		{
			if (cancelSearch) return null;
			List<TriggerDefinition> triggers = trgReader.getTriggerList(null, schema, null);
			result.addAll(triggers);
		}
		return result;
	}

	private List<DbObject> retrieveObjects(Set<String> types)
		throws SQLException
	{
		if (this.monitor != null)
		{
			monitor.setMonitorType(RowActionMonitor.MONITOR_PLAIN);
			monitor.setCurrentObject(ResourceMgr.getString("MsgRetrievingTables"), -1, -1);
		}
		List<DbObject> result = CollectionUtil.sizedArrayList(50);

		String[] typeNames = new String[types.size()];
		int i=0;
		for (String type : types)
		{
			if (type != null)
			{
				// the JDBC calls all use upper-case type names, even
				// if the DBMS stores them lower case
				typeNames[i] = type.toUpperCase();
				i++;
			}
		}

		for (String schema : schemas)
		{
			for (String name : names)
			{
				if (cancelSearch) return null;
				List<TableIdentifier> objects = connection.getMetadata().getObjectList(name, schema, typeNames);
				result.addAll(objects);
			}
		}
		return result;
	}

	private String getObjectKey(DbObject def)
	{
		if (def instanceof ProcedureDefinition)
		{
			ProcedureDefinition proc = (ProcedureDefinition)def;
			if (proc.isPackageProcedure())
			{
				return proc.getSchema() + "." + proc.getPackageName();
			}
		}
		return def.getObjectNameForDrop(connection);
	}


}
