/*
 * WbSysProps.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.sql.wbcommands;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import workbench.sql.SqlCommand;
import workbench.sql.StatementRunnerResult;
import workbench.storage.DataStore;
import workbench.util.ArgumentParser;


/**
 *
 * @author Thomas Kellerer
 */
public class WbSysProps
	extends SqlCommand
{
	public static final String VERB = "WBSYSPROPS";

	public WbSysProps()
	{
		super();
		cmdLine = new ArgumentParser();
	}

	@Override
	public StatementRunnerResult execute(String sql)
		throws SQLException, Exception
	{
		StatementRunnerResult result = new StatementRunnerResult(sql);

		DataStore data = new DataStore(new String[] { "PROPERTY", "VALUE"}, new int[] { Types.VARCHAR, Types.VARCHAR} );
		Set<Entry<Object, Object>> entries = System.getProperties().entrySet();
		for (Map.Entry<Object, Object> entry : entries)
		{
			int row = data.addRow();
			data.setValue(row, 0, entry.getKey().toString());
			data.setValue(row, 1, entry.getValue().toString());
		}
		data.sortByColumn(0, true);
		data.resetStatus();
		
		result.addDataStore(data);
		result.setSuccess();
		return result;
	}

	@Override
	public String getVerb()
	{
		return VERB;
	}


}
