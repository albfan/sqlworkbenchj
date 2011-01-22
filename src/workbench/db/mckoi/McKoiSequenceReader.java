/*
 * McKoiSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.mckoi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;


/**
 * @author  Thomas Kellerer
 */
public class McKoiSequenceReader
	implements SequenceReader
{
	private Connection connection;

	public McKoiSequenceReader(Connection con)
	{
		this.connection = con;
	}

	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>();
		for (int row=0; row < ds.getRowCount(); row++)
		{
			SequenceDefinition def = createDefinition(ds, row);
			result.add(def);
		}
		return result;
	}

	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
		if (ds == null || ds.getRowCount() != 1) return null;
		SequenceDefinition def = createDefinition(ds, 0);
		return def;
	}

	public DataStore getRawSequenceDefinition(String catalog, String owner, String sequence)
	{
		String sql = "SELECT si.name, " +
								 "       sd.minvalue, " +
								 "       sd.maxvalue, " +
								 "       sd.increment, " +
								 "       sd.cycle, " +
								 "       sd.start, " +
								 "       sd.cache " +
								 " FROM SYS_INFO.sUSRSequence sd, " +
								 "     SYS_INFO.sUSRSequenceInfo si " +
								 "WHERE sd.seq_id = si.id " +
								 "AND   si.schema = ? ";

		if (!StringUtil.isEmptyString(sequence))
		{
			sql += "AND   si.name LIKE ? ";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("McKoiMetadata.getRawSequenceDefinition()", "Using query=" + sql);
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = this.connection.prepareStatement(sql);
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

	public CharSequence getSequenceSource(String catalog, String owner, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, owner, sequence);
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
		result.append(";\n");

		def.setSource(result);
	}

}
