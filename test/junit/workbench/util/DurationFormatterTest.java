/*
 * DurationFormatterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Thomas Kellerer
 */
public class DurationFormatterTest
{

	@Test
	public void testGetDurationAsSeconds()
	{
		DurationFormatter f = new DurationFormatter('.');
		long millis = DurationFormatter.ONE_SECOND + 500;
		String s = f.getDurationAsSeconds(millis);
		assertEquals("1.5s", s);

		millis = DurationFormatter.ONE_SECOND * 102 + (DurationFormatter.ONE_SECOND / 2);
		s = f.getDurationAsSeconds(millis);
		assertEquals("102.5s", s);

		millis = DurationFormatter.ONE_HOUR * 3 + DurationFormatter.ONE_MINUTE * 12 + DurationFormatter.ONE_SECOND * 12 + 300;
		s = f.formatDuration(millis, true);
		assertEquals("3h 12m 12.3s", s);

		millis = DurationFormatter.ONE_HOUR * 26 + DurationFormatter.ONE_MINUTE * 12 + DurationFormatter.ONE_SECOND * 12 + 300;
		s = f.formatDuration(millis, true);
		assertEquals("26h 12m 12.3s", s);

		millis = DurationFormatter.ONE_MINUTE * 59 + DurationFormatter.ONE_SECOND * 59;
		s = f.formatDuration(millis, true);
		assertEquals("59m 59s", s);

		millis += DurationFormatter.ONE_SECOND;
		s = f.formatDuration(millis, true);
		assertEquals("1h 0m 0s", s);

		millis += 500;
		s = f.formatDuration(millis, true);
		assertEquals("1h 0m 0.5s", s);

		millis = DurationFormatter.ONE_MINUTE * 60 + DurationFormatter.ONE_SECOND * 59;
		s = f.formatDuration(millis, true);
		assertEquals("1h 0m 59s", s);

		millis = DurationFormatter.ONE_SECOND * 59;
		s = f.formatDuration(millis, true);
		assertEquals("59s", s);

		millis += DurationFormatter.ONE_SECOND;
		s = f.formatDuration(millis, true);
		assertEquals("1m 0s", s);

    s = f.formatDuration(DurationFormatter.ONE_MINUTE, false, false);
		assertEquals("1m", s.trim());
	}
}
