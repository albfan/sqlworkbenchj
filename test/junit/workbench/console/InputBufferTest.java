/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.console;

import workbench.WbTestCase;
import workbench.sql.DelimiterDefinition;

/**
 *
 * @author support@sql-workbench.net
 */
public class InputBufferTest
	extends WbTestCase
{
	public InputBufferTest()
	{
		super("InputBufferTest");
	}

	public void testAddLine()
	{
		InputBuffer buffer = new InputBuffer();
		boolean result = buffer.addLine("select * ");
		assertFalse(result);
		result = buffer.addLine("from mytable");
		assertFalse(result);
		result = buffer.addLine(";");
		assertTrue(result);

		buffer.setDelimiter(DelimiterDefinition.DEFAULT_ALTERNATE_DELIMITER);
		buffer.clear();

		result = buffer.addLine("create or replace procedure proc");
		assertFalse(result);
		result = buffer.addLine("as ");
		assertFalse(result);
		result = buffer.addLine("begin ");
		assertFalse(result);
		result = buffer.addLine("  delete from test; ");
		assertFalse(result);
		result = buffer.addLine("end;");
		assertFalse(result);
		result = buffer.addLine("/");
		assertTrue(result);

	}

}