/*
 * WbSysProps.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import workbench.db.WbConnection;
import workbench.interfaces.JobErrorHandler;
import workbench.resource.Settings;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.storage.DmlStatement;
import workbench.storage.RowData;
import workbench.util.ArgumentParser;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSysProps
	extends SqlCommand
{
	public static final String VERB = "WBPROPS";

	public WbSysProps()
	{
		super();
		cmdLine = new ArgumentParser();
		cmdLine.addArgument("type");
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		cmdLine.parse(getCommandLine(sql).toLowerCase());

		List<String> types = cmdLine.getListValue("type");

		if (types.contains("wb"))
		{
			result.addDataStore(getWbProperties(null));
		}
		if (types.isEmpty() || types.contains("system"))
		{
			result.addDataStore(getSystemProperties());
		}
		result.setSuccess();
		return result;
	}

	static DataStore getWbProperties(String prefix)
	{
		DataStore data = new PropertyDataStore(true);
		Set<String> keys = Settings.getInstance().getKeys();

		for (String key : keys)
		{
			if (isWorkbenchProperty(key) && (prefix == null || key.startsWith(prefix)))
			{
				int row = data.addRow();
				data.setValue(row, 0, key);
				data.setValue(row, 1, Settings.getInstance().getProperty(key, null));
			}
		}
		data.sortByColumn(0, true);
		data.setResultName("Workbench Properties");
		data.resetStatus();
		return data;
	}

	private static boolean isWorkbenchProperty(String key)
	{
		return key.startsWith("workbench.db.")
			    || key.startsWith("workbench.settings.")
			    || (key.startsWith("workbench.sql.") && !key.startsWith("workbench.sql.replace.")
				                                       && !key.startsWith("workbench.sql.formatter.")
				                                       && !key.startsWith("workbench.sql.search."));
	}

	private DataStore getSystemProperties()
	{
		DataStore data = new PropertyDataStore(false);
		Set<Entry<Object, Object>> entries = System.getProperties().entrySet();
		for (Map.Entry<Object, Object> entry : entries)
		{
			int row = data.addRow();
			data.setValue(row, 0, entry.getKey().toString());
			data.setValue(row, 1, entry.getValue().toString());
		}
		data.sortByColumn(0, true);
		data.setResultName("System Properties");
		data.resetStatus();
		return data;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}

	@Override
	protected boolean isConnectionRequired()
	{
		return false;
	}

	static class PropertyDataStore
		extends DataStore
	{
		private boolean wbProps;
		PropertyDataStore(boolean isWbProps)
		{
			super(new String[] { "PROPERTY", "VALUE"}, new int[] { Types.VARCHAR, Types.VARCHAR} );
			wbProps = isWbProps;
			getColumns()[0].setIsPkColumn(true);
		}

		@Override
		public boolean checkUpdateTable()
		{
			return true;
		}

		@Override
		public boolean checkUpdateTable(WbConnection aConn)
		{
			return true;
		}

		@Override
		public boolean hasPkColumns()
		{
			return true;
		}

		@Override
		public boolean hasUpdateableColumns()
		{
			return true;
		}

		@Override
		public boolean isUpdateable()
		{
			return true;
		}

		@Override
		public boolean needPkForUpdate()
		{
			return true;
		}

		@Override
		public boolean pkColumnsComplete()
		{
			return true;
		}

		@Override
		public synchronized int updateDb(WbConnection aConnection, JobErrorHandler errorHandler)
			throws SQLException
		{
			int rows = 0;
			this.resetUpdateRowCounters();

			for (int row=0; row < getRowCount(); row++)
			{
				RowData rowData = getRow(row);
				if (!rowData.isOriginal())
				{
					String key = getValueAsString(row, 0);
					String value = getValueAsString(row, 1);
					if (wbProps)
					{
						Settings.getInstance().setProperty(key, value);
					}
					else
					{
						System.setProperty(key, value);
					}
					getRow(row).resetStatus();
					rows ++;
				}
			}

			resetUpdateRowCounters();
			RowData row = this.getNextDeletedRow();
			while (row != null)
			{
				String key = row.getValue(0).toString();
				if (wbProps)
				{
					Settings.getInstance().removeProperty(key);
				}
				else
				{
					System.clearProperty(key);
				}
				row = this.getNextDeletedRow();
				rows ++;
			}
			resetStatus();
			return rows;
		}

		@Override
		public List<DmlStatement> getUpdateStatements(WbConnection aConnection)
			throws SQLException
		{
			return Collections.emptyList();
		}
	}

}


