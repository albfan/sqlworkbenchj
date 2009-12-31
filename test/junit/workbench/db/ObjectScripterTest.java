/*
 * ObjectScripterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db;

import java.util.ArrayList;
import java.util.List;
import workbench.TestUtil;
import workbench.WbTestCase;

/**
 * @author Thomas Kellerer
 */
public class ObjectScripterTest
	extends WbTestCase
{
	public ObjectScripterTest(String testName)
	{
		super(testName);
	}

	private void setupDatabase(WbConnection conn)
	{
		try
		{
			String script =
				"create sequence test_sequence;\n" +
				"create table person (id integer primary key, firstname varchar(50), lastname varchar(50)); \n" +
				"create table address (id integer primary key, person_id integer not null, address_info varchar(500)); \n" +
				"alter table address add constraint fk_adr_per foreign key (person_id) references person(id); \n" +
				"create view v_person as select * from person\n;";

			TestUtil.executeScript(conn, script);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	public void testGenerateScript()
	{

		try
		{
			TestUtil util = getTestUtil();
			WbConnection con = util.getConnection();
			setupDatabase(con);
			List<DbObject> objects = new ArrayList<DbObject>();

			objects.add(new SequenceDefinition(null, "TEST_SEQUENCE"));
			TableIdentifier tbl = new TableIdentifier("PERSON");
			tbl.setType("TABLE");
			objects.add(tbl);

			tbl = new TableIdentifier("ADDRESS");
			tbl.setType("TABLE");
			objects.add(tbl);

			objects.add(new ViewDefinition("V_PERSON"));
			ObjectScripter scripter = new ObjectScripter(objects, con);
			String script = scripter.getScript();
//			System.out.println(script);

			int personPos = script.indexOf("CREATE TABLE PERSON");
			assertTrue(personPos > -1);

			int addressPos = script.indexOf("CREATE TABLE ADDRESS");
			assertTrue(addressPos > -1);

			int seqPos = script.indexOf("CREATE SEQUENCE TEST_");
			assertTrue(seqPos > -1);

			assertTrue(personPos > seqPos);
			assertTrue(addressPos > seqPos);

			int fkPos = script.indexOf("CONSTRAINT FK_ADR_PER");
			assertTrue(fkPos > personPos);
			assertTrue(fkPos > addressPos);

			int viewPos = script.indexOf("CREATE FORCE VIEW");
			assertTrue(viewPos > -1);
			assertTrue(viewPos > personPos);
			assertTrue(viewPos > addressPos);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
