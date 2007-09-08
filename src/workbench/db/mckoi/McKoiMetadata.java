/*
 * McKoiMetadata.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mckoi;

import java.math.BigInteger;
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
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * @author  support@sql-workbench.net
 */
public class McKoiMetadata
	implements SequenceReader
{
	private Connection connection;
	
	public McKoiMetadata(Connection con)
	{
		this.connection = con;
	}
	
	// This is a dirty hack, as McKoi does not store the real SQL
	// but some kind of Object in the database. But for now this seems
	// to work.
	public String getViewSourceFromBlob(byte[] content)
	{
		String s = new String(content, 2, content.length - 6);
		return s;
	}
	
	public List<SequenceDefinition> getSequences(String owner)
	{
		DataStore ds = getRawSequenceDefinition(owner, null);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();
		for (int row=0; row < ds.getRowCount(); row++)
		{
			SequenceDefinition def = createDefinition(ds, row);
			result.add(def);
		}
		return result;
	}
	
	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(owner, sequence);
		if (ds == null || ds.getRowCount() != 1) return null;
		SequenceDefinition def = createDefinition(ds, 0);
		return def;
	}

	public DataStore getRawSequenceDefinition(String owner, String sequence)
	{
		String sql = "SELECT si.name, \n" + 
								 "       sd.minvalue, \n" + 
								 "       sd.maxvalue, \n" + 
								 "       sd.increment, \n" + 
								 "       sd.cycle, \n" + 
								 "       sd.start, \n" + 
								 "       sd.cache \n" + 
								 " FROM SYS_INFO.sUSRSequence sd, \n" + 
								 "     SYS_INFO.sUSRSequenceInfo si \n" + 
								 "WHERE sd.seq_id = si.id \n" + 
								 "AND   si.schema = ? \n";
		
		if (!StringUtil.isEmptyString(sequence))
		{
			sql += "AND   si.name = ? ";	
		}
		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.prepareStatement(sql.toString());
			stmt.setString(1, owner);
			if (!StringUtil.isEmptyString(sequence)) stmt.setString(2, sequence);
			rs = stmt.executeQuery();
			result = new DataStore(rs, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("McKoiMetadata.getSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		
		return result;
	}
	
	private SequenceDefinition createDefinition(DataStore ds, int row)
	{
		if (ds == null || row >= ds.getRowCount()) return null;
		String name = ds.getValueAsString(row, "name");
		SequenceDefinition result = new SequenceDefinition(null, name);
		result.setSequenceProperty("minvalue", ds.getValue(row, "minvalue"));
		result.setSequenceProperty("maxvalue", ds.getValue(row, "maxvalue"));
		result.setSequenceProperty("increment", ds.getValue(row, "increment"));
		result.setSequenceProperty("cycle", ds.getValue(row, "cycle"));
		result.setSequenceProperty("cache", ds.getValue(row, "cache"));
		result.setSequenceProperty("start", ds.getValue(row, "start"));
		readSequenceSource(result);
		return result;
	}
	
	public List<String> getSequenceList(String owner)
	{
		DataStore ds = getRawSequenceDefinition(owner, null);
		if (ds == null) return Collections.emptyList();
		List<String> result = new LinkedList<String>();
		for (int row=0; row < ds.getRowCount(); row ++)
		{
			result.add(ds.getValueAsString(row, "NAME"));
		}
		return result;
	}
	
	public CharSequence getSequenceSource(String owner, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(owner, sequence);
		if (def == null) return null;
		return def.getSource();
	}
	
	public void readSequenceSource(SequenceDefinition def)
	{
		StringBuilder result = new StringBuilder(200);
		
		result.append("CREATE SEQUENCE ");
		result.append(def.getSequenceName());

		Number minvalue = (Number) def.getSequenceProperty("minvalue");
		Number maxvalue = (Number) def.getSequenceProperty("maxvalue");
		Number increment = (Number) def.getSequenceProperty("increment");
		Boolean cycle = (Boolean) def.getSequenceProperty("cycle");
		Number start = (Number) def.getSequenceProperty("start");
		Number cache = (Number) def.getSequenceProperty("cache");

		if (start.longValue() > 0)
		{
			result.append("\n      START ");
			result.append(start);
		}

		if (increment.longValue() != 1)
		{
			result.append("\n      INCREMENT ");
			result.append(increment);
		}

		BigInteger max = new BigInteger(Long.toString(Long.MAX_VALUE));

		if (minvalue.longValue() != 0)
		{
			result.append("\n      MINVALUE ");
			result.append(minvalue);
		}

		if (maxvalue.longValue() != Long.MAX_VALUE)
		{
			result.append("\n      MAXVALUE ");
			result.append(maxvalue);
		}

		if (cache.longValue() > 0)
		{
			result.append("\n      CACHE ");
			result.append(cache);
		}
		if (cycle)
		{
			result.append("\n      CYCLE");
		}
		result.append(';');
		def.setSource(result);
	}
	
}
