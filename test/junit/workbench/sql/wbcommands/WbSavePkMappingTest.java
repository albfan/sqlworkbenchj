/*
 * WbSavePkMappingTest
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import java.io.BufferedReader;
import java.util.List;
import workbench.util.EncodingUtil;
import workbench.util.FileUtil;
import workbench.sql.StatementRunnerResult;
import org.junit.Test;
import workbench.TestUtil;
import workbench.WbTestCase;
import workbench.storage.PkMapping;
import workbench.util.WbFile;
import static org.junit.Assert.*;

/**
 *
 * @author Thomas Kellerer
 */
public class WbSavePkMappingTest
	extends WbTestCase
{

	public WbSavePkMappingTest()
	{
		super("WbSavePkMappingTest");
	}

	@Test
	public void testExecute()
		throws Exception
	{
		TestUtil util = getTestUtil();
		WbFile f = new WbFile(util.getBaseDir(), "pkmapping.properties");
		PkMapping.getInstance().clear();
		PkMapping.getInstance().addMapping("person", "id");
		PkMapping.getInstance().addMapping("v_person", "id");

		WbSavePkMapping save = new WbSavePkMapping();

		StatementRunnerResult result = save.execute(save.getVerb() + "-file=" + f.getFullPath());
		assertNotNull(result);
		assertTrue(result.isSuccess());

		BufferedReader in = EncodingUtil.createBufferedReader(f, "ISO-8859-1");
		List<String> lines = FileUtil.getLines(in, true);
		assertEquals(3, lines.size());
		assertEquals("# Primary key mapping for SQL Workbench/J", lines.get(0));
		assertEquals("person=id", lines.get(1));
		assertEquals("v_person=id", lines.get(2));
	}
}
