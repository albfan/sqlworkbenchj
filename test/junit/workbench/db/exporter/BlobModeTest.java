/*
 * BlobModeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import junit.framework.TestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class BlobModeTest
	extends TestCase
{

	public BlobModeTest(String testName)
	{
		super(testName);
	}

	public void testGetMode()
	{
		assertEquals(BlobMode.SaveToFile, BlobMode.getMode("file "));
		assertEquals(BlobMode.Base64, BlobMode.getMode("base64"));
		assertEquals(BlobMode.AnsiLiteral, BlobMode.getMode("ANSI"));
		assertEquals(BlobMode.DbmsLiteral, BlobMode.getMode("dbms"));
	}

}
