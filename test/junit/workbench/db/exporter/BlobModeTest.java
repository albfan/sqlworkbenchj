/*
 * BlobModeTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import org.junit.Test;
import workbench.WbTestCase;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class BlobModeTest
	extends WbTestCase
{

	@Test
	public void testGetMode()
	{
		assertEquals(BlobMode.SaveToFile, BlobMode.getMode("file "));
		assertEquals(BlobMode.Base64, BlobMode.getMode("base64"));
		assertEquals(BlobMode.AnsiLiteral, BlobMode.getMode("ANSI"));
		assertEquals(BlobMode.DbmsLiteral, BlobMode.getMode("dbms"));
	}

}
