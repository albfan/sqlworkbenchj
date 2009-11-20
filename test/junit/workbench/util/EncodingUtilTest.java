/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.util;

import junit.framework.TestCase;

/**
 *
 * @author tkellerer
 */
public class EncodingUtilTest
	extends TestCase
{
	public EncodingUtilTest(String testName)
	{
		super(testName);
	}

	public void testCleanupEncoding()
	{
		assertEquals("UTF-8", EncodingUtil.cleanupEncoding("utf"));
		assertEquals("UTF-8", EncodingUtil.cleanupEncoding("utf-8"));
		assertEquals("UTF-8", EncodingUtil.cleanupEncoding("UTF-8"));
		assertEquals("UTF-16", EncodingUtil.cleanupEncoding("UTF-16"));
		assertEquals("UTF-32", EncodingUtil.cleanupEncoding("UTF-32"));
		assertEquals("UTF-8", EncodingUtil.cleanupEncoding("utf8"));
		assertEquals("UTF-16", EncodingUtil.cleanupEncoding("utf16"));
		assertEquals("UTF-16BE", EncodingUtil.cleanupEncoding("utf16be"));
		assertEquals("UTF-16LE", EncodingUtil.cleanupEncoding("utf16LE"));
		assertEquals("UTF-32", EncodingUtil.cleanupEncoding("utf32"));
		assertEquals("UTF-32BE", EncodingUtil.cleanupEncoding("utf32be"));
		assertEquals("UTF-32LE", EncodingUtil.cleanupEncoding("utf32Le"));
		assertEquals("UTF-32LE", EncodingUtil.cleanupEncoding("utf-32Le"));
		assertEquals("ISO-8859-1", EncodingUtil.cleanupEncoding("iso88591"));
		assertEquals("ISO-8859-15", EncodingUtil.cleanupEncoding("iso885915"));
		assertEquals("WIN1251", EncodingUtil.cleanupEncoding("win1251"));
	}
}
