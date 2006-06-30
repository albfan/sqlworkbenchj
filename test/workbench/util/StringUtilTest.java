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

    public void testStringToList()
    {
        String list = "1,2,3";
        List l = StringUtil.stringToList(list, ",", true, true, true);
        assertEquals("Wrong number of elements returned", 3, l.size());
        
        list = "1,2,,3";
        l = StringUtil.stringToList(list, ",", true, true, true);
        assertEquals("Empty element not removed", 3, l.size());        
        
        list = "1,2, ,3";
        l = StringUtil.stringToList(list, ",", false);
        assertEquals("Empty element removed", 4, l.size());        
        
        list = "1,2,,3";
        l = StringUtil.stringToList(list, ",", false);
        assertEquals("Null element not removed", 3, l.size());        
        
        list = " 1 ,2,3";
        l = StringUtil.stringToList(list, ",", true);
        assertEquals("Null element not removed", 3, l.size());        
        assertEquals(" 1 ", l.get(0));
        
        l = StringUtil.stringToList(list, ",", true, true);
        assertEquals("Element not trimmed", "1", l.get(0));
        
        list = "1,\"2,5\",3";
        l = StringUtil.stringToList(list, ",", true, true, true);
        assertEquals("Quoted string not recognized","2,5", l.get(1));
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
