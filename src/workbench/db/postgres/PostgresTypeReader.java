/*
 *  PostgresTableListEnhancer.java
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import workbench.db.DbMetadata;
import workbench.db.DbObject;
import workbench.db.JdbcUtils;
import workbench.db.ObjectListEnhancer;
import workbench.db.ObjectListExtender;
import workbench.db.TableColumnsDatastore;
import workbench.db.TableDefinition;
import workbench.db.TableIdentifier;
import workbench.db.TableSourceBuilder;
import workbench.db.TableSourceBuilderFactory;
import workbench.db.WbConnection;
import workbench.log.LogMgr;
import workbench.storage.DataStore;
import workbench.util.CollectionUtil;

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

    String select = 
				" SELECT NULL AS TABLE_CAT, " +
				" n.nspname AS TABLE_SCHEM, " +
				" c.relname AS TABLE_NAME, " +
				" 'TYPE' as TABLE_TYPE, " +
				" d.description AS REMARKS " +
				" FROM pg_catalog.pg_namespace n, pg_catalog.pg_class c " +
				" LEFT JOIN pg_catalog.pg_description d ON (c.oid = d.objoid AND d.objsubid = 0) " +
				" LEFT JOIN pg_catalog.pg_class dc ON (d.classoid=dc.oid AND dc.relname='pg_class') " +
				" LEFT JOIN pg_catalog.pg_namespace dn ON (dn.oid=dc.relnamespace AND dn.nspname='pg_catalog') " +
				" WHERE c.relkind = 'c' AND c.relnamespace = n.oid ";

		if (schemaPattern != null && !"".equals(schemaPattern))
		{
				select += " AND n.nspname LIKE '" + schemaPattern + "' ";
		}

		if (objectPattern != null)
		{
				select += " AND c.relname LIKE '" + objectPattern + "' ";
		}
		
		select += " ORDER BY TABLE_TYPE,TABLE_SCHEM,TABLE_NAME ";
		Statement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = con.createStatementForQuery();
			rs = stmt.executeQuery(select);
			while (rs.next())
			{
				String schema = rs.getString("TABLE_SCHEM");
				String name = rs.getString("TABLE_NAME");
				String type = rs.getString("TABLE_TYPE");
				String remarks = rs.getString("REMARKS");
				int row = result.addRow();
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, schema);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, name);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, type);
				result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, remarks);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresTypeReader.extendObjectList()", "Error retrieving object types", e);
		}
		return true;
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
	public DbObject getObjectDefinition(WbConnection con, DbObject name)
	{
		// Only used in the SchemaReporter. If this method returns null
		// the reporter will tread the object as a table
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
		TableSourceBuilder builder = TableSourceBuilderFactory.getBuilder(con);
		try
		{
			CharSequence sql = builder.getTableSource(createTableIdentifier(object), true, true);
			return sql == null ? "" : sql.toString();
		}
		catch (Exception e)
		{
			LogMgr.logError("PostgresTypeReader.getObjectSource()", "Cannot build type source ", e);
			return "";
		}
	}
}
