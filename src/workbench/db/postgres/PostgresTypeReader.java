/*
 *  PostgresTypeReader.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import workbench.db.BaseObjectType;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnIdentifier;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.ObjectListEnhancer;
import workbench.db.ObjectListExtender;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ObjectlistEnhancer to work around a bug in the Postgres JDBC driver, as that driver
 * does not return a value in the TABLE_TYPE column
 *
 * @author Thomas Kellerer
 */
public class PostgresTypeReader
	implements ObjectListExtender, ObjectListEnhancer
{

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
	{
		int count = result.getRowCount();
		for (int row=0; row < count; row++)
		{
			String type = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
			if (type == null)
			{
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, "TYPE");
			}
		}
	}

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schemaPattern, String objectPattern, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("TYPE", requestedTypes)) return false;

		if (JdbcUtils.hasMiniumDriverVersion(con.getSqlConnection(), "9.0"))
		{
			// nothing to do, the 9.0 driver will correctly return the TYPE entries
			return false;
		}

		if (requestedTypes == null)
		{
			// if all objects were selected, the driver will already return the types
			// we just need to update the TABLE_TYPE column
			updateObjectList(con, result, catalog, schemaPattern, objectPattern, requestedTypes);
			return true;
		}
		
		List<BaseObjectType> types = getTypes(con, schemaPattern, objectPattern);
		if (types.isEmpty()) return false;

		for (BaseObjectType type : types)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, type.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, type.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type.getObjectType());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, type.getComment());
			result.getRow(row).setUserObject(type);
		}
		return true;
	}

	public List<BaseObjectType> getTypes(WbConnection con, String schemaPattern, String objectPattern)
	{
		List<BaseObjectType> result = new ArrayList<BaseObjectType>();
		
    StringBuilder select = new StringBuilder(100);

		select.append(" SELECT NULL AS TABLE_CAT, " +
				" n.nspname AS TABLE_SCHEM, " +
				" c.relname AS TABLE_NAME, " +
				" 'TYPE' as TABLE_TYPE, " +
				" d.description AS REMARKS " +
				" FROM pg_catalog.pg_namespace n, pg_catalog.pg_class c " +
				" LEFT JOIN pg_catalog.pg_description d ON (c.oid = d.objoid AND d.objsubid = 0) " +
				" LEFT JOIN pg_catalog.pg_class dc ON (d.classoid=dc.oid AND dc.relname='pg_class') " +
				" LEFT JOIN pg_catalog.pg_namespace dn ON (dn.oid=dc.relnamespace AND dn.nspname='pg_catalog') " +
				" WHERE c.relkind = 'c' AND c.relnamespace = n.oid ");

		SqlUtil.appendAndCondition(select, "n.nspname", schemaPattern);
		SqlUtil.appendAndCondition(select, "c.relname", objectPattern);

		select.append(" ORDER BY TABLE_TYPE,TABLE_SCHEM,TABLE_NAME ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("PostgresTypeReader.extendObjectList()", "Using SQL: " + select);
		}

		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(select.toString());
			while (rs.next())
			{
				String schema = rs.getString("TABLE_SCHEM");
				String name = rs.getString("TABLE_NAME");
				String type = rs.getString("TABLE_TYPE");
				String remarks = rs.getString("REMARKS");
				BaseObjectType pgtype = new BaseObjectType(schema, name);
				pgtype.setComment(remarks);
				result.add(pgtype);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresTypeReader.getTypes()", "Error retrieving object types", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
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
		try
		{
			TableDefinition tdef = con.getMetadata().getTableDefinition(createTableIdentifier(object));
			DataStore result = new TableColumnsDatastore(tdef);
			return result;
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresTypeReader.getObjectDetails()", "Cannot retrieve type columns", e);
			return null;
		}
	}

	@Override
	public BaseObjectType getObjectDefinition(WbConnection con, DbObject name)
	{
		try
		{
			TableDefinition tdef = con.getMetadata().getTableDefinition(createTableIdentifier(name));
			BaseObjectType type = new BaseObjectType(tdef.getTable().getSchema(), tdef.getTable().getTableName());
			type.setComment(tdef.getTable().getComment());
			type.setAttributes(tdef.getColumns());
			return type;
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresTypeReader.getObjectDetails()", "Cannot retrieve type columns", e);
		}
		return null;
	}

	private TableIdentifier createTableIdentifier(DbObject object)
	{
		TableIdentifier tbl = new TableIdentifier(object.getCatalog(), object.getSchema(), object.getObjectName());
		tbl.setComment(object.getComment());
		tbl.setType(object.getObjectType());
		return tbl;
	}

	@Override
	public String getObjectSource(WbConnection con, DbObject object)
	{
		BaseObjectType type = getObjectDefinition(con, object);
		if (type == null) return null;
		StringBuilder sql = new StringBuilder(50 + type.getNumberOfAttributes() * 50);
		sql.append("CREATE TYPE ");
		sql.append(type.getObjectName());
		sql.append(" AS\n(\n");
		List<ColumnIdentifier> columns = type.getAttributes();
		int maxLen = ColumnIdentifier.getMaxNameLength(columns);
		for (int i=0; i < columns.size(); i++)
		{
			sql.append("  ");
			sql.append(StringUtil.padRight(columns.get(i).getColumnName(), maxLen + 2));
			sql.append(columns.get(i).getDbmsType());
			if (i < columns.size() - 1) sql.append(",\n");
		}
		sql.append("\n);\n");
		return sql.toString();
	}
}
