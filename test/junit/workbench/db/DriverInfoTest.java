/*
 * DriverInfoTest
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db;

import java.util.HashMap;
import java.util.Map;
import org.junit.*;
import static org.junit.Assert.*;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class DriverInfoTest
	extends WbTestCase
{

	public DriverInfoTest()
	{
		super("DriverInfo");
	}

	@Test
	public void testGetInfoMap()
		throws Exception
	{
		WbConnection con = getTestUtil().getConnection();
		DriverInfo info = new DriverInfo(con.getSqlConnection());
		Map<String, String> infoMap = info.getInfoMap();
		Map<String, String> expected = new HashMap<String, String>();
		expected.put("defaultIsolationLevel", "READ COMMITTED");
		expected.put("maxTableNameLength","0");
		expected.put("maxTableNameLength","0");
		expected.put("maxTablesInSelect", "0");
		expected.put("supportsUnion", "true");
		expected.put("supportsSavepoints", "true");
		expected.put("maxCharLiteralLength", "0");
		expected.put("storesMixedCaseQuotedIdentifiers", "true");
		expected.put("supportsTransactions", "true");
		expected.put("supportsANSI92FullSQL", "false");
		expected.put("allProceduresAreCallable", "true");
		expected.put("supportsSubqueriesInExists", "true");
		expected.put("supportsGetGeneratedKeys", "true");
		expected.put("dataDefinitionIgnoredInTransactions", "false");
		expected.put("nullsAreSortedAtStart", "false");
		expected.put("storesUpperCaseQuotedIdentifiers", "false");
		expected.put("supportsTableCorrelationNames", "true");
		expected.put("supportsANSI92EntryLevelSQL", "true");
		expected.put("supportsDataDefinitionAndDataManipulationTransactions", "false");
		expected.put("supportsCorrelatedSubqueries", "true");
		expected.put("dataDefinitionCausesTransactionCommit", "true");
		expected.put("storesUpperCaseIdentifiers", "true");
		expected.put("nullsAreSortedAtEnd", "false");
		expected.put("storesLowerCaseIdentifiers", "false");
		expected.put("supportsSubqueriesInComparisons", "true");
		expected.put("searchStringEscape", "\\");
		expected.put("catalogSeparator", ".");
		expected.put("supportsCoreSQLGrammar", "true");
		expected.put("supportsLikeEscapeClause", "true");
		expected.put("supportsMultipleResultSets", "false");
		expected.put("supportsMultipleTransactions", "true");
		expected.put("supportsColumnAliasing", "true");
		expected.put("supportsMixedCaseIdentifiers", "false");
		expected.put("supportsANSI92IntermediateSQL", "false");
		expected.put("supportsUnionAll", "true");
		expected.put("supportsMixedCaseQuotedIdentifiers", "true");
		expected.put("schemaTerm", "schema");
		expected.put("extraNameCharacters", "");
		expected.put("supportsStoredProcedures", "false");
		expected.put("maxColumnsInIndex", "0");
		expected.put("storesLowerCaseQuotedIdentifiers", "false");
		expected.put("identifierQuoteString", "\"");
		expected.put("supportsAlterTableWithAddColumn", "true");
		expected.put("allTablesAreSelectable", "true");
		expected.put("maxColumnNameLength", "0");
		expected.put("procedureTerm", "procedure");
		expected.put("supportsDataManipulationTransactionsOnly", "true");
		expected.put("storesMixedCaseIdentifiers", "false");
		expected.put("nullPlusNonNullIsNull", "true");
		expected.put("supportsFullOuterJoins", "false");
		expected.put("supportsSubqueriesInIns", "true");
		expected.put("supportsBatchUpdates", "true");
//		System.out.println(infoMap + "\n-----------\n" + expected);
		assertEquals(expected, infoMap);
	}
}
