/*
 * ImportStringVerifierTest.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.sql;

import java.sql.Types;
import junit.framework.TestCase;
import workbench.db.ColumnIdentifier;
import workbench.storage.ResultInfo;

/**
 *
 * @author support@sql-workbench.net
 */
public class ImportStringVerifierTest
	extends TestCase
{
	public ImportStringVerifierTest(String testName)
	{
		super(testName);
	}

	public void testCheckData()
	{
		try
		{
			String data = "id\tfirstname\tlastname\n1\tArthur\tDent";
			ColumnIdentifier id = new ColumnIdentifier("ID", Types.INTEGER);
			ColumnIdentifier fname = new ColumnIdentifier("FIRSTNAME", Types.VARCHAR);
			ColumnIdentifier lname = new ColumnIdentifier("LASTNAME", Types.VARCHAR);
			ResultInfo info = new ResultInfo(new ColumnIdentifier[]{id, lname, fname});
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
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
