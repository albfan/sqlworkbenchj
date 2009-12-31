/*
 * H2ConstantReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.h2database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
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
import workbench.util.StringUtil;

/**
 * A class to read information about defined CONSTANTs in H2.
 *
 * @author Thomas Kellerer
 */
public class H2ConstantReader
	implements ObjectListExtender
{
	final String baseSql = "SELECT constant_catalog,  \n" +
                         "       constant_schema, \n" +
                         "       constant_name, \n" +
                         "       data_type, \n" +
                         "       sql as constant_value, \n" +
						             "       remarks \n" +
                         " FROM information_schema.constants ";

	private String getSql(WbConnection connection, String schema, String name)
	{
		StringBuilder sql = new StringBuilder(baseSql.length() + 40);

		sql.append(baseSql);

		boolean whereAdded = false;
		if (StringUtil.isNonBlank(name))
		{
			sql.append(" WHERE constant_name like '");
			sql.append(connection.getMetadata().quoteObjectname(name));
			sql.append("%' ");
			whereAdded = true;
		}

		if (StringUtil.isNonBlank(schema))
		{
			sql.append(whereAdded ? " AND " : " WHERE ");

			sql.append(" constant_schema = '");
			sql.append(connection.getMetadata().quoteObjectname(schema));
			sql.append("'");
		}
		sql.append(" ORDER BY 1, 2 ");

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("H2ConstantReader.getSql()", "Using SQL=\n" + sql);
		}

		return sql.toString();
	}

	public List<H2Constant> getConstantsList(WbConnection connection, String schemaPattern, String namePattern)
	{
		Statement stmt = null;
		ResultSet rs = null;
		Savepoint sp = null;
		List<H2Constant> result = new ArrayList<H2Constant>();
		try
		{
			sp = connection.setSavepoint();
			stmt = connection.createStatementForQuery();
			String sql = getSql(connection, schemaPattern, namePattern);
			rs = stmt.executeQuery(sql);
			while (rs.next())
			{
				String cat = rs.getString("constant_catalog");
				String schema = rs.getString("constant_schema");
				String name = rs.getString("constant_name");
				H2Constant constant = new H2Constant(cat, schema, name);
				int type = rs.getInt("data_type");
				String dataType = SqlUtil.getTypeName(type);
				constant.setDataType(dataType);
				constant.setValue(rs.getString("constant_value"));
				constant.setComment(rs.getString("remarks"));
				result.add(constant);
			}
			connection.releaseSavepoint(sp);
		}
		catch (SQLException e)
		{
			connection.rollback(sp);
			LogMgr.logError("H2ConstantReader.getConstantsList()", "Could not read constants", e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		return result;
	}

	public H2Constant getObjectDefinition(WbConnection connection, DbObject object)
	{
		List<H2Constant> constants = getConstantsList(connection, object.getSchema(), object.getObjectName());
		if (CollectionUtil.isEmpty(constants)) return null;
		return constants.get(0);
	}

	public String getConstantSource(H2Constant constant)
	{
		if (constant == null) return null;
		
		StringBuilder result = new StringBuilder(50);
		result.append("CREATE CONSTANT ");
		result.append(constant.getObjectName());
		result.append(" VALUE ");
		result.append(constant.getValue());
		result.append(";\n");
		if (StringUtil.isNonBlank(constant.getComment()))
		{
			result.append("\nCOMMENT ON CONSTANT " + constant.getObjectName() + " IS '");
			result.append(SqlUtil.escapeQuotes(constant.getComment()));
			result.append("';\n");
		}
		return result.toString();
	}

	public void extendObjectList(WbConnection con, DataStore result, String catalog, String schema, String objects, String[] requestedTypes)
	{
		if (!DbMetadata.typeIncluded("CONSTANT", requestedTypes)) return;

		List<H2Constant> constants = getConstantsList(con, schema, objects);
		if (constants.size() == 0) return;
		for (H2Constant constant : constants)
		{
			int row = result.addRow();
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_CATALOG, null);
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_SCHEMA, constant.getSchema());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_NAME, constant.getObjectName());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_REMARKS, constant.getComment());
			result.setValue(row, DbMetadata.COLUMN_IDX_TABLE_LIST_TYPE, constant.getObjectType());
		}
	}

	public boolean handlesType(String type)
	{
		return StringUtil.equalStringIgnoreCase("CONSTANT", type) || "*".equals(type);
	}

	public boolean handlesType(String[] types)
	{
		if (types == null) return true;
		for (String type : types)
		{
			if (handlesType(type)) return true;
		}
		return false;
	}

	public DataStore getObjectDetails(WbConnection con, DbObject object)
	{
		if (object == null) return null;
		if (!handlesType(object.getObjectType())) return null;

		H2Constant constant = getObjectDefinition(con, object);
		if (constant == null) return null;

		String[] columns = new String[] { "CONSTANT", "DATA_TYPE", "VALUE", "REMARKS" };
		int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR };
		int[] sizes = new int[] { 20, 10, 5, 30, 30 };
		DataStore result = new DataStore(columns, types, sizes);
		result.addRow();
		result.setValue(0, 0, constant.getObjectName());
		result.setValue(0, 1, constant.getDataType());
		result.setValue(0, 2, constant.getValue());
		result.setValue(0, 3, constant.getComment());

		return result;
	}

	public List<String> supportedTypes()
	{
		return CollectionUtil.arrayList("CONSTANT");
	}

	public String getObjectSource(WbConnection con, DbObject object)
	{
		return getConstantSource(getObjectDefinition(con, object));
	}

}
