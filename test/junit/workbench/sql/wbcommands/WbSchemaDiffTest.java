/*
 * WbSchemaDiffTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import workbench.TestUtil;
import workbench.db.WbConnection;
import workbench.sql.StatementRunnerResult;
import workbench.util.SqlUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbSchemaDiffTest 
	extends junit.framework.TestCase
{
	
	private WbConnection source;
	private WbConnection target;
	private TestUtil util;
	
	public WbSchemaDiffTest(String testName)
	{
		super(testName);
	}
	
	public void tearDown()
	{
		try { source.disconnect(); } catch (Throwable th) {}
		try { target.disconnect(); } catch (Throwable th) {}
	}
	
	public void testBaseDiff()
	{
		try
		{
			setupDatabase();
			WbSchemaDiff diff = new WbSchemaDiff();
			File output = new File(util.getBaseDir(), "diffTest.xml");
			output.delete();
			StatementRunnerResult result = diff.execute("WbSchemaDiff -file='" + output.getAbsolutePath() + "' -includeForeignKeys=false -includePrimaryKeys=false -includeIndex=false -referenceProfile=source -targetProfile=target");
			assertTrue(result.isSuccess());
			assertTrue("File not created", output.exists());
			
			if (!output.delete())
			{
				fail("could not delete output file");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void setupDatabase()
		throws SQLException, ClassNotFoundException
	{
		util = new TestUtil("schemaDiffTest");
		this.source = util.getConnection("source");
		this.target = util.getConnection("target");

		Statement stmt = null;
		
		try
		{
			stmt = source.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(100), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(50), city varchar(100), phone varchar(50), email varchar(50))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
      stmt.executeUpdate("alter table person_address add constraint fk_pa_address foreign key (address_id) references address(address_id)");

			stmt.executeUpdate("CREATE VIEW v_person AS SELECT * FROM person");
			stmt.executeUpdate("CREATE sequence seq_one");
			stmt.executeUpdate("CREATE sequence seq_two  increment by 5");
			stmt.executeUpdate("CREATE sequence seq_three");
			SqlUtil.closeStatement(stmt);
			
			stmt = target.createStatement();
			stmt.executeUpdate("create table person (person_id integer primary key, firstname varchar(50), lastname varchar(100))");
			stmt.executeUpdate("create table address (address_id integer primary key, street varchar(10), city varchar(100), pone varchar(50), remark varchar(500))");
			stmt.executeUpdate("create table person_address (person_id integer, address_id integer, primary key (person_id, address_id))");
			stmt.executeUpdate("alter table person_address add constraint fk_pa_person foreign key (person_id) references person(person_id)");
			
			stmt.executeUpdate("CREATE VIEW something AS SELECT * FROM address");

			stmt.executeUpdate("CREATE sequence seq_one");
			stmt.executeUpdate("CREATE sequence seq_two");
			stmt.executeUpdate("CREATE sequence seq_to_be_deleted");
		}
		finally
		{
			SqlUtil.closeStatement(stmt);
		}
		
	}
}
