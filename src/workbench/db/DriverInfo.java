/*
 * DriverInfo
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class DriverInfo
{
	private Map<String, String> infoMap = new HashMap<String, String>(30);
	private List<String> methods = new ArrayList<String>(30);

	public DriverInfo(Connection con)
	{
		methods.add("getCatalogSeparator");
		methods.add("getExtraNameCharacters");
		methods.add("getIdentifierQuoteString");
		methods.add("getProcedureTerm");
		methods.add("getSchemaTerm");
		methods.add("getSchemaTerm");
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
		methods.add("getMaxColumnsInIndex");
		methods.add("getMaxTableNameLength");
		methods.add("getMaxTablesInSelect");
		methods.add("othersInsertsAreVisible");
		methods.add("othersInsertsAreVisible");
		methods.add("othersInsertsAreVisible");
		methods.add("ownDeletesAreVisible");
		methods.add("ownDeletesAreVisible");
		methods.add("ownUpdatesAreVisible");
		methods.add("storesLowerCaseIdentifiers");
		methods.add("storesUpperCaseIdentifiers");
		methods.add("storesMixedCaseIdentifiers");
		methods.add("storesMixedCaseQuotedIdentifiers");
		methods.add("storesLowerCaseQuotedIdentifiers");
		methods.add("storesUpperCaseQuotedIdentifiers");
		methods.add("supportsAlterTableWithAddColumn");
		methods.add("supportsANSI92EntryLevelSQL");
		methods.add("supportsANSI92FullSQL");
		methods.add("supportsANSI92IntermediateSQL");
		methods.add("supportsBatchUpdates");
		methods.add("supportsColumnAliasing");
		methods.add("supportsCorrelatedSubqueries");
		methods.add("supportsDataDefinitionAndDataManipulationTransactions");
		methods.add("supportsDataManipulationTransactionsOnly");
		methods.add("supportsFullOuterJoins");
		methods.add("supportsGetGeneratedKeys");
		methods.add("supportsMixedCaseIdentifiers");
		methods.add("supportsMixedCaseQuotedIdentifiers");
		methods.add("supportsSavepoints");
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
		}
		catch (SQLException sql)
		{
			LogMgr.logError("DriverInfo.fillMap()", "Could not obtain MetaData", sql);
		}
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
			return "n/a";
		}
	}
}
