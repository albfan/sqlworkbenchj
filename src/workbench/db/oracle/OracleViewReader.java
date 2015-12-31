/*
 * OracleViewReader.java
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
package workbench.db.oracle;

import java.sql.SQLException;

import workbench.db.DbMetadata;
import workbench.db.DefaultViewReader;
import workbench.db.DropType;
import workbench.db.NoConfigException;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleViewReader
	extends DefaultViewReader
{

	public OracleViewReader(WbConnection con)
	{
		super(con);
	}

	@Override
	public CharSequence getExtendedViewSource(TableDefinition view, DropType dropType, boolean includeCommit)
		throws SQLException
	{
		String type = view.getTable().getType();
		if (DbMetadata.MVIEW_NAME.equals(type))
		{
			OracleMViewReader reader = new OracleMViewReader();
			CharSequence sql = reader.getMViewSource(this.connection, view, null, dropType, true);
			return sql;
		}
		return super.getExtendedViewSource(view, dropType, includeCommit);
	}

	@Override
	public CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException
	{
		if (DbMetadata.MVIEW_NAME.equalsIgnoreCase(viewId.getType()))
		{
			OracleMViewReader reader = new OracleMViewReader();
			CharSequence sql = reader.getMViewSource(this.connection, new TableDefinition(viewId), null, DropType.none, false);
			return sql;
		}

    if (OracleUtils.getUseOracleDBMSMeta(OracleUtils.DbmsMetadataTypes.view))
    {
      try
      {
        return DbmsMetadata.getDDL(connection, "VIEW", viewId.getRawTableName(), viewId.getRawSchema());
      }
      catch (SQLException ex)
      {
        // ignore
      }
    }

		return super.getViewSource(viewId);
	}

}
