/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.oracle;

import workbench.db.ViewGrantReader;

/**
 *
 * @author tkellerer
 */
public class OracleViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		String sql = "select grantee, privilege, grantable  \n" +
             "from ALL_TAB_PRIVS \n" +
             "where table_name = ? \n" +
             " and table_schema = ? ";
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
