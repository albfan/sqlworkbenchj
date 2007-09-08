/*
 * H2SequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
import workbench.util.StringUtil;

/**
 * @author  support@sql-workbench.net
 */
public class H2SequenceReader
	implements SequenceReader
{
	private Connection dbConnection;
	
	public H2SequenceReader(Connection conn)
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
	
	public List<String> getSequenceList(String owner)
	{
		List<String> result = new LinkedList<String>();
		ResultSet rs = null;
		Statement stmt = null;
		try
		{
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery("SELECT sequence_name FROM information_schema.sequences ORDER BY 1");
			while (rs.next())
			{
				result.add(rs.getString(1));
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("H2SequenceReader.getSequenceList()", "Error reading sequences", e);
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

		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String schema = ds.getValueAsString(row, "SEQUENCE_SCHEMA");
		result = new SequenceDefinition(schema, name);
		
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT"));
		
		// All properties are used to compare Sequences for SchemaDiff, so 
		// properties that have not equivalent for the CREATE SEQUENCE command
		// should not be included here.
		
    //result.setSequenceProperty("CURRENT_VALUE", ds.getValue(row, "CURRENT_VALUE"));
		//result.setSequenceProperty("REMARKS", ds.getValue(row, "REMARKS"));
		//result.setSequenceProperty("IS_GENERATED", ds.getValue(row, "IS_GENERATED"));
		readSequenceSource(result);
		return result;		
	}

	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		
		StringBuilder result = new StringBuilder(100);
		String nl = Settings.getInstance().getInternalEditorLineEnding();
		
//    result.append("DROP SEQUENCE " + def.getSequenceName() + " IF EXISTS;");
//    result.append(nl);
//    result.append(nl);
    result.append("CREATE SEQUENCE ");
    result.append(def.getSequenceName());
		
		Long inc = (Long)def.getSequenceProperty("INCREMENT");
    if (inc != null && inc != 1)
    {
      result.append(" INCREMENT BY ");
      result.append(inc);
    }
		result.append(';');
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
			String sql = "SELECT SEQUENCE_CATALOG, " +
				"SEQUENCE_SCHEMA, SEQUENCE_NAME, " +
				"CURRENT_VALUE, " +
				"INCREMENT, " +
				"IS_GENERATED, " +
				"REMARKS, " +
				"ID " +
				"FROM information_schema.sequences ";
			
			boolean whereAdded = false;

			if (!StringUtil.isEmptyString(owner)) 
			{
				if (!whereAdded)
				{
					sql += " WHERE ";
					whereAdded = true;
				}
				else
				{
					sql += " AND ";
				}
				sql += " sequence_schema = '" + owner + "'";
			}
		
			if (!StringUtil.isEmptyString(sequence))
			{
				if (!whereAdded)
				{
					sql += " WHERE ";
					whereAdded = true;
				}
				else
				{
					sql += " AND ";
				}
				sql += " sequence_name = '" + sequence + "'";
			}
			stmt = this.dbConnection.createStatement();
			rs = stmt.executeQuery(sql);
			ds = new DataStore(rs, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("H2SequenceReader.getSequenceDefinition()", "Error reading sequence definition", e);
			ds = null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return ds;
	}
}
