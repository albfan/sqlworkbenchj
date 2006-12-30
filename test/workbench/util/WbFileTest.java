/*
 * WbFileTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import junit.framework.*;

/**
 *
 * @author support@sql-workbench.net
 */
public class WbFileTest extends TestCase
{
	
	public WbFileTest(String testName)
	{
		super(testName);
	}

	public void testGetFileName()
	{
		WbFile f = new WbFile("test.dat");
		assertEquals("Wrong filename returned", "test", f.getFileName());
		
		f = new WbFile("test.dat.zip");
		assertEquals("Wrong filename returned", "test.dat", f.getFileName());
		
		f = new WbFile("/temp/bla/test.zip");
		assertEquals("Wrong filename returned", "test", f.getFileName());
	}

	public void testGetExtension()
	{
		WbFile f = new WbFile("test.dat");
		
		assertEquals("Wrong extension returned", "dat", f.getExtension());
		f = new WbFile("test.dat.zip");
		assertEquals("Wrong extension returned", "zip", f.getExtension());
		
		f = new WbFile("test.");
		assertEquals("Wrong extension returned", "", f.getExtension());
		
		f = new WbFile("c:/temp/bla/test.zip");
		assertEquals("Wrong extension returned", "zip", f.getExtension());
	}
	
}
