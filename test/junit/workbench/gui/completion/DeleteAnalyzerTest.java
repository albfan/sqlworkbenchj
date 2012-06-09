/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.gui.completion;


import org.junit.Test;
import static org.junit.Assert.*;

import workbench.WbTestCase;
import workbench.db.TableIdentifier;

/**
 *
 * @author Thomas Kellerer
 */
public class DeleteAnalyzerTest
	extends WbTestCase
{
	public DeleteAnalyzerTest()
	{
		super("DeleteAnalyzer");
	}

	@Test
	public void testGetTable()
	{
		String sql = "DELETE FROM public.sometable WHERE foo = 1";
		DeleteAnalyzer analyzer = new DeleteAnalyzer(null, sql, sql.indexOf("foo") - 1);
		analyzer.checkContext();
		TableIdentifier table = analyzer.getTableForColumnList();
		assertNotNull(table);
		assertEquals("public", table.getSchema());
		assertEquals("sometable", table.getTableName());
	}

	@Test
	public void testAlternateSeparator()
	{
		String sql = "DELETE FROM mylib/sometable WHERE foo = 1";
		DeleteAnalyzer analyzer = new DeleteAnalyzer(null, sql, sql.indexOf("foo") - 1);
		analyzer.setCatalogSeparator('/');
		analyzer.checkContext();
		TableIdentifier table = analyzer.getTableForColumnList();
		assertNotNull(table);
		assertEquals("mylib", table.getCatalog());
		assertEquals("sometable", table.getTableName());
	}
}
