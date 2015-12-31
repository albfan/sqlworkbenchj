/*
 * DerbyViewGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.derby;

import workbench.db.ViewGrantReader;

/**
 * A class to read view grants for Apache Derby
 * @author Thomas Kellerer
 */
public class DerbyViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		String sql = "select trim(grantee) as grantee, privilege, is_grantable \n" +
             "from (  \n" +
             "select grantee,   \n" +
             "       'SELECT' as privilege,   \n" +
             "       case   \n" +
             "         when SELECTPRIV = 'Y' then 'YES'  \n" +
             "         else 'NO'  \n" +
             "       end as is_grantable,  \n" +
             "       TABLEID \n" +
             "from sys.SYSTABLEPERMS  \n" +
             "where SELECTPRIV in ('Y', 'y')  \n" +
             "UNION   \n" +
             "select grantee,   \n" +
             "       'UPDATE' as privilege,   \n" +
             "       case  \n" +
             "         when updatepriv = 'Y' then 'YES'  \n" +
             "         else 'NO'  \n" +
             "       end as is_grantable,  \n" +
             "       TABLEID \n" +
             "from sys.SYSTABLEPERMS  \n" +
             "where updatepriv in ('Y', 'y')  \n" +
             "UNION   \n" +
             "select grantee,   \n" +
             "       'DELETE' as privilege,   \n" +
             "       case  \n" +
             "         when deletepriv = 'Y' then 'YES'  \n" +
             "         else 'NO'  \n" +
             "       end as is_grantable,  \n" +
             "       TABLEID \n" +
             "from sys.SYSTABLEPERMS  \n" +
             "where deletepriv in ('Y', 'y')  \n" +
             "UNION   \n" +
             "select grantee,   \n" +
             "       'INSERT' as privilege,   \n" +
             "       case  \n" +
             "         when insertpriv = 'Y' then 'YES'  \n" +
             "         else 'NO'  \n" +
             "       end as is_grantable,  \n" +
             "       TABLEID \n" +
             "from sys.SYSTABLEPERMS  \n" +
             "where insertpriv in ('Y', 'y')  \n" +
             ") t, sys.systables tab  \n" +
             "where t.tableid = tab.tableid " +
						 "and tab.tablename = ?";
		return sql;
	}

	@Override
	public int getIndexForTableNameParameter()
	{
		return 1;
	}

}
