/*
 * ImportStringVerifierTest.java
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
package workbench.gui.sql;

import java.sql.Types;
import workbench.db.ColumnIdentifier;
import workbench.storage.ResultInfo;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class ImportStringVerifierTest
	extends WbTestCase
{

	public ImportStringVerifierTest()
	{
		super();
		prepare();
	}

	@Test
	public void testCheckData()
	{
		String data = "id\tfirstname\tlastname\n1\tArthur\tDent";
		ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
		ColumnIdentifier fname = new ColumnIdentifier("FIRSTNAME", Types.VARCHAR);
		ColumnIdentifier lname = new ColumnIdentifier("LASTNAME", Types.VARCHAR);
		ResultInfo info = new ResultInfo(new ColumnIdentifier[]
			{
				id, lname, fname
			});
		ImportStringVerifier v = new ImportStringVerifier(data, info);
		assertTrue(v.checkData());

		data = "1\tArthur\tDent";
		v = new ImportStringVerifier(data, info);
		// If the number of columns matches, it is assumed the data is "OK"
		assertTrue(v.checkData());

		data = "Arthur\tDent";
		v = new ImportStringVerifier(data, info);
		assertFalse(v.checkData());

	}
}
