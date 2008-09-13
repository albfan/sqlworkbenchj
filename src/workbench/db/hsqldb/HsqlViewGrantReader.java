/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.hsqldb;

import workbench.db.ViewGrantReader;

/**
 *
 * @author support@sql-workbench.net
 */
public class HsqlViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		String sql = "select grantee, privilege, is_grantable  \n" +
             "from information_schema.SYSTEM_TABLEPRIVILEGES \n" +
             "where table_name = ? \n" +
             " and table_schem = ? ";
		return sql;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

	@Override
	public int getIndexForSchemaParameter()
	{
		return 2;
	}

}
