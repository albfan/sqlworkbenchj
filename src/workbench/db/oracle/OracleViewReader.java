/*
 * OracleViewReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.SQLException;
import workbench.db.DbMetadata;
import workbench.db.DefaultViewReader;
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
	public CharSequence getExtendedViewSource(TableDefinition view, boolean includeDrop, boolean includeCommit)
		throws SQLException
	{
		String type = view.getTable().getType();
		if (DbMetadata.MVIEW_NAME.equals(type))
		{
			OracleMViewReader reader = new OracleMViewReader();
			CharSequence sql = reader.getMViewSource(this.connection, view.getTable(), null, includeDrop);
			return sql;
		}
		return super.getExtendedViewSource(view, includeDrop, includeCommit);
	}

	@Override
	public CharSequence getViewSource(TableIdentifier viewId)
		throws NoConfigException
	{
		if (DbMetadata.MVIEW_NAME.equalsIgnoreCase(viewId.getType()))
		{
			OracleMViewReader reader = new OracleMViewReader();
			CharSequence sql = reader.getMViewSource(this.connection, viewId, null, false);
			return sql;
		}
		return super.getViewSource(viewId);
	}

}
