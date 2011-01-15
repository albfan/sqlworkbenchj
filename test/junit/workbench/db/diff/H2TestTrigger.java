/*
 * H2TestTrigger.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.diff;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

/**
 *
 * @author Thomas Kellerer
 */
public class H2TestTrigger
	implements Trigger
{

	@Override
	public void init(Connection cnctn, String string, String string1, String string2, boolean bln, int i)
		throws SQLException
	{
		// nothing to do
	}

	@Override
	public void fire(Connection cnctn, Object[] os, Object[] os1)
		throws SQLException
	{
		// nothing to do
	}

}
