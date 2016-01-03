/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.h2database;

import workbench.TestUtil;
import workbench.WbTestCase;

import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class H2SequenceAdjusterTest
	extends WbTestCase
{
	public H2SequenceAdjusterTest()
	{
		super("SequenceSync");
	}

	@Test
	public void testIdentitySync()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbConnection con = util.getConnection("seq_test");

		TestUtil.executeScript(con,
			"create table table_one (id identity not null);\n" +
			"insert into table_one (id) values (1), (2), (7), (41);\n" +
			"commit;" );

		TableIdentifier tbl = con.getMetadata().findTable(new TableIdentifier("table_one"));

		H2SequenceAdjuster sync = new H2SequenceAdjuster();
		sync.adjustTableSequences(con, tbl, true);

		TestUtil.executeScript(con,
			"insert into table_one (id) values (default);\n" +
			"commit;" );

		Number value = (Number)TestUtil.getSingleQueryValue(con, "select max(id) from table_one");
		assertEquals(42, value.intValue());
	}

}
