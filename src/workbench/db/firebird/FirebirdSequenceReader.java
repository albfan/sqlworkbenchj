/*
 * FirebirdSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.firebird;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * SequenceReader for <a href="http://www.h2database.com">H2 Database</a>
 *
 * @author  Thomas Kellerer
 */
public class FirebirdSequenceReader
	implements SequenceReader
{
	private WbConnection dbConnection;

	public FirebirdSequenceReader(WbConnection conn)
	{
		this.dbConnection = conn;
	}

	/**
	 *	Return the source SQL for a H2 sequence definition.
	 *
	 *	@return The SQL to recreate the given sequence
	 */
	public CharSequence getSequenceSource(String owner, String aSequence)
	{
		SequenceDefinition def = getSequenceDefinition(owner, aSequence);
		if (def == null) return "";
		return def.getSource();
	}

	public List<SequenceDefinition> getSequences(String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		for (int row=0; row < ds.getRowCount(); row++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
    DataStore ds = getRawSequenceDefinition(owner, sequence);
    if (ds == null || ds.getRowCount() == 0) return null;

		return createSequenceDefinition(ds, 0);
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		SequenceDefinition result = null;

    if (ds == null || ds.getRowCount() == 0) return null;

		String name = ds.getValueAsString(row, 0);
		result = new SequenceDefinition(null, name);

		String comment = ds.getValueAsString(row, 1);
		result.setComment(comment);
		readSequenceSource(result);

		return result;
	}

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;

		StringBuilder result = new StringBuilder(100);

    result.append("CREATE SEQUENCE ");
    result.append(def.getSequenceName());

		result.append(";\n");

		String comments = def.getComment();
		if (StringUtil.isNonBlank(comments))
		{
			result.append("\nCOMMENT ON SEQUENCE ");
			result.append(def.getSequenceName());
			result.append(" IS '");
			result.append(comments.replace("'", "''"));
			result.append("';");
		}
		def.setSource(result);
		return;
	}

	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		Statement stmt = null;
		ResultSet rs = null;
		DataStore ds = null;
		try
		{
			String sql = 
				"SELECT trim(rdb$generator_name) AS SEQUENCE_NAME, \n" +
				"       trim(rdb$description) AS REMARKS \n" +
				"FROM rdb$generators \n" +
				"WHERE rdb$system_flag = 0";

			if (StringUtil.isNonBlank(sequence))
			{
				if (sequence.indexOf("%") > 0)
				{
					sql += " AND rdb$generator_name LIKE '" + sequence + "'";
				}
				else
				{
					sql += " AND rdb$generator_name = '" + sequence + "'";
				}
			}
			sql += " ORDER BY 1";
			if (Settings.getInstance().getDebugMetadataSql())
			{
				LogMgr.logInfo("FirebirdSequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
			}
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			ds = new DataStore(rs, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("FirebirdSequenceReader.getRawSequenceDefinition()", "Error reading sequence definition", e);
			ds = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return ds;
	}
}
