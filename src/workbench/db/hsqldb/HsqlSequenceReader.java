/*
 * HsqlSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.hsqldb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import workbench.db.JdbcUtils;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import static workbench.util.StringUtil.isEmptyString;


/**
 * SequenceReader for <a href="http://www.hsqldb.org">HSQLDB</a>
 * 
 * @author  support@sql-workbench.net
 */
public class HsqlSequenceReader
	implements SequenceReader
{
	private Connection dbConn;
	private String baseQuery;

	public HsqlSequenceReader(Connection conn)
	{
		this.dbConn = conn;
		String query = "SELECT sequence_schema, " +
						"sequence_name, " +
						"dtd_identifier as data_type, " +
						"maximum_value, " +
						"minimum_value, " +
						"increment, " +
						"cycle_option, " +
						"start_with " +
						"FROM ";

		if (JdbcUtils.hasMinimumServerVersion(conn, "1.9"))
		{
			query += "information_schema.sequences";
			query = query.replace("dtd_identifier as data_type", "data_type");
		}
		else
		{
			query += "information_schema.system_sequences";
		}
		baseQuery = query;
	}

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		CharSequence s = getSequenceSource(def.getSequenceOwner(), def.getSequenceName());
		def.setSource(s);
	}
	
	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		String query = baseQuery;
		
		if (!isEmptyString(sequence))
		{
			query += " WHERE sequence_name = ?";
		}
		
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("HsqlSequenceReader.getRawSequenceDefinition()", "Using query=" + query);
		}
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.dbConn.prepareStatement(query);
			if (!isEmptyString(sequence)) stmt.setString(1, sequence.trim());
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Throwable e)
		{
			LogMgr.logError("HsqlSequenceReader.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return result;	
	}

	
	public List<SequenceDefinition> getSequences(String owner)
	{
		DataStore ds = getRawSequenceDefinition(owner, null);
		if (ds == null) return Collections.emptyList();
		
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();

		for (int row = 0; row < ds.getRowCount(); row++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;	
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		SequenceDefinition result = null;
		
    if (ds == null || ds.getRowCount() == 0) return null;

		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String schema = ds.getValueAsString(row, "SEQUENCE_SCHEMA");
		result = new SequenceDefinition(schema, name);
		
		result.setSequenceProperty("START_WITH", ds.getValue(row, "START_WITH"));
		result.setSequenceProperty("MAXIMUM_VALUE", ds.getValue(row, "MAXIMUM_VALUE"));
		result.setSequenceProperty("MINIMUM_VALUE", ds.getValue(row, "MINIMUM_VALUE"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT"));
		result.setSequenceProperty("CYCLE_OPTION", ds.getValue(row, "CYCLE_OPTION"));
		result.setSequenceProperty("DATA_TYPE", ds.getValue(row, "DATA_TYPE"));
		result.setSource(buildSource(result));
		return result;		
	}
	
	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(owner, sequence);
		if (ds == null) return null;
		return createSequenceDefinition(ds, 0);
	}
	
	public List<String> getSequenceList(String owner)
	{
		DataStore ds = getRawSequenceDefinition(owner, null);
		if (ds == null) return Collections.emptyList();
		
		List<String> result = new LinkedList<String>();

		for (int row = 0; row < ds.getRowCount(); row++)
		{
			result.add(ds.getValueAsString(row, "SEQUENCE_NAME"));
		}
		return result;
	}

	public CharSequence getSequenceSource(String owner, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(owner, sequence);
		return buildSource(def);
	}

	protected CharSequence buildSource(SequenceDefinition def)
	{
		if (def == null) return StringUtil.EMPTY_STRING;
		
		StringBuilder result = new StringBuilder(100);
		result.append("CREATE SEQUENCE ");
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append(def.getSequenceName());
		String type = (String)def.getSequenceProperty("DATA_TYPE");
		
		if (!"INTEGER".equals(type))
		{
			result.append(" AS " + type);
		}
		
		// For some reason HSQLDB returns all properties as String objects, even the numeric ones!
		String start = (String)def.getSequenceProperty("START_WITH");
		result.append(nl + "       START WITH ");
		result.append(start);
		
		String inc = (String)def.getSequenceProperty("INCREMENT"); 
		result.append(nl + "       INCREMENT BY ");
		result.append(inc);
		result.append(';');
		result.append(nl);
		
		return result;
	}
}
