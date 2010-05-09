/*
 * LiquibaseSupportTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.liquibase;

import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.sql.DelimiterDefinition;
import workbench.util.WbFile;

/**
 *
 * @author Thomas Kellerer
 */
public class LiquibaseSupportTest
	extends WbTestCase
{

	public LiquibaseSupportTest(String testName)
	{
		super(testName);
	}

	public void testGetCustomSQL()
		throws Exception
	{
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" +
             "  \n" +
             "<databaseChangeLog \n" +
             "  xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog/1.9\" \n" +
             "  xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" +
             "  xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog/1.9 \n" +
             "         http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-1.9.xsd\"> \n" +
             "  \n" +
             "    <changeSet id=\"1\" author=\"arthur\"> \n" +
						 "         <sql splitStatements=\"false\"> \n" +
						 "            SELECT 42 FROM DUAL; \n" +
						 "            COMMIT;" +
						 "        </sql>\n" +
             "    </changeSet> \n" +
						 "\n" +
						 "    <changeSet id=\"2\" author=\"Tricia\"> \n" +
						 "       <createTable tableName=\"person\"> \n" +
						 "         <column name=\"id\" type=\"integer\"> \n" +
						 "           <constraint primaryKey=\"true\" nullable=\"false\"/> \n" +
						 "         </column> \n" +
						 "       </createTable> \n" +
						 "       <sql splitStatements=\"true\"> \n" +
						 "            INSERT INTO person (id) VALUES (1); \n" +
						 "            INSERT INTO person (id) VALUES (2); \n" +
						 "            COMMIT;" +
						 "        </sql>\n" +
             "    </changeSet> \n" +
						 "\n" +
             "    <changeSet id=\"3\" author=\"zaphod\"> \n" +
						 "         <createProcedure> \n" +
						 "            SELECT 2 FROM DUAL; \n" +
						 "        </createProcedure>\n" +
             "    </changeSet> \n" +
             "  \n" +
						 "    <changeSet id=\"4\" author=\"Ford\"> \n" +
						 "       <sql splitStatements=\"true\"> \n" +
						 "            INSERT INTO person (id) VALUES (1)\n" +
						 "            GO\n" +
						 "\n" +
						 "            INSERT INTO person (id) VALUES (2)\n" +
						 "            GO\n" +
						 "\n" +
						 "            COMMIT\n" +
						 "            GO\n" +
						 "\n" +
						 "        </sql>\n" +
             "    </changeSet> \n" +
						 "\n" +
             "</databaseChangeLog>";

		TestUtil util = getTestUtil();
		WbFile xmlFile = new WbFile(util.getBaseDir(), "changelog.xml");

		TestUtil.writeFile(xmlFile, xml, "UTF-8");
		LiquibaseSupport lb = new LiquibaseSupport(xmlFile);
		List<String> sql = lb.getSQLFromChangeSet("1", null);
		assertNotNull(sql);
		assertEquals(1, sql.size());

		sql = lb.getSQLFromChangeSet("2");
		assertEquals(3, sql.size());

		sql = lb.getSQLFromChangeSet("3");
		assertNotNull(sql);
		assertEquals(1, sql.size());

		sql = lb.getSQLFromChangeSet("1", "2");
		assertNotNull(sql);
		assertEquals(4, sql.size());

		lb.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		sql = lb.getSQLFromChangeSet("4");

		assertNotNull(sql);
		assertEquals(3, sql.size());

	}
}
