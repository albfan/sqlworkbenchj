/*
 * ColumnDefinitionTemplateTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2012, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.sqltemplates;


import java.sql.Types;

import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;
import workbench.db.ColumnIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class ColumnDefinitionTemplateTest
	extends WbTestCase
{

	public ColumnDefinitionTemplateTest()
	{
		super("ColumnTemplate");
	}

	@Test
	public void testGetColumnDefinitionSQL()
	{
		ColumnIdentifier column = new ColumnIdentifier("id", Types.INTEGER, false);
		column.setDbmsType("INTEGER");
		column.setIsNullable(false);
		column.setDefaultValue("42");

		String colConstraint = "";

		ColumnDefinitionTemplate tmpl = new ColumnDefinitionTemplate();

		String templateSql =
			ColumnChanger.PARAM_DATATYPE + " " +
			ColumnChanger.PARAM_DEFAULT_VALUE + " " +
			ColumnDefinitionTemplate.PARAM_NOT_NULL + " " +
			ColumnDefinitionTemplate.PARAM_COL_CONSTRAINTS;

		tmpl.setTemplate(templateSql);

		String expResult = "INTEGER    DEFAULT 42 NOT NULL";
		String result = tmpl.getColumnDefinitionSQL(column, colConstraint, 10);
		assertEquals(expResult, result);

		column.setIsNullable(true);
		expResult = "INTEGER    DEFAULT 42";
		result = tmpl.getColumnDefinitionSQL(column, colConstraint, 10);
		assertEquals(expResult, result);

		column.setDefaultValue(null);
		expResult = "INTEGER";
		result = tmpl.getColumnDefinitionSQL(column, colConstraint, 10);
		assertEquals(expResult, result);

		column.setIsNullable(false);
		expResult = "INTEGER    NOT NULL";
		result = tmpl.getColumnDefinitionSQL(column, colConstraint, 10);
		assertEquals(expResult, result);
	}

	@Test
	public void testFixDefaultExpression()
	{
		ColumnDefinitionTemplate tmpl = new ColumnDefinitionTemplate();

		String templateSql =
			ColumnChanger.PARAM_DATATYPE + " " +
			ColumnChanger.PARAM_DEFAULT_VALUE + " " +
			ColumnDefinitionTemplate.PARAM_NOT_NULL + " " +
			ColumnDefinitionTemplate.PARAM_COL_CONSTRAINTS;

		tmpl.setTemplate(templateSql);

		ColumnIdentifier column = new ColumnIdentifier("first_name", Types.VARCHAR, false);
		column.setDbmsType("VARCHAR(50)");
		column.setColumnSize(50);
		column.setIsNullable(true);
		column.setDefaultValue("'Arthur'");

		tmpl.setFixDefaultValues(false);
		String expResult = "VARCHAR(50)  DEFAULT 'Arthur'";
		assertEquals(expResult, tmpl.getColumnDefinitionSQL(column, null, 12));

		tmpl.setFixDefaultValues(true);
		assertEquals(expResult, tmpl.getColumnDefinitionSQL(column, null, 12));

		column.setDefaultValue("Arthur");
		tmpl.setFixDefaultValues(false);
		expResult = "VARCHAR(50)  DEFAULT Arthur";
		assertEquals(expResult, tmpl.getColumnDefinitionSQL(column, null, 12));

		column.setDefaultValue("Arthur");
		tmpl.setFixDefaultValues(true);
		expResult = "VARCHAR(50)  DEFAULT 'Arthur'";
		assertEquals(expResult, tmpl.getColumnDefinitionSQL(column, null, 12));

	}

	@Test
	public void testMySQLWorkaround()
	{
		ColumnDefinitionTemplate tmpl = new ColumnDefinitionTemplate("mysql");

		ColumnIdentifier column = new ColumnIdentifier("id", Types.INTEGER, false);
		column.setDbmsType("INTEGER");
		column.setIsNullable(true);
		column.setDefaultValue("((42))");
		String coldef = tmpl.getColumnDefinitionSQL(column, null, 1);
		assertEquals("INTEGER DEFAULT 42", coldef);

		column.setDefaultValue("(0)");
		coldef = tmpl.getColumnDefinitionSQL(column, null, 1);
		assertEquals("INTEGER DEFAULT 0", coldef);

		column.setDefaultValue("42");
		coldef = tmpl.getColumnDefinitionSQL(column, null, 1);
		assertEquals("INTEGER DEFAULT 42", coldef);
	}
}
