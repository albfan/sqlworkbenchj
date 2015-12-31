/*
 * UpdateCheckTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.*;

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
