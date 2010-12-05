/*
 * JoinCreatorTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.fksupport;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import workbench.util.TableAlias;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class JoinCreatorTest
{

	public JoinCreatorTest()
	{
	}

	@Test
	public void testJoinCreator()
		throws Exception
	{
		String sql = "select * from person p join address a ";
		int pos = sql.indexOf("address a") + "address a".length() + 1;
		JoinCreator creator = new JoinCreator(sql, pos, null);
		
		TableAlias join = creator.getJoinTable();
		System.out.println("main: " + join);
		TableAlias joined = creator.getJoinedTable();
		System.out.println("joined: " + joined);
	}
}
