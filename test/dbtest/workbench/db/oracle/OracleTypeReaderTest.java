/*
 * OracleTypeReaderTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.oracle;

import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.db.WbConnection;
import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptParser;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTypeReaderTest
	extends WbTestCase
{

	public OracleTypeReaderTest()
	{
		super("OracleTypeReaderTest");
	}

	@BeforeClass
	public static void setUp()
		throws Exception
	{
		OracleTestUtil.initTestCase();
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null)
		{
			return;
		}

		String sql =
			"CREATE TYPE address_type AS OBJECT (street varchar(100), city varchar(50), zipcode varchar(10));\n" +
			"/\n" +
			"\n "+
			"CREATE TYPE TYP1 AS OBJECT  \n" +
			"(  \n" +
			"   my_data NUMBER(16,2),  \n" +
			"   MEMBER FUNCTION get_value(add_to NUMBER) RETURN NUMBER  \n" +
			");\n" +
			"/\n" +
			"CREATE TYPE BODY TYP1 IS    \n" +
			"  MEMBER FUNCTION get_value(add_to NUMBER) RETURN NUMBER  IS  \n" +
			"  BEGIN  \n" +
			"     RETURN (my_data + add_to) \n" +
			"  END;  \n" +
			"END;  \n" +
			"/";
		TestUtil.executeScript(con, sql, DelimiterDefinition.DEFAULT_ORA_DELIMITER);
	}

	@AfterClass
	public static void tearDown()
		throws Exception
	{
		OracleTestUtil.cleanUpTestCase();
	}

	@Test
	public void testGetTypes()
		throws Exception
	{
		WbConnection con = OracleTestUtil.getOracleConnection();
		if (con == null)
		{
			System.out.println("No Oracle connection available. Skipping test");
			return;
		}
		OracleTypeReader reader = new OracleTypeReader();

		List<OracleObjectType> types = reader.getTypes(con, "WBJUNIT", null);
		assertNotNull(types);
		assertEquals(2, types.size());

		// List is sorted by name, so the first must be the address_type
		OracleObjectType address = types.get(0); 
		assertEquals("ADDRESS_TYPE", address.getObjectName());
		assertEquals(3, address.getNumberOfAttributes());
		assertEquals(0, address.getNumberOfMethods());

		OracleObjectType typ1 = types.get(1);
		assertEquals("TYP1", typ1.getObjectName());
		assertEquals(1, typ1.getNumberOfAttributes());
		assertEquals(1, typ1.getNumberOfMethods());
		String source = typ1.getSource(con).toString();
		ScriptParser p = new ScriptParser(source);
		p.setAlternateDelimiter(DelimiterDefinition.DEFAULT_ORA_DELIMITER);
		assertEquals(2, p.getSize());
	}

}
