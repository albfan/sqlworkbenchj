/*
 *  DerbyTypeReader.java
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2011, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.derby;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.ObjectListExtender;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class DerbyTypeReader
	implements ObjectListExtender
{

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schemaPattern, String objectPattern, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

		String select = getSelect(schemaPattern, objectPattern);
		select += " ORDER BY a.alias, s.schemaname ";

		Statement stmt = null;
		ResultSet rs = null;

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("DerbyTypeReader.extendObjectList()", "Using sql=\n" + select);
		}
		
		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(select);
			while (rs.next())
			{
				String schema = rs.getString("schemaname");
				String name = rs.getString("type_name");
				String classname = rs.getString("javaclassname");
				String info = rs.getString("aliasinfo");
				int row = result.addRow();
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, schema);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, name);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, "TYPE");
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, null);
				DerbyTypeDefinition def = new DerbyTypeDefinition(schema, name, classname, info);
				result.getRow(row).setUserObject(def);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("DerbyTypeReader.extendObjectList()", "Error retrieving object types", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return true;
	}

	private String getSelect(String schemaPattern, String objectPattern)
	{
		String select = 
			       "select s.schemaname, \n" +
			       "       a.alias as type_name,  \n" +
             "       a.javaclassname, \n" +
             "       a.aliasinfo \n" +
             "from sys.sysaliases a \n" +
             "  join sys.sysschemas s on a.schemaid = s.schemaid \n" +
             "where a.aliastype = 'A'   \n" +
             "and a.systemalias = 'false'";

		if (schemaPattern != null && !"".equals(schemaPattern))
		{
				select += " AND s.schemaname LIKE '" + schemaPattern + "' ";
		}

		if (objectPattern != null)
		{
				select += " AND a.alias LIKE '" + objectPattern + "' ";
		}

		return select;
	}
	
	@Override
	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("TYPE");
	}

	@Override
	public boolean handlesType(String type)
	{
		return "TYPE".equalsIgnoreCase(type);
	}

	@Override
	public boolean handlesType(String[] types)
	{
		if (types == null) return true;
		for (String type : types)
		{
			if (handlesType(type)) return true;
		}
		return false;
	}

	@Override
	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		Statement stmt = null;
		ResultSet rs = null;

		try
		{
			String select = getSelect(object.getSchema(), object.getObjectName());

			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(select);
			DataStore result = new DataStore(rs, true);
			return result;
		}
		catch (Exception e)
		{
			LogMgr.logError("DerbyTypeReader.extendObjectList()", "Error retrieving object types", e);
			return null;
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}

	@Override
	public DerbyTypeDefinition getObjectDefinition(WbConnection con, DbObject name)
	{
		DataStore def = getObjectDetails(con, name);
		if (def.getRowCount() < 1) return null;

		String schema = def.getValueAsString(0, 0);
		String typeName = def.getValueAsString(0, 1);
		String javaClass = def.getValueAsString(0, 2);
		String info = def.getValueAsString(0, 3);
		DerbyTypeDefinition type = new DerbyTypeDefinition(schema, typeName, javaClass, info);
		return type;
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
	{
		DerbyTypeDefinition type = getObjectDefinition(con, object);
		try
		{
			return type.getSource(con).toString();
		}
		catch (SQLException e)
		{
			// cannot happen
			return "";
		}
	}
}
