/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.ibm;

import workbench.db.ViewGrantReader;

/**
 * A class to read view grants for DB2
 * @author support@sql-workbench.net
 */
public class Db2ViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		String sql = "select trim(grantee) as grantee, privilege, is_grantable  \n" +
             "from ( \n" +
             "select grantee,  \n" +
             "       'SELECT' as privilege,  \n" +
             "       case controlauth \n" +
             "         when 'Y' then 'YES' \n" +
             "         else 'NO' \n" +
             "       end as is_grantable, \n" +
             "       tabname,  \n" +
             "       tabschema \n" +
             "from syscat.tabauth \n" +
             "where selectauth = 'Y' \n" +
             "UNION  \n" +
             "select grantee,  \n" +
             "       'UPDATE' as privilege,  \n" +
             "       case controlauth \n" +
             "         when 'Y' then 'YES' \n" +
             "         else 'NO' \n" +
             "       end as is_grantable, \n" +
             "       tabname,  \n" +
             "       tabschema \n" +
             "from syscat.tabauth \n" +
             "where updateauth = 'Y' \n" +
             "UNION  \n" +
             "select grantee,  \n" +
             "       'DELETE' as privilege,  \n" +
             "       case controlauth \n" +
             "         when 'Y' then 'YES' \n" +
             "         else 'NO' \n" +
             "       end as is_grantable, \n" +
             "       tabname,  \n" +
             "       tabschema \n" +
             "from syscat.tabauth \n" +
             "where deleteauth = 'Y' \n" +
             "UNION  \n" +
             "select grantee,  \n" +
             "       'INSERT' as privilege,  \n" +
             "       case controlauth \n" +
             "         when 'Y' then 'YES' \n" +
             "         else 'NO' \n" +
             "       end as is_grantable, \n" +
             "       tabname,  \n" +
             "       tabschema \n" +
             "from syscat.tabauth \n" +
             "where insertauth = 'Y' \n" +
             ") t \n" +
						 "where tabname = ? and tabschema = ? ";
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
