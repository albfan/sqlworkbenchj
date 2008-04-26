/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.importer;

import java.sql.SQLException;

/**
 *
 * @author support@sql-workbench.net
 */
public class ModeNotPossibleException
	extends SQLException
{
	public ModeNotPossibleException(String msg)
	{
		super(msg);
	}
}
