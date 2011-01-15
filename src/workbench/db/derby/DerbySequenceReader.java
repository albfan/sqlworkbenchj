/*
 * DerbySequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.derby;

import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.WbConnection;


/**
 * SequenceReader for Apache Derby 10.6
 *
 * @author  Thomas Kellerer
 */
public class DerbySequenceReader
	implements SequenceReader
{
	private WbConnection dbConn;
	private String baseQuery;

	public DerbySequenceReader(WbConnection conn)
	{
		dbConn = conn;
		baseQuery = "SELECT sch.schemaname,  \n" +
             "       seq.sequencename, \n" +
             "       seq.sequencedatatype, \n" +
             "       seq.currentvalue, \n" +
             "       seq.startvalue, \n" +
             "       seq.minimumvalue, \n" +
             "       seq.maximumvalue, \n" +
             "       seq.increment, \n" +
             "       seq.cycleoption \n" +
             "FROM sys.syssequences seq \n" +
             "  JOIN sys.sysschemas sch ON sch.schemaid = seq.schemaid";
	}

	@Override
	public List<SequenceDefinition> getSequences(String catalog, String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, namePattern);
		if (ds == null) return Collections.emptyList();
		List<SequenceDefinition> result = new ArrayList<SequenceDefinition>(ds.getRowCount());
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			result.add(createSequenceDefinition(ds, row));
		}
		return result;
	}

	@Override
	public SequenceDefinition getSequenceDefinition(String catalog, String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(catalog, owner, sequence);
		if (ds == null || ds.getRowCount() != 1) return null;
		return createSequenceDefinition(ds, 0);
	}

	private SequenceDefinition createSequenceDefinition(DataStore ds, int row)
	{
		String name = ds.getValueAsString(row, "sequencename");
		String schema = ds.getValueAsString(row, "schemaname");
		SequenceDefinition result = new SequenceDefinition(schema, name);
		result.setSequenceProperty("START", ds.getValue(row, "startvalue"));
		result.setSequenceProperty("MINVALUE", ds.getValue(row, "minimumvalue"));
		result.setSequenceProperty("MAXVALUE", ds.getValue(row, "maximumvalue"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "increment"));
		result.setSequenceProperty("CYCLE", ds.getValue(row, "cycleoption"));
		result.setSequenceProperty("DATATYPE", ds.getValueAsString(row, "sequencedatatype"));
		readSequenceSource(result);
		return result;
	}

	public DataStore getRawSequenceDefinition(String catalog, String schema, String namePattern)
	{
		String sql = baseQuery;

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(schema))
		{
			sql += " WHERE sch.schemaname = '" + schema + "'";
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(namePattern))
		{
			if (whereAdded)
			{
				sql += " AND ";
			}
			else
			{
				sql += " WHERE ";
			}
			sql += " seq.sequencename LIKE '" + namePattern + "'";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("DerbySequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
		}

		Statement stmt = null;
		ResultSet rs = null;
		DataStore result = null;
		try
		{
			stmt = dbConn.createStatementForQuery();
			rs = stmt.executeQuery(sql);
			result = new DataStore(rs, dbConn, true);
		}
		catch (Exception e)
		{
			LogMgr.logError("DerbySequenceReader.getRawSequenceDefinition()", "Error when retrieving sequence definition", e);
		}
		finally
		{
			SqlUtil.closeAll(rs,stmt);
		}

		return result;
	}

	@Override
	public CharSequence getSequenceSource(String catalog, String schema, String sequence)
	{
		SequenceDefinition def = getSequenceDefinition(catalog, schema, sequence);
		if (def == null) return null;
		return def.getSource();
	}

	@Override
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
		String type = (String) def.getSequenceProperty("DATATYPE");

    result.append(" AS " + type);
		result.append(buildSequenceDetails(true, type, start, minvalue, maxvalue, increment, cycle, order, cache));

		result.append(';');
		result.append(nl);
		def.setSource(result);
	}

	public static CharSequence buildSequenceDetails(boolean doFormat, String type, Number start, Number minvalue, Number maxvalue, Number increment, String cycle, String order, Number cache)
	{
		StringBuilder result = new StringBuilder(30);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		if (start.longValue() > 0)
		{
			if (doFormat) result.append(nl + "       ");
			result.append("START WITH ");
			result.append(start);
		}

		if (doFormat) result.append(nl + "      ");
		result.append(" INCREMENT BY ");
		result.append(increment);

		if (doFormat) result.append(nl + "      ");

		boolean hasMinValue = false;
		if ("integer".equalsIgnoreCase(type))
		{
			hasMinValue = minvalue != null && minvalue.intValue() != Integer.MIN_VALUE;
		}
		else if ("bigint".equalsIgnoreCase(type))
		{
			hasMinValue = minvalue != null && minvalue.longValue() != Long.MIN_VALUE;
		}

		if (hasMinValue)
		{
			result.append(" MINVALUE ");
			result.append(minvalue);
		}
		else
		{
			if (doFormat) result.append(" NO MINVALUE");
		}

		if (doFormat) result.append(nl + "      ");
		if (maxvalue != null && maxvalue.longValue() == -1)
		{
			if (maxvalue.longValue() != Long.MAX_VALUE)
			{
				result.append(" MAXVALUE ");
				result.append(maxvalue);
			}
		}
		else if (doFormat)
		{
			result.append(" NO MAXVALUE");
		}

		if (doFormat) result.append(nl + "      ");
		if (cycle != null && cycle.equals("Y"))
		{
			result.append(" CYCLE");
		}
		else if (doFormat)
		{
			result.append(" NO CYCLE");
		}

		return result;
	}

}
