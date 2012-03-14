/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.ibm;

import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class Db2SearchPathTest
{
	public Db2SearchPathTest()
	{
	}

	@Test
	public void testParseResult()
	{
		Db2SearchPath reader = new Db2SearchPath();
		List<String> entries = CollectionUtil.arrayList("*LBL");
		List<String> result = reader.parseResult(entries);
		assertTrue(result.isEmpty());
		entries = CollectionUtil.arrayList("one, two");
		result = reader.parseResult(entries);
		assertEquals(2, result.size());
	}
}
