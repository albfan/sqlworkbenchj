/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.parser;



import workbench.WbTestCase;

import workbench.sql.lexer.SQLToken;

import org.junit.Test;

import workbench.sql.DelimiterDefinition;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleDelimiterTesterTest
	extends WbTestCase
{

	public OracleDelimiterTesterTest()
	{
		super("OracleDelimiterTesterTest");
	}

	@Test
	public void testGetCurrentDelimiter()
	{
		OracleDelimiterTester tester = new OracleDelimiterTester();
		tester.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		SQLToken create = new SQLToken(SQLToken.RESERVED_WORD, "CREATE", 0, 0);
		tester.currentToken(create, true);
		DelimiterDefinition delim = tester.getCurrentDelimiter();
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, delim);

		SQLToken proc = new SQLToken(SQLToken.RESERVED_WORD, "PROCEDURE", 0, 0);
		tester.currentToken(proc, false);
		delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);

		SQLToken name = new SQLToken(SQLToken.IDENTIFIER, "foobar", 0, 0);
		tester.currentToken(name, false);
		delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);

		SQLToken t = new SQLToken(SQLToken.IDENTIFIER, "as", 0, 0);
		tester.currentToken(t, false);
		delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);

		tester.statementFinished();
		delim = tester.getCurrentDelimiter();
		assertEquals(DelimiterDefinition.STANDARD_DELIMITER, delim);
	}

	@Test
	public void testBlock()
	{
		OracleDelimiterTester tester = new OracleDelimiterTester();
		tester.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);

		SQLToken begin = new SQLToken(SQLToken.RESERVED_WORD, "BEGIN", 0, 0);
		tester.currentToken(begin, true);
		DelimiterDefinition delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);

		tester.statementFinished();

		SQLToken declare = new SQLToken(SQLToken.RESERVED_WORD, "DECLARE", 0, 0);
		tester.currentToken(declare, false);
		delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);

		tester.statementFinished();

		tester.currentToken(begin, false);
		delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);

		SQLToken select = new SQLToken(SQLToken.RESERVED_WORD, "SELECT", 0, 0);
		tester.currentToken(select, false);
		delim = tester.getCurrentDelimiter();
		assertEquals(tester.getAlternateDelimiter(), delim);
	}

}
