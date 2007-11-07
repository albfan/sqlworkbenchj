/*
 * HsqlSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
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
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import static workbench.util.StringUtil.isEmptyString;


/**
 * @author  support@sql-workbench.net
 */
public class HsqlSequenceReader
	implements SequenceReader
{
	private Connection dbConn;
	private String sequenceTable;
	
	public HsqlSequenceReader(Connection conn)
	{
		this.dbConn = conn;
		if (HsqlMetadata.supportsInformationSchema(conn))
		{
			sequenceTable = "information_schema.system_sequences";
		}
		else
		{
			sequenceTable = "system_sequences";
		}
	}

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		CharSequence s = getSequenceSource(def.getSequenceOwner(), def.getSequenceName());
		def.setSource(s);
	}
	
	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		String query = "SELECT sequence_schema, " +
						"sequence_name, " +
						"dtd_identifier, " +
						"maximum_value, " +
						"minimum_value, " +
						"increment, " +
						"cycle_option, " +
						"start_with " +
						"FROM " + sequenceTable;
		
		if (!isEmptyString(sequence))
		{
			query += " WHERE sequence_name = ?";
		}
		
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("HsqlSequenceReader.getRawSequenceDefinition()", "Using query=" + query.toString());
		}
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.dbConn.prepareStatement(query.toString());
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
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT"));
		result.setSequenceProperty("DTD_IDENTIFIER", ds.getValue(row, "DTD_IDENTIFIER"));
		
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
		
		StringBuilder result = new StringBuilder(100);
		result.append("CREATE SEQUENCE ");
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		result.append(def.getSequenceName());
		String type = (String)def.getSequenceProperty("DTD_IDENTIFIER");
		
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
		
		return result;
	}
}
