/*
 * ImportFileHandlerTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;
import workbench.TestUtil;
import workbench.util.ZipOutputFactory;

/**
 *
 * @author support@sql-workbench.net
 */
public class ImportFileHandlerTest extends junit.framework.TestCase
{

	public ImportFileHandlerTest(String testName)
	{
		super(testName);
	}

	public void testHandler()
	{
		try
		{
			TestUtil util = new TestUtil("outputFactoryTest");
			File importFile  = new File(util.getBaseDir(), "datafile.txt");

			File archive = new File(util.getBaseDir(), "archive.zip");
			ZipOutputFactory zout = new ZipOutputFactory(archive);
			PrintWriter out = new PrintWriter(zout.createWriter(importFile, "UTF-8"));

			String firstline = "nr\tfirstname\tlastname";
			out.println(firstline);
			out.print(Integer.toString(1));
			out.print('\t');
			out.println("First\t\"Last");
			out.println("name\"");
			out.close();

			zout.done();

			ImportFileHandler handler = new ImportFileHandler();
			handler.setMainFile(archive, "UTF-8");
			assertEquals("ZIP Archive not recognized", true, handler.isZip());

			BufferedReader br = handler.getMainFileReader();
			String line = br.readLine();

			assertEquals("Wrong data read", firstline, line);

			handler.done();
			handler.setMainFile(archive, "UTF-8");
			handler.done();
			if (!archive.delete())
			{
				fail("Could not delete archive!");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
