/*
 * Db2SequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.ibm;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
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
 * A class to read sequence definitions from a DB2 database.
 * 
 * @author  support@sql-workbench.net
 */
public class Db2SequenceReader
	implements SequenceReader
{
	private WbConnection connection;
	
	public Db2SequenceReader(WbConnection conn)
	{
		this.connection = conn;
	}
	
	public List<SequenceDefinition> getSequences(String owner)
	{
		DataStore ds = getRawSequenceDefinition(owner, null);
		if (ds == null || ds.getRowCount() != 1) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>(ds.getRowCount());
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}
	
	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(owner, sequence);
		if (ds == null || ds.getRowCount() != 1) return null;
		return createSequenceDefinition(ds, 0);
	}
	
	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		String name = ds.getValueAsString(row, "SEQNAME");
		SequenceDefinition result = new SequenceDefinition(null, name);
		result.setSequenceProperty("START", ds.getValue(row, "START"));
		result.setSequenceProperty("MINVALUE", ds.getValue(row, "MINVALUE"));
		result.setSequenceProperty("MAXVALUE", ds.getValue(row, "MAXVALUE"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT"));
		result.setSequenceProperty("CYCLE", ds.getValue(row, "CYCLE"));
		result.setSequenceProperty("ORDER", ds.getValue(row, "ORDER"));
		result.setSequenceProperty("CACHE", ds.getValue(row, "CACHE"));
		result.setSequenceProperty("DATATYPEID", ds.getValue(row, "DATATYPEID"));
		readSequenceSource(result);
		return result;
	}
	
	public DataStore getRawSequenceDefinition(String schema, String sequence)
	{
		String sql ="SELECT SEQNAME, \n" +
			"       START, \n" +
			"       MINVALUE, \n" +
			"       MAXVALUE, \n" +
			"       INCREMENT, \n" +
			"       CYCLE, \n" +
			"       ORDER, \n" +
			"       CACHE, \n" +
			"       DATATYPEID  \n" +
			"FROM   syscat.sequences \n" +
			"WHERE seqschema = ?";
		
		if (!StringUtil.isEmptyString(sequence))
		{
			sql += "  AND seqname = ? ";
		}
		
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("Db2SequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
		}
		
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, schema);
			if (!StringUtil.isEmptyString(sequence)) stmt.setString(2, sequence);
			rs = stmt.executeQuery();
			result = new DataStore(rs, this.connection, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetaData.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}
		
		return result;
	}
	
	/**
	 * 	Get a list of sequences for the given owner
	 */
	public List<String> getSequenceList(String schema)
	{
		DataStore ds = getRawSequenceDefinition(schema, null);
		if (ds == null || ds.getRowCount() == 0) return Collections.emptyList();
		List<String> result = new LinkedList<String>();

		for (int row=0; row < ds.getRowCount(); row ++)
		{
			result.add(ds.getValueAsString(row, "SEQNAME"));
		}
		return result;
	}
	
	public CharSequence getSequenceSource(String schema, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(schema, sequence);
		if (def == null) return null;
		return def.getSource();
	}
	
	public void readSequenceSource(SequenceDefinition def)
	{
		StringBuilder result = new StringBuilder(100);

		String nl = Settings.getInstance().getInternalEditorLineEnding();

		result.append("CREATE SEQUENCE ");
		result.append(def.getSequenceName());

		Number start = (Number) def.getSequenceProperty("START");
		Number minvalue = (Number) def.getSequenceProperty("MINVALUE");
		Number maxvalue = (Number) def.getSequenceProperty("MAXVALUE");
		Number increment = (Number) def.getSequenceProperty("INCREMENT");
		String cycle = (String) def.getSequenceProperty("CYCLE");
		String order = (String) def.getSequenceProperty("ORDER");
		Number cache = (Number) def.getSequenceProperty("CACHE");
		Number typeid = (Number) def.getSequenceProperty("typeid");

		if (typeid != null)
		{
      result.append(" AS " + typeIdToName(typeid.intValue()));
		}
		result.append(nl + "      INCREMENT BY ");
		result.append(increment);

		if (start.longValue() > 0)
		{
			result.append(nl + "      START WITH ");
			result.append(start);
		}

		if (minvalue == null || minvalue.longValue() == 0)
		{
			result.append(nl + "      NO MINVALUE");
		}
		else
		{
			result.append(nl + "      MINVALUE ");
			result.append(minvalue);
		}

		if (maxvalue == null || maxvalue.longValue() == -1)
		{
			result.append(nl + "      MAXVALUE ");
			result.append(maxvalue);
		}
		else
		{
			result.append(nl + "      NO MAXVALUE");
		}
		if (cache != null || cache.longValue() > 0)
		{
			result.append(nl + "      CACHE ");
			result.append(cache);
		}
		else
		{
			result.append(nl + "      NO CACHE");
		}
		
		result.append(nl + "      ");
		if (cycle != null && cycle.equals("Y"))
		{
			result.append("CYCLE");
		}
		else
		{
			result.append("NO CYCLE");
		}

		result.append(nl + "      ");
		if (order != null && order.equals("Y"))
		{
			result.append("ORDER");
		}
		else
		{
			result.append("NO ORDER");
		}

		result.append(';');

		def.setSource(result);
	}

	private String typeIdToName(int id)
	{
		switch (id)
		{
			case 20:
				return "BIGINT";
			case 28:
				return "SMALLINT";
			case 16:
				return "DECIMAL";
		}
		return "INTEGER";
	}
}
