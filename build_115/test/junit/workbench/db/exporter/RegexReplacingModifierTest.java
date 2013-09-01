/*
 * RegexReplacingModifierTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.exporter;

import org.junit.Test;
import java.sql.Types;
import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;
import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.util.StrBuffer;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class RegexReplacingModifierTest
	extends WbTestCase
{

	public RegexReplacingModifierTest()
	{
		super("RegexReplacingModifierTest");
	}

	@Test
	public void testReplacePattern()
	{
		RegexReplacingModifier modifier = new RegexReplacingModifier("[\\n\\r]+", " ");
		
		ColumnIdentifier[] cols = new ColumnIdentifier[3];
		cols[0] = new ColumnIdentifier("id", Types.INTEGER, true);
		cols[1] = new ColumnIdentifier("name", Types.VARCHAR, false);
		cols[2] = new ColumnIdentifier("description", Types.VARCHAR, false);

		final ResultInfo info = new ResultInfo(cols);

		RowData row = new RowData(info);
		row.setValue(0, Integer.valueOf(42));
		row.setValue(1, "Arthur Dent");
		row.setValue(2, "This is\na multiline\ndescription");
		
		RowDataConverter converter = new RowDataConverter()
		{

			@Override
			public ResultInfo getResultInfo()
			{
				return info;
			}

			@Override
			public boolean includeColumnInExport(int col)
			{
				return true;
			}

			@Override
			public StrBuffer convertRowData(RowData row, long rowIndex)
			{
				return null;
			}

			@Override
			public StrBuffer getStart()
			{
				return null;
			}

			@Override
			public StrBuffer getEnd(long totalRows)
			{
				return null;
			}
		};
		modifier.modifyData(converter, row, 0);
		Integer i = (Integer)row.getValue(0);
		assertEquals(42, i.intValue());

		String name = (String)row.getValue(1);
		assertEquals("Arthur Dent", name);

		String descr = (String)row.getValue(2);
		assertEquals("This is a multiline description", descr);
	}
}
