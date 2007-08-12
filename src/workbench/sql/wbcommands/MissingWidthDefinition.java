/*
 * MissingWidthDefinition.java
 *
 * Created on 10.08.2007, 21:47:37
 *
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package workbench.sql.wbcommands;

/**
 *
 * @author support@sql-workbench.net
 */
public class MissingWidthDefinition
	extends Exception
{
	private String colname;

	public MissingWidthDefinition(String col)
	{
		colname = col;
	}

	public String getColumnName()
	{
		return colname;
	}

	@Override
	public String getMessage()
	{
		return "Missing or invalid width definition for: " + colname;
	}
}
