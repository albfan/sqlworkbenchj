/*
 * ByteBufferTest.java
 * JUnit based test
 *
 * Created on July 29, 2006, 11:34 AM
 */

package workbench.util;

import junit.framework.*;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;

/**
 *
 * @author support@sql-workbench.net
 */
public class ByteBufferTest extends TestCase
{
	
	public ByteBufferTest(String testName)
	{
		super(testName);
	}

	public void testByteBuffer()
	{
		try
		{
			ByteBuffer buffer = new ByteBuffer();
			int size = 1024;
			byte[] data = new byte[size];
			for (int i = 0; i < size; i++)
			{
				data[i] = (byte)(i % 255);
			}
			buffer.append(data);
			buffer.append(data);
			assertEquals("Wrong size", size * 2, buffer.getLength());
			byte b = buffer.getBuffer()[0];
			assertEquals("Wrong data", 0, b);
			b = buffer.getBuffer()[size];
			assertEquals("Wrong data", 0, b);
			b = buffer.getBuffer()[size + 1];
			assertEquals("Wrong data", 1, b);
			int l = buffer.getBuffer().length;
			assertEquals("Wrong buffer size", 2048, l);
			byte[] more = new byte[20];
			buffer.append(more);
			l = buffer.getBuffer().length;
			assertEquals("Wrong buffer size", 2068, l);			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail();
		}
	}
	
}
