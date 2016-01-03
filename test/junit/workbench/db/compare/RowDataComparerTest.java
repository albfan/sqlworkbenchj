/*
 * RowDataComparerTest.java
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
package workbench.db.compare;

import java.sql.Types;

import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;

import workbench.storage.ResultInfo;
import workbench.storage.RowData;
import workbench.storage.StatementFactory;

import workbench.util.SqlUtil;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class RowDataComparerTest
	extends WbTestCase
{

	public RowDataComparerTest()
	{
		super("RowDataComparerTest");
	}

	@Test
	public void testGetMigrationSql()
	{
		boolean oldDel = Settings.getInstance().getDoFormatDeletes();
		boolean oldIns = Settings.getInstance().getDoFormatInserts();
		boolean oldUpd = Settings.getInstance().getDoFormatUpdates();

		try
		{
			Settings.getInstance().setDoFormatDeletes(false);
			Settings.getInstance().setDoFormatInserts(false);
			Settings.getInstance().setDoFormatUpdates(false);

			ColumnIdentifier[] cols = new ColumnIdentifier[3];
			cols[0] = new ColumnIdentifier("ID");
			cols[0].setIsPkColumn(true);
			cols[0].setIsNullable(false);

			cols[1] = new ColumnIdentifier("FIRSTNAME");
			cols[1].setIsPkColumn(false);
			cols[1].setIsNullable(false);

			cols[2] = new ColumnIdentifier("LASTNAME");
			cols[2].setIsPkColumn(false);
			cols[2].setIsNullable(false);

			ResultInfo info = new ResultInfo(cols);
			info.setUpdateTable(new TableIdentifier("PERSON"));

			StatementFactory factory = new StatementFactory(info, null);
			factory.setEmptyStringIsNull(true);
			factory.setIncludeNullInInsert(true);

			RowData reference = new RowData(info);
			reference.setValue(0, new Integer(42));
			reference.setValue(1, "Zaphod");
			reference.setValue(2, "Beeblebrox");
			reference.resetStatus();

			RowData target = new RowData(info);
			target.setValue(0, new Integer(42));
			target.setValue(1, "Arthur");
			target.setValue(2, "Beeblebrox");
			target.resetStatus();

			RowDataComparer instance = new RowDataComparer();
			instance.setTypeSql();
			instance.setRows(reference, target);
			instance.setConnection(null);
			instance.setResultInfo(info);

			String sql = instance.getMigration(1);
			String verb = SqlUtil.getSqlVerb(sql);
			assertEquals("UPDATE", verb);
			assertTrue(sql.indexOf("SET FIRSTNAME = 'Zaphod'") > -1);

			instance.setRows(reference, null);
			sql = instance.getMigration(1);
			verb = SqlUtil.getSqlVerb(sql);
			assertEquals("INSERT", verb);
			assertTrue(sql.indexOf("(42,'Zaphod','Beeblebrox')") > -1);

			reference = new RowData(info);
			reference.setValue(0, new Integer(42));
			reference.setValue(1, "Zaphod");
			reference.setValue(2, null);
			reference.resetStatus();

			target = new RowData(info);
			target.setValue(0, new Integer(42));
			target.setValue(1, "Zaphod");
			target.setValue(2, null);
			target.resetStatus();

			instance.setRows(reference, target);
			sql = instance.getMigration(1);
			assertNull(sql);
		}
		finally
		{
			Settings.getInstance().setDoFormatDeletes(oldDel);
			Settings.getInstance().setDoFormatInserts(oldIns);
			Settings.getInstance().setDoFormatUpdates(oldUpd);
		}
	}

	@Test
	public void testGetMigrationXml()
	{
		ColumnIdentifier[] cols = new ColumnIdentifier[3];
		cols[0] = new ColumnIdentifier("ID");
		cols[0].setIsPkColumn(true);
		cols[0].setIsNullable(false);

		cols[1] = new ColumnIdentifier("FIRSTNAME");
		cols[1].setIsPkColumn(false);
		cols[1].setIsNullable(false);

		cols[2] = new ColumnIdentifier("LASTNAME");
		cols[2].setIsPkColumn(false);
		cols[2].setIsNullable(false);

		ResultInfo info = new ResultInfo(cols);
		info.setUpdateTable(new TableIdentifier("PERSON"));

		StatementFactory factory = new StatementFactory(info, null);
		factory.setEmptyStringIsNull(true);
		factory.setIncludeNullInInsert(true);

		RowData reference = new RowData(info);
		reference.setValue(0, new Integer(42));
		reference.setValue(1, "Zaphod");
		reference.setValue(2, "Beeblebrox");
		reference.resetStatus();

		RowData target = new RowData(info);
		target.setValue(0, new Integer(42));
		target.setValue(1, "Arthur");
		target.setValue(2, "Beeblebrox");
		target.resetStatus();

		RowDataComparer instance = new RowDataComparer();
		instance.setTypeXml(false);
		instance.setRows(reference, target);
		instance.setConnection(null);
		instance.setResultInfo(info);

		String xml = instance.getMigration(1);
//		System.out.println(xml);
		assertTrue(xml.startsWith("<update>"));

		instance.setRows(reference, null);
		xml = instance.getMigration(1);
//		System.out.println(xml);
		assertTrue(xml.startsWith("<insert>"));

		reference = new RowData(info);
		reference.setValue(0, new Integer(42));
		reference.setValue(1, "Zaphod");
		reference.setValue(2, null);
		reference.resetStatus();

		target = new RowData(info);
		target.setValue(0, new Integer(42));
		target.setValue(1, "Zaphod");
		target.setValue(2, null);
		target.resetStatus();

		instance.setRows(reference, target);
		xml = instance.getMigration(1);
		assertNull(xml);
	}

	@Test
	public void testGetMigrationXml2()
	{
		ColumnIdentifier[] cols = new ColumnIdentifier[3];
		cols[0] = new ColumnIdentifier("ID");
		cols[0].setIsPkColumn(true);
		cols[0].setIsNullable(false);
		cols[0].setDataType(Types.INTEGER);
		cols[0].setDbmsType("integer");

		cols[1] = new ColumnIdentifier("SOME_DATA");
		cols[1].setIsPkColumn(false);
		cols[1].setIsNullable(false);
		cols[1].setDataType(Types.VARCHAR);
		cols[1].setDbmsType("varchar(100)");

		cols[2] = new ColumnIdentifier("SOME_MORE");
		cols[2].setIsPkColumn(false);
		cols[2].setIsNullable(false);
		cols[2].setDataType(Types.VARCHAR);
		cols[2].setDbmsType("varchar(100)");

		ResultInfo info1 = new ResultInfo(cols);
		info1.setUpdateTable(new TableIdentifier("FOO1"));

		ResultInfo info2 = new ResultInfo(cols);
		info2.setUpdateTable(new TableIdentifier("FOO2"));

		RowData reference = new RowData(info1);
		reference.setValue(0, new Integer(1));
		reference.setValue(1, "one");
		reference.setValue(2, "more");
		reference.resetStatus();

		RowData target = new RowData(info2);
		target.setValue(0, new Integer(1));
		target.setValue(1, "one-");
		target.setValue(2, "more");
		target.resetStatus();

		RowDataComparer instance = new RowDataComparer();
		instance.setTypeXml(true);
		instance.setConnection(null);
		instance.setResultInfo(info2);
		instance.setRows(reference, target);
		String xml = instance.getMigration(1);
		System.out.println(xml);
    // <update><col name="ID" pk="true">1</col><col name="SOME_DATA" modified="true"><![CDATA[one]]></col><col name="SOME_MORE"><![CDATA[more]]></col></update>
    String value = TestUtil.getXPathValue(xml, "/update/col[@name='SOME_DATA']/@modified");
    assertEquals("true", value);
    value = TestUtil.getXPathValue(xml, "/update/col[@name='SOME_MORE']/@modified");
    System.out.println("value: " + value);
    assertEquals("", value);
	}

}
