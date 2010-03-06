/*
 * OracleSequenceReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import workbench.db.SequenceDefinition;
import workbench.db.SequenceReader;
import workbench.db.WbConnection;
import workbench.resource.Settings;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class OracleSequenceReader
  implements SequenceReader
{
  private WbConnection connection;

  public OracleSequenceReader(WbConnection conn)
  {
    this.connection = conn;
  }

	public List<SequenceDefinition> getSequences(String owner, String namePattern)
	{
		DataStore ds = getRawSequenceDefinition(owner, namePattern);
		if (ds == null || ds.getRowCount() == 0) return Collections.emptyList();
		ArrayList<SequenceDefinition> result = new ArrayList<SequenceDefinition>();
		for (int row = 0; row < ds.getRowCount(); row ++)
		{
			result.add(createDefinition(ds, row));
		}
		return result;
	}

	public SequenceDefinition getSequenceDefinition(String owner, String sequence)
	{
		DataStore ds = getRawSequenceDefinition(owner, sequence);
		if (ds == null || ds.getRowCount() == 0) return null;
		SequenceDefinition def = createDefinition(ds, 0);
		return def;
	}

  public DataStore getRawSequenceDefinition(String owner, String sequence)
  {
    String sql = "SELECT SEQUENCE_OWNER, SEQUENCE_NAME, \n       " +
			"MIN_VALUE, \n       " +
			"MAX_VALUE, \n       " +
			"INCREMENT_BY, \n       " +
			"CASE WHEN CYCLE_FLAG = 'Y' then 'CYCLE' ELSE 'NOCYCLE' END AS CYCLE_FLAG, \n       " +
			"CASE WHEN ORDER_FLAG = 'Y' then 'ORDER' ELSE 'NOORDER' END AS ORDER_FLAG, \n       " +
			"CACHE_SIZE, \n" +
			"LAST_NUMBER \n" +
			"FROM ALL_SEQUENCES \n" +
			"WHERE sequence_owner = ?";

		if (!StringUtil.isEmptyString(sequence))
		{
			sql += "  AND sequence_name LIKE ? ";
		}

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("OracleSequenceReader.getRawSequenceDefinition()", "Using query=\n" + sql);
		}

    PreparedStatement stmt = null;
    ResultSet rs = null;
    DataStore result = null;
    try
    {
      stmt = this.connection.getSqlConnection().prepareStatement(sql);
      stmt.setString(1, owner);
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
      SqlUtil.closeAll(rs, stmt);
    }

    return result;
  }

  public CharSequence getSequenceSource(String owner, String sequence)
  {
		SequenceDefinition def = getSequenceDefinition(owner, sequence);
		if (def == null) return null;
		return def.getSource();
	}

	private SequenceDefinition createDefinition(DataStore ds, int row)
	{
		if (ds == null || row >= ds.getRowCount()) return null;
		String name = ds.getValueAsString(row, "SEQUENCE_NAME");
		String owner = ds.getValueAsString(row, "SEQUENCE_OWNER");
		SequenceDefinition result = new SequenceDefinition(owner, name);
		result.setSequenceProperty("MIN_VALUE", ds.getValue(row, "MIN_VALUE"));
		result.setSequenceProperty("MIN_VALUE", ds.getValue(row, "MIN_VALUE"));
		result.setSequenceProperty("MAX_VALUE", ds.getValue(row, "MAX_VALUE"));
		result.setSequenceProperty("INCREMENT", ds.getValue(row, "INCREMENT_BY"));
		result.setSequenceProperty("CYCLE_FLAG", ds.getValue(row, "CYCLE_FLAG"));
		result.setSequenceProperty("CACHE_SIZE", ds.getValue(row, "CACHE_SIZE"));
		result.setSequenceProperty("ORDER_FLAG", ds.getValue(row, "ORDER_FLAG"));
		readSequenceSource(result);
		return result;
	}


	public void readSequenceSource(SequenceDefinition def)
	{
		if (def == null) return;
		if (def.getSource() != null) return;

		StringBuilder result = new StringBuilder(100);
		String nl = Settings.getInstance().getInternalEditorLineEnding();

		result.append("CREATE SEQUENCE ");
		result.append(def.getSequenceName());

		Number minValue = (Number) def.getSequenceProperty("MIN_VALUE");
		Number maxValue = (Number) def.getSequenceProperty("MAX_VALUE");

		Number increment = (Number) def.getSequenceProperty("INCREMENT");

		String cycle = (String) def.getSequenceProperty("CYCLE_FLAG");
		String order = (String) def.getSequenceProperty("ORDER_FLAG");
		Number cache = (Number) def.getSequenceProperty("CACHE_SIZE");

		result.append(nl + "      INCREMENT BY ");
		result.append(increment);

		if (minValue != null && minValue.intValue() != 0)
		{
			result.append(nl + "      NOMINVALUE");
		}
		else
		{
			result.append(nl + "      MINVALUE ");
			result.append(minValue);
		}

		if (maxValue != null && !maxValue.toString().equals("999999999999999999999999999"))
		{
			result.append(nl + "      MAXVALUE ");
			result.append(maxValue);
		}
		else
		{
			result.append(nl + "      NOMAXVALUE");
		}

		if (cache != null && cache.longValue() > 0)
		{
			result.append(nl + "      CACHE ");
			result.append(cache);
		}
		else
		{
			result.append(nl + "      NOCACHE");
		}
		result.append(nl + "      ");
		result.append(cycle);

		result.append(nl + "      ");
		result.append(order);

		result.append(';');
		result.append(nl);

		def.setSource(result);
	}

}
