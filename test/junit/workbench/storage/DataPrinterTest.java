/*
 * DataPrinterTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.storage;

import java.io.StringWriter;
import java.sql.Types;
import java.io.Writer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class DataPrinterTest
{

	public DataPrinterTest()
	{
	}

	@Test
	public void testWriteDataString()
		throws Exception
	{
		System.out.println("writeDataString");
		int[] types = new int[] {Types.INTEGER, Types.VARCHAR, Types.VARCHAR };
		String[] names = new String[] {"ID", "FIRSTNAME", "LASTNAME" };
		DataStore ds = new DataStore(names, types);
		int row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(1));
		ds.setValue(row, 1, "Arthur");
		ds.setValue(row, 2, "Dent");

		row = ds.addRow();
		ds.setValue(row, 0, Integer.valueOf(2));
		ds.setValue(row, 1, "Zaphod");
		ds.setValue(row, 2, "Beeblebrox");
		
		Writer out = new StringWriter(50);

		DataPrinter printer = new DataPrinter(ds, ";", "\n", null, true);
		printer.writeDataString(out, null);
		assertEquals("ID;FIRSTNAME;LASTNAME\n1;Arthur;Dent\n2;Zaphod;Beeblebrox\n", out.toString());

		out = new StringWriter(50);
		printer.writeDataString(out, new int[] {1} );
		assertEquals("ID;FIRSTNAME;LASTNAME\n2;Zaphod;Beeblebrox\n", out.toString());

		int[] colMap = new int[] {2,1,0};
		printer.setColumnMapping(colMap);

		out = new StringWriter(50);
		printer.writeDataString(out, new int[] {0} );
		assertEquals("LASTNAME;FIRSTNAME;ID\nDent;Arthur;1\n", out.toString());

	}
}
