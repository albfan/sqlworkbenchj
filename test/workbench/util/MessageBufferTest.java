/*
 * MessageBufferTest.java
 * JUnit based test
 *
 * Created on November 14, 2006, 9:47 AM
 */

package workbench.util;

import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class MessageBufferTest 
	extends TestCase
{
	
	public MessageBufferTest(String testName)
	{
		super(testName);
		try
		{
			TestUtil util = new TestUtil();
			util.prepareEnvironment();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void testMaxSize()
	{
		try
		{
			int max = 5;
			MessageBuffer b = new MessageBuffer(5);
			for (int i = 0; i < max * 2; i++)
			{
				b.append("Line" + i + "\n");
			}
			String content = b.getBuffer().toString();
			String expected = "(...)\nLine5\nLine6\nLine7\nLine8\nLine9\n";
			assertEquals(expected, content);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public void testBuffer()
	{
		try
		{
			MessageBuffer b = new MessageBuffer();
			b.append("Hello, world");
			b.appendNewLine();
			b.append("how are you?");
			
			StringBuilder s = b.getBuffer();
			assertEquals("Hello, world\nhow are you?", s.toString());
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	
}
