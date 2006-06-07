/*
 * StringUtilTest.java
 * JUnit based test
 *
 * Created on June 6, 2006, 10:49 PM
 */

package workbench.util;

import junit.framework.*;
import java.io.BufferedReader;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.log.LogMgr;

/**
 *
 * @author support@sql-workbench.net
 */
public class StringUtilTest extends TestCase
{
	
	public StringUtilTest(String testName)
	{
		super(testName);
	}

	public void testIsNumber()
	{
		boolean isNumber = StringUtil.isNumber("1");
		assertEquals(true, isNumber);
		
		isNumber = StringUtil.isNumber("1.234");
		assertEquals(true, isNumber);
		
		isNumber = StringUtil.isNumber("1.xxx");
		assertEquals(false, isNumber);
		
		isNumber = StringUtil.isNumber("bla");
		assertEquals(false, isNumber);
	}

	public void testTrimQuotes()
	{
		String s = StringUtil.trimQuotes(" \"bla\" ");
		assertEquals("bla", s);
		s = StringUtil.trimQuotes(" \"bla ");
		assertEquals(" \"bla ", s);		
		s = StringUtil.trimQuotes(" 'bla' ");
		assertEquals("bla", s);
	}


}
