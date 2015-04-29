/*
 * DriverInfo.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import workbench.log.LogMgr;

import workbench.storage.DataStore;

import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DriverInfo
{
	private Map<String, String> infoMap = new HashMap<>(30);
	private final Set<String> methods = new TreeSet<>();

	public DriverInfo(Connection con)
	{
		methods.add("getSearchStringEscape");
		methods.add("getCatalogSeparator");
		methods.add("getExtraNameCharacters");
		methods.add("getIdentifierQuoteString");
		methods.add("getProcedureTerm");
		methods.add("getSchemaTerm");
		methods.add("getCatalogTerm");
		methods.add("allProceduresAreCallable");
		methods.add("allTablesAreSelectable");
		methods.add("dataDefinitionCausesTransactionCommit");
		methods.add("dataDefinitionIgnoredInTransactions");
		methods.add("nullPlusNonNullIsNull");
		methods.add("nullsAreSortedAtEnd");
		methods.add("nullsAreSortedAtStart");
		methods.add("nullsAreSortedAtEnd");
		methods.add("getMaxCharLiteralLength");
		methods.add("getMaxColumnNameLength");
		methods.add("getMaxColumnsInIndex");
		methods.add("getMaxTableNameLength");
		methods.add("getMaxTablesInSelect");
		methods.add("getMaxStatementLength");
		methods.add("storesLowerCaseIdentifiers");
		methods.add("storesUpperCaseIdentifiers");
		methods.add("storesMixedCaseIdentifiers");
		methods.add("storesMixedCaseQuotedIdentifiers");
		methods.add("storesLowerCaseQuotedIdentifiers");
		methods.add("storesUpperCaseQuotedIdentifiers");
		methods.add("supportsAlterTableWithAddColumn");
		methods.add("supportsAlterTableWithDropColumn");
		methods.add("supportsANSI92EntryLevelSQL");
		methods.add("supportsANSI92FullSQL");
		methods.add("supportsANSI92IntermediateSQL");
		methods.add("supportsBatchUpdates");
		methods.add("supportsColumnAliasing");
		methods.add("supportsCorrelatedSubqueries");
		methods.add("supportsCoreSQLGrammar");
		methods.add("supportsDataDefinitionAndDataManipulationTransactions");
		methods.add("supportsDataManipulationTransactionsOnly");
		methods.add("supportsFullOuterJoins");
		methods.add("supportsGetGeneratedKeys");
		methods.add("supportsLikeEscapeClause");
		methods.add("supportsMultipleResultSets");
		methods.add("supportsMultipleTransactions");
		methods.add("supportsMixedCaseIdentifiers");
		methods.add("supportsMixedCaseQuotedIdentifiers");
		methods.add("supportsSavepoints");
		methods.add("supportsSchemasInTableDefinitions");
		methods.add("supportsSchemasInDataManipulation");
		methods.add("supportsSchemasInIndexDefinitions");
		methods.add("supportsSchemasInProcedureCalls");
		methods.add("supportsCatalogsInTableDefinitions");
		methods.add("supportsCatalogsInDataManipulation");
		methods.add("supportsCatalogsInIndexDefinitions");
		methods.add("supportsCatalogsInProcedureCalls");
		methods.add("supportsStoredProcedures");
		methods.add("supportsSubqueriesInComparisons");
		methods.add("supportsSubqueriesInExists");
		methods.add("supportsSubqueriesInIns");
		methods.add("supportsTableCorrelationNames");
		methods.add("supportsTransactions");
		methods.add("supportsUnion");
		methods.add("supportsUnionAll");
		fillMap(con);
	}

	private void fillMap(Connection con)
	{
		DatabaseMetaData metaData = null;
		try
		{
			metaData = con.getMetaData();
			int level = metaData.getDefaultTransactionIsolation();
			infoMap.put("defaultIsolationLevel", SqlUtil.getIsolationLevelName(level));
		}
		catch (SQLException sql)
		{
			LogMgr.logError("DriverInfo.fillMap()", "Could not obtain MetaData", sql);
		}

		if (metaData == null) return;

		for (String method : methods)
		{
			String value = getValue(metaData, method);
			if (method.startsWith("get"))
			{
				infoMap.put(Character.toLowerCase(method.charAt(3)) + method.substring(4), value);
			}
			else
			{
				infoMap.put(method, value);
			}
		}

	}

	public Map<String, String> getInfoMap()
	{
		return Collections.unmodifiableMap(infoMap);
	}

	public DataStore getInfo()
	{
		DataStore data = new DataStore(new String[] { "FEATURE", "VALUE"}, new int[] { Types.VARCHAR, Types.VARCHAR} );
		for (Map.Entry<String, String> entry : infoMap.entrySet())
		{
			int row = data.addRow();
			data.setValue(row, 0, entry.getKey());
			data.setValue(row, 1, entry.getValue());
		}
		data.sortByColumn(0, true);
		return data;
	}

	private String getValue(DatabaseMetaData meta, String methodName)
	{
		if (meta == null) return "n/a";

		try
		{
			Method method = meta.getClass().getMethod(methodName);
			Object result = method.invoke(meta);
			if (result == null)
			{
				return "";
			}
			return result.toString();
		}
		catch (Throwable th)
		{
			LogMgr.logDebug("DriverInfo.getValue()", "Could not retrieve property: " + methodName, th);
			return "n/a";
		}
	}
}
