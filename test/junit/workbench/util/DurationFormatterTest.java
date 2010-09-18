/*
 * DurationFormatterTest.java
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
		s = s = f.getDurationAsSeconds(millis);
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
	}
}
