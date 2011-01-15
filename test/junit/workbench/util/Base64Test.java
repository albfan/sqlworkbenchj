/*
 * Base64Test.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.*;
import org.junit.Test;

public class Base64Test
{
	private static final long SEED = 12345678;
	private static Random s_random = new Random(SEED);

	private byte[] createData(int length)
		throws Exception
	{
		byte[] bytes = new byte[length];
		s_random.nextBytes(bytes);
		return bytes;
	}

	private void runStreamTest(int length)
		throws Exception
	{
		byte[] data = createData(length);
		ByteArrayOutputStream out_bytes = new ByteArrayOutputStream();
		OutputStream out = new Base64.OutputStream(out_bytes);
		out.write(data);
		out.close();
		byte[] encoded = out_bytes.toByteArray();
		byte[] decoded = Base64.decode(encoded, 0, encoded.length, 0);
		assertTrue(Arrays.equals(data, decoded));

		Base64.InputStream in = new Base64.InputStream(new ByteArrayInputStream(encoded));
		out_bytes = new ByteArrayOutputStream();
		byte[] buffer = new byte[3];
		for (int n = in.read(buffer); n > 0; n = in.read(buffer))
		{
			out_bytes.write(buffer, 0, n);
		}
		out_bytes.close();
		in.close();
		decoded = out_bytes.toByteArray();
		assertTrue(Arrays.equals(data, decoded));
	}

	@Test
	public void testStreams()
		throws Exception
	{
		for (int i = 0; i < 100; ++i)
		{
			runStreamTest(i);
		}
		for (int i = 100; i < 2000; i += 250)
		{
			runStreamTest(i);
		}
		for (int i = 2000; i < 80000; i += 1000)
		{
			runStreamTest(i);
		}
	}
}
