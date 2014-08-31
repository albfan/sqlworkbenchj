/*
 * PostgresTypeReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.postgres;

import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.BaseObjectType;
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
import workbench.db.sqltemplates.ColumnChanger;

import workbench.storage.DataStore;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * An ObjectlistEnhancer to work around a bug in the Postgres JDBC driver
 * to return a proper value in the TABLE_TYPE column.
 *
 * @author Thomas Kellerer
 */
public class PostgresTypeReader
	implements ObjectListExtender, ObjectListEnhancer
{
	private final PostgresRangeTypeReader rangeReader = new PostgresRangeTypeReader();

	@Override
	public void updateObjectList(WbConnection con, DataStore result, String aCatalog, String aSchema, String objects, String[] requestedTypes)
	{
		String replacement = null;

		if (JdbcUtils.hasMinimumServerVersion(con, "9.3"))
		{
			// this assumes that the 9.3 server is used together with a 9.x driver!
			replacement = DbMetadata.MVIEW_NAME;
		}
		else if (!JdbcUtils.hasMiniumDriverVersion(con, "9.0"))
		{
			// pre-9.0 drivers did no handle types correctly
			replacement = "TYPE";
		}

		if (DbMetadata.typeIncluded(replacement, requestedTypes))
		{
			int count = result.getRowCount();
			for (int row=0; row < count; row++)
			{
				String type = result.getValueAsString(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE);
				if (type == null)
				{
					result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, replacement);
				}
			}
		}
	}

	@Override
	public boolean extendObjectList(WbConnection con, DataStore result, String catalog, String schemaPattern, String objectPattern, String[] requestedTypes)
	{
		boolean retrieveTypes = DbMetadata.typeIncluded("TYPE", requestedTypes);
		boolean retrieveRangeTypes = JdbcUtils.hasMinimumServerVersion(con, "9.2") && PostgresRangeTypeReader.retrieveRangeTypes();

		if (JdbcUtils.hasMiniumDriverVersion(con, "9.0"))
		{
			// nothing to do, the 9.0 driver will correctly return the TYPE entries
			retrieveTypes = false;
		}

		if (requestedTypes == null)
		{
			// if all objects were selected, even the old driver will already return the types
			retrieveTypes = false;
		}

		List<BaseObjectType> types = new ArrayList<>();
		if (retrieveTypes)
		{
			// this is only needed for pre 9.0 drivers as they did not return
			// any object types, if that was specifically requested
			types.addAll(getTypes(con, schemaPattern, objectPattern));
		}

		if (retrieveRangeTypes)
		{
			List<PgRangeType> rangeTypes = rangeReader.getRangeTypes(con, schemaPattern, objectPattern);
			types.addAll(rangeTypes);
		}

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
		List<BaseObjectType> result = new ArrayList<>();

    StringBuilder select = new StringBuilder(100);

		String baseSelect =
			"SELECT null as table_cat, \n" +
		 "        n.nspname as table_schem, \n" +
		 "        t.typname as table_name, \n" +
		 "        'TYPE' as table_type, \n" +
		 "        pg_catalog.obj_description(t.oid, 'pg_type') as remarks \n" +
		 "FROM pg_catalog.pg_type t \n" +
		 "  JOIN pg_catalog.pg_namespace n ON n.oid = t.typnamespace \n" +
		 "WHERE (t.typrelid = 0 OR (SELECT c.relkind = 'c' FROM pg_catalog.pg_class c WHERE c.oid = t.typrelid)) \n" +
		 " AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_type el WHERE el.oid = t.typelem AND el.typarray = t.oid) \n" +
		 " AND n.nspname <> 'pg_catalog' \n" +
		 " AND n.nspname <> 'information_schema' \n";

		if (!JdbcUtils.hasMinimumServerVersion(con, "8.3"))
		{
			baseSelect = baseSelect.replace("AND el.typarray = t.oid", "");
		}

		select.append(baseSelect);
		SqlUtil.appendAndCondition(select, "n.nspname", schemaPattern, con);
		SqlUtil.appendAndCondition(select, "t.typname", objectPattern, con);

		select.append("\n ORDER BY 2,3 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logInfo("PostgresTypeReader.extendObjectList()", "Using SQL: " + select);
		}

		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		try
		{
			sp = con.setSavepoint();
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
			con.releaseSavepoint(sp);
		}
		catch (Exception e)
		{
			con.rollback(sp);
			LogMgr.logError("PostgresTypeReader.getTypes()", "Error retrieving object types", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}


	@Override
	public boolean isDerivedType()
	{
		return false;
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
		String template = mgr.getCommentSqlTemplate("type", null);
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
