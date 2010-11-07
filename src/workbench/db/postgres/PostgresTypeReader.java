/*
 *  PostgresTypeReader.java
 *
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code may be reused without the permission of the author
 *
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import workbench.db.BaseObjectType;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import workbench.db.ColumnChanger;
import workbench.db.ColumnIdentifier;
import workbench.db.CommentSqlManager;
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

		String baseSelect =
			"SELECT null as table_cat, \n" +
		 "       n.nspname as table_schem, \n" +
		 "       pg_catalog.format_type(t.oid, NULL) as table_name, \n" +
		 "       'TYPE' as table_type, \n" +
		 "       pg_catalog.obj_description(t.oid, 'pg_type') as remarks \n" +
		 "FROM pg_catalog.pg_type t \n" +
		 "     JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace \n" +
		 "WHERE (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)) \n" +
		 "  AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid) \n" +
		 "      AND n.nspname <> 'pg_catalog' \n" +
		 "      AND n.nspname <> 'information_schema' \n";

		select.append(baseSelect);
		SqlUtil.appendAndCondition(select, "n.nspname", schemaPattern);
		SqlUtil.appendAndCondition(select, "t.typname", objectPattern);

		select.append("\n ORDER BY 2,3 ");

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
			// Currently the Postgres driver does not return the comments defined for a type
			// so it's necessary to retrieve the type definition again in order to get the correct remarks
			List<BaseObjectType> types = getTypes(con, name.getSchema(), name.getObjectName());
			if (types.size() == 1)
			{
				TableDefinition tdef = con.getMetadata().getTableDefinition(createTableIdentifier(name));
				BaseObjectType result = types.get(0);
				result.setAttributes(tdef.getColumns());
				return result;
			}
			return null;
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

		String comment = type.getComment();
		CommentSqlManager mgr = new CommentSqlManager(con.getDbSettings().getDbId());
		String template = mgr.getCommentSqlTemplate("type");
		if (StringUtil.isNonBlank(comment) && template != null)
		{
			template = template.replace(CommentSqlManager.COMMENT_OBJECT_NAME_PLACEHOLDER, type.getObjectExpression(con));
			template = template.replace(CommentSqlManager.COMMENT_PLACEHOLDER, comment);
			sql.append('\n');
			sql.append(template);
			sql.append(";\n");
		}

		ColumnChanger changer = new ColumnChanger(con);
		for (ColumnIdentifier col : columns)
		{
			String colComment = col.getComment();
			if (StringUtil.isNonBlank(colComment))
			{
				String commentSql = changer.getColumnCommentSql(object, col);
				sql.append(commentSql);
				sql.append(";\n");
			}
		}
		return sql.toString();
	}
}
