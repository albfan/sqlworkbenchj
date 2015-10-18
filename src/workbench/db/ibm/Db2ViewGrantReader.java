/*
 * Db2ViewGrantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.db.ibm;

import workbench.db.DbMetadata;
import workbench.db.ViewGrantReader;

/**
 * A class to read view grants for DB2
 * @author Thomas Kellerer
 */
public class Db2ViewGrantReader
	extends ViewGrantReader
{
	private final boolean isHostDB2;

	public Db2ViewGrantReader(String dbid)
	{
    isHostDB2 = dbid.equals(DbMetadata.DBID_DB2_ZOS);
	}

	@Override
	public String getViewGrantSql()
	{
		if (isHostDB2)
		{
			return getHostSQL();
		}
		return getLUWSql();
	}

	private String getHostSQL()
	{
		return
      "select rtrim(grantee) as grantee, privilege, is_grantable  \n" +
      "from ( \n" +
      "select grantee,  \n" +
      "       'SELECT' as privilege,  \n" +
      "       case selectauth \n" +
      "         when 'G' then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_grantable, \n" +
      "       ttname,  \n" +
      "       tcreator \n" +
      "from  sysibm.systabauth \n" +
      "where selectauth in ('Y', 'G') \n" +
      "UNION ALL \n" +
      "select grantee,  \n" +
      "       'UPDATE' as privilege,  \n" +
      "       case updateauth \n" +
      "         when 'G' then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_grantable, \n" +
      "       ttname,  \n" +
      "       tcreator \n" +
      "from  sysibm.systabauth \n" +
      "where updateauth in ('Y', 'G') \n" +
      "UNION ALL \n" +
      "select grantee,  \n" +
      "       'DELETE' as privilege,  \n" +
      "       case deleteauth \n" +
      "         when 'G' then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_grantable, \n" +
      "       ttname,  \n" +
      "       tcreator \n" +
      "from sysibm.systabauth \n" +
      "where deleteauth in ('Y', 'G') \n" +
      "UNION ALL \n" +
      "select grantee,  \n" +
      "       'INSERT' as privilege,  \n" +
      "       case insertauth \n" +
      "         when 'G' then 'YES' \n" +
      "         else 'NO' \n" +
      "       end as is_grantable, \n" +
      "       ttname,  \n" +
      "       tcreator \n" +
      "from sysibm.systabauth \n" +
      "where insertauth in ('Y', 'G') \n" +
      ") t \n" +
      "where ttname = ? and tcreator = ? ";
	}

	private String getLUWSql()
	{

    return
      "select trim(grantee) as grantee, privilege, is_grantable  \n" +
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
      "UNION ALL \n" +
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
      "UNION ALL \n" +
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
      "UNION ALL \n" +
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
