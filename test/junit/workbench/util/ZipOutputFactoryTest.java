/*
 * ZipOutputFactoryTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.io.File;
import java.io.PrintWriter;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

/**
 *
 * @author Thomas Kellerer
 */
public class ZipOutputFactoryTest
	extends WbTestCase
{

	@Test
	public void testOuputFactory()
		throws Exception
	{
		TestUtil util = getTestUtil();
		File importFile  = new File(util.getBaseDir(), "datafile.txt");

		File archive = new File(util.getBaseDir(), "archive.zip");
		ZipOutputFactory zout = new ZipOutputFactory(archive);
		PrintWriter out = new PrintWriter(zout.createWriter(importFile, "UTF-8"));

		out.println("nr\tfirstname\tlastname");
		out.print(Integer.toString(1));
		out.print('\t');
		out.println("First\t\"Last");
		out.println("name\"");
		out.close();

		zout.done();

		if (!archive.delete())
		{
			fail("Could not delete archive");
		}
	}
}
