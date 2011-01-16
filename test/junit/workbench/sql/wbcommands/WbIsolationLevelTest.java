/*
 * WbIsolationLevelTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.sql.Connection;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbIsolationLevelTest
{

	public WbIsolationLevelTest()
	{
	}

	@Test
	public void testLevelMap()
		throws Exception
	{
		WbIsolationLevel cmd = new WbIsolationLevel();

		int level = cmd.stringToLevel(" read  committed ");
		assertEquals(Connection.TRANSACTION_READ_COMMITTED, level);

		level = cmd.stringToLevel(" Serializable");
		assertEquals(Connection.TRANSACTION_SERIALIZABLE, level);

		level = cmd.stringToLevel(" repeatable READ");
		assertEquals(Connection.TRANSACTION_REPEATABLE_READ, level);

		level = cmd.stringToLevel(" repeatable_READ");
		assertEquals(Connection.TRANSACTION_REPEATABLE_READ, level);

		level = cmd.stringToLevel(" read \nUNcommitted ");
		assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, level);

		level = cmd.stringToLevel(" not known");
		assertEquals(-1, level);

	}


}
