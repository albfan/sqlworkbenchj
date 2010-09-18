/*
 * OracleMetadataTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import org.junit.Test;
import java.sql.Types;
import static org.junit.Assert.*;

/**
 * @author Thomas Kellerer
 */
public class OracleMetadataTest
{

	@Test
	public void testGetSqlTypeDisplay()
	{
		// Test with BYTE as default semantics
		OracleMetadata meta = new OracleMetadata(OracleMetadata.BYTE_SEMANTICS, false);
		
		// Test non-Varchar types
		assertEquals("CLOB", meta.getSqlTypeDisplay("CLOB", Types.CLOB, -1, -1, 0));
		assertEquals("NVARCHAR(300)", meta.getSqlTypeDisplay("NVARCHAR", Types.VARCHAR, 300, -1, 0));
		assertEquals("CHAR(5)", meta.getSqlTypeDisplay("CHAR", Types.CHAR, 5, -1, 0));
		assertEquals("NUMBER(10,2)", meta.getSqlTypeDisplay("NUMBER", Types.NUMERIC, 10, 2, 0));
		
		String display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200)", display);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200 Char)", display);

		meta = new OracleMetadata(OracleMetadata.CHAR_SEMANTICS, false);
		
		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200 Byte)", display);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200)", display);

		meta = new OracleMetadata(OracleMetadata.CHAR_SEMANTICS, true);
		
		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.BYTE_SEMANTICS);
		assertEquals("VARCHAR(200 Byte)", display);

		display = meta.getSqlTypeDisplay("VARCHAR", Types.VARCHAR, 200, 0, OracleMetadata.CHAR_SEMANTICS);
		assertEquals("VARCHAR(200 Char)", display);
		
	}
}
