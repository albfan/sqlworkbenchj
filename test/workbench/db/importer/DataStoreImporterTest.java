/*
 * DataStoreImporterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.io.FileWriter;
import java.sql.Types;
import junit.framework.*;
import workbench.TestUtil;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.storage.DataStore;

/**
 *
 * @author support@sql-workbench.net
 */
public class DataStoreImporterTest extends TestCase
{

	private TestUtil util;
	
	public DataStoreImporterTest(String testName)
		throws Exception
	{
		super(testName);
		util = new TestUtil(testName);
		util.prepareEnvironment();
	}

	private DataStore prepareDataStore()
	{
		String[] cols = new String[] { "ID", "FIRSTNAME", "LASTNAME" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 5, 25, 25 };

		DataStore ds = new DataStore(cols, types, sizes);
		return ds;
	}
	
	public void testImportFile()
	{
		try
		{
			String content = "id\tfirstname\tlastname\n1\tHarry\tHandsome\n2\tMary\tMoviestart\n3\tArthur\tDent";
			File f = new File(util.getBaseDir(), "ds_import.txt");
			
			FileWriter w = new FileWriter(f);
			w.write(content);
			w.close();
			
			DataStore ds = prepareDataStore();
			DataStoreImporter importer = new DataStoreImporter(ds, null, null);
			
			TextImportOptions to = new DefaultTextImportOptions("\t", "\"");
			ImportOptions o = new DefaultImportOptions();
			
			importer.setImportOptions(f,ProducerFactory.IMPORT_TEXT, o, to, null);
			importer.startImport();
			assertEquals("Wrong number of rows imported", 3, ds.getRowCount());
			
			String name = ds.getValueAsString(0, 1);
			assertEquals("Wrong firstname", "Harry", name);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
		
	}
	public void testImportString()
	{
		try
		{
			String content = "id\tfirstname\tlastname\n1\tHarry\tHandsome\n2\tMary\tMoviestart\n3\tArthur\tDent";
			DataStore ds = prepareDataStore();
			DataStoreImporter importer = new DataStoreImporter(ds, null, null);
			importer.importString(content);
			importer.startImport();
			assertEquals("Wrong number of rows imported", 3, ds.getRowCount());
			
			String name = ds.getValueAsString(0, 1);
			assertEquals("Wrong firstname", "Harry", name);
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	
}
