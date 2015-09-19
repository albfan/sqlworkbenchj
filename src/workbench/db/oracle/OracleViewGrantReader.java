/*
 * OracleViewGrantReader.java
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
package workbench.db.oracle;

import workbench.db.TableIdentifier;
import workbench.db.ViewGrantReader;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleViewGrantReader
	extends ViewGrantReader
{

	@Override
	public String getViewGrantSql()
	{
		return
      "-- SQL Workbench \n" +
      "SELECT grantee, privilege, grantable \n" +
      "FROM all_tab_privs \n" +
      "WHERE table_name = ? \n" +
      "  AND table_schema = ? ";
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

  @Override
  public StringBuilder getViewGrantSource(WbConnection dbConnection, TableIdentifier view)
  {
    if (OracleUtils.getUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.grant))
    {
      String grants = OracleUtils.getDependentDDL(dbConnection, "OBJECT_GRANT", view.getTableName(), view.getSchema());
      StringBuilder result = new StringBuilder(grants == null ? 0 : grants.length());
      result.append(grants);
      return result;
    }
    return super.getViewGrantSource(dbConnection, view); //To change body of generated methods, choose Tools | Templates.
  }

}
