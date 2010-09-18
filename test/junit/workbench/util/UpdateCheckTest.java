/*
 * UpdateCheckTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Date;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class UpdateCheckTest
{

	@SuppressWarnings("deprecation")
	@Test
	public void testNeedCheck()
	{
		UpdateCheck check = new UpdateCheck();
		int interval = 7;
		Date last = new Date(2007, 3, 10);
		Date now = new Date(2007, 3, 10);
		boolean need = check.needCheck(interval, now, last);
		assertFalse(need);

		now = new Date(2007, 3, 16);
		need = check.needCheck(interval, now, last);
		assertFalse(need);

		now = new Date(2007, 3, 17);
		need = check.needCheck(interval, now, last);
		assertTrue(need);

		need = check.needCheck(interval, now, null);
		assertTrue(need);

		now = new Date(2007, 3, 10);
		need = check.needCheck(1, now, last);
		assertFalse(need);

		now = new Date(2007, 3, 11);
		need = check.needCheck(1, now, last);
		assertTrue(need);

	}

}
