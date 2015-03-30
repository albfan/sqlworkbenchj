/*
 * JsonRowDataConverterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.exporter;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Date;

import workbench.WbTestCase;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;

import workbench.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class JsonRowDataConverterTest
	extends WbTestCase
{
	public JsonRowDataConverterTest()
	{
		super("JsonRowDataConverterTest");
	}

	@Test
	public void testConvertRowData()
		throws Exception
	{
		ColumnIdentifier id = new ColumnIdentifier("id", Types.INTEGER, true);
		id.setPosition(1);

		ColumnIdentifier fname = new ColumnIdentifier("firstname", Types.VARCHAR);
		fname.setPosition(2);

		ColumnIdentifier lname = new ColumnIdentifier("lastname", Types.VARCHAR);
		lname.setPosition(3);

		ColumnIdentifier lastLogin = new ColumnIdentifier("last_login", Types.TIMESTAMP);
		lname.setPosition(4);

		ColumnIdentifier salaray = new ColumnIdentifier("salary", Types.DECIMAL);
		salaray.setPosition(5);

		ResultInfo info = new ResultInfo(new ColumnIdentifier[] { id, fname, lname, lastLogin, salaray });
		info.setUpdateTable(new TableIdentifier("PERSON"));
		JsonRowDataConverter converter = new JsonRowDataConverter();
		converter.setResultInfo(info);

		Date login = StringUtil.getIsoTimestampFormatter().parse("2013-01-12 14:56:12.000");

		RowData data = new RowData(info);
		data.setValue(0, new Integer(1));
		data.setValue(1, "Arthur");
		data.setValue(2, "Dent");
		data.setValue(3, new java.sql.Timestamp(login.getTime()));
		data.setValue(4, new BigDecimal("42.24"));

		String result = converter.getStart().toString();
		result += converter.convertRowData(data, 0).toString();

		data.setValue(0, new Integer(2));
		data.setValue(1, "Ford");
		data.setValue(2, "\"Prefect\"");
		data.setValue(3, null);
		data.setValue(4, new BigDecimal("24.42"));

		result += converter.convertRowData(data, 1).toString();
		result += converter.getEnd(2).toString();

		String expected =
			"{\n" +
			"  \"person\":\n" +
			"  [\n" +
			"    {\"id\": \"1\", \"firstname\": \"Arthur\", \"lastname\": \"Dent\", \"last_login\": \"2013-01-12 14:56:12.000\", \"salary\": \"42.24\"},\n" +
			"    {\"id\": \"2\", \"firstname\": \"Ford\", \"lastname\": \"\\\"Prefect\\\"\", \"last_login\": null, \"salary\": \"24.42\"}\n" +
			"  ]\n" +
			"}";
		assertEquals(expected, result);
	}

}