/*
 * LiquibaseSupportTest
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2011, Thomas Kellerer
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
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author Thomas Kellerer
 */
public class LiquibaseSupportTest
	extends WbTestCase
{

	public LiquibaseSupportTest()
	{
		super("LiquibaseSupportTest");
	}

	@Test
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
             "    <changeSet id=\"1\" author=\"Arthur\"> \n" +
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
             "    <changeSet id=\"5\" author=\"Zaphod\"> \n" +
						 "         <createProcedure> \n" +
						 "            SELECT 'zaphod-5' FROM DUAL; \n" +
						 "        </createProcedure>\n" +
             "    </changeSet> \n" +
						 "\n" +
             "    <changeSet id=\"5\" author=\"Arthur\"> \n" +
						 "         <createProcedure> \n" +
						 "            SELECT 'arthur-5' FROM DUAL; \n" +
						 "        </createProcedure>\n" +
             "    </changeSet> \n" +
						 "\n" +
             "    <changeSet id=\"3\" author=\"Zaphod\"> \n" +
						 "         <createProcedure> \n" +
						 "            SELECT 3 FROM DUAL; \n" +
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
		List<String> sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("1"));
		assertNotNull(sql);
		assertEquals(1, sql.size());

		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("2"));
		assertEquals(3, sql.size());

		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("3"));
		assertNotNull(sql);
		assertEquals(1, sql.size());

		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("1"), new ChangeSetIdentifier("2"));
		assertNotNull(sql);
		assertEquals(4, sql.size());

		lb.setAlternateDelimiter(DelimiterDefinition.DEFAULT_MS_DELIMITER);
		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("4"));

		assertNotNull(sql);
		assertEquals(3, sql.size());

		lb.setAlternateDelimiter(null);
		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("5"));
		assertEquals(2, sql.size());

		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("Arthur;5"));
		assertEquals(1, sql.size());
		assertEquals("SELECT 'arthur-5' FROM DUAL;", sql.get(0).trim());

		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("3"), new ChangeSetIdentifier("Arthur;5"));
		assertEquals(2, sql.size());
		assertEquals("SELECT 'arthur-5' FROM DUAL;", sql.get(0).trim());
		assertEquals("SELECT 3 FROM DUAL;", sql.get(1).trim());

		sql = lb.getSQLFromChangeSet(new ChangeSetIdentifier("Arthur", "*"));
		assertEquals(2, sql.size());
	}

}
