/*
 * DataStoreImporterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import java.io.File;
import java.io.FileWriter;
import java.sql.Types;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.gui.dialogs.dataimport.ImportOptions;
import workbench.gui.dialogs.dataimport.TextImportOptions;
import workbench.storage.DataStore;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DataStoreImporterTest 
	extends WbTestCase
{
	private TestUtil util;
	
	public DataStoreImporterTest()
		throws Exception
	{
		super();
		util = getTestUtil();
	}

	private DataStore prepareDataStore()
	{
		String[] cols = new String[] { "ID", "FIRSTNAME", "LASTNAME" };
		int[] types = new int[] { Types.INTEGER, Types.VARCHAR, Types.VARCHAR };
		int[] sizes = new int[] { 5, 25, 25 };

		DataStore ds = new DataStore(cols, types, sizes);
		return ds;
	}

	@Test
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
			
			importer.setImportOptions(f,ProducerFactory.ImportType.Text, o, to, null);
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
	
	@Test
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
