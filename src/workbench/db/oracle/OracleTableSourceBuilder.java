/*
 * OracleTableSourceBuilder.java
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
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;

/**
 *
 * @author Thomas Kellerer
 */
public class OracleTableSourceBuilder
	extends TableSourceBuilder
{

	public OracleTableSourceBuilder(WbConnection con)
	{
		super(con);
	}

	@Override
	protected String getAdditionalTableOptions(TableIdentifier table, List<ColumnIdentifier> columns, DataStore aIndexDef)
	{
		if (Settings.getInstance().getBoolProperty("workbench.db.oracle.retrieve_partitions", true))
		{
			try
			{
				OracleTablePartition reader = new OracleTablePartition(this.dbConnection);
				reader.retrieve(table, dbConnection);
				return reader.getSource();
			}
			catch (SQLException sql) 
			{
				LogMgr.logError("OracleTableSourceBuilder.getAdditionalTableOptions()", "Error retrieving partitions", sql);
			}
		}
		return null;
	}
	
}
