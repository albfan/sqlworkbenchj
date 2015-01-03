/*
 * PostgresColumnEnhancer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnDefinitionEnhancer;
import workbench.db.ColumnIdentifier;
import workbench.db.JdbcUtils;
import workbench.db.TableDefinition;
import workbench.db.WbConnection;

import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A column enhancer to update column definitions for PostgreSQL.
 *
 * The following additional information is updated:
 * <ul>
 * <li>the column collation available for versions >= PostgreSQL 9.1</li>
 * <li>The array dimensions so that arrays are displayed correctly.</li>
 * </ul>
 *
 * @author Thomas Kellerer
 */
public class PostgresColumnEnhancer
	implements ColumnDefinitionEnhancer
{
	public static final int STORAGE_PLAIN = 1;
	public static final int STORAGE_MAIN = 2;
	public static final int STORAGE_EXTERNAL = 3;
	public static final int STORAGE_EXTENDED = 4;

	public static final String PROP_SHOW_REAL_SERIAL_DEF = "workbench.db.postgresql.serial.show";

	@Override
	public void updateColumnDefinition(TableDefinition table, WbConnection conn)
	{
		if (JdbcUtils.hasMinimumServerVersion(conn, "9.1"))
		{
			readColumnInfo(table, conn);
		}

		if (JdbcUtils.hasMinimumServerVersion(conn, "8.0"))
		{
			updateArrayTypes(table, conn);
		}

		updateSerials(table);
	}

	public static String getStorageOption(int storage)
	{
		switch (storage)
		{
			case STORAGE_EXTENDED:
				return "EXTENDED";
			case STORAGE_EXTERNAL:
				return "EXTERNAL";
			case STORAGE_MAIN:
				return "MAIN";
			case STORAGE_PLAIN:
				return "PLAIN";
		}
		return null;
	}

	private void updateSerials(TableDefinition table)
	{
		for (ColumnIdentifier col : table.getColumns())
		{
			String dbmsType = col.getDbmsType();
			String defaultValue = col.getDefaultValue();
			if (dbmsType.endsWith("serial") && defaultValue != null)
			{
				// The nextval() call is returned with a full qualified name if the
				// sequence is not in the current schema.
				// to avoid calling WbConnection.getCurrentSchema() for each default value
				// I'm just checking for a . in the default value which would indicate a fully qualified sequence
				String expectedDefault = "nextval('" ;
				if (defaultValue.indexOf('.') > -1)
				{
					expectedDefault += table.getTable().getRawSchema()+ ".";
				}
				expectedDefault += table.getTable().getRawTableName() + "_" + col.getColumnName() + "_seq'::regclass)";

				if (Settings.getInstance().getBoolProperty(PROP_SHOW_REAL_SERIAL_DEF, true) && (defaultValue.equals(expectedDefault)))
				{
					col.setDefaultValue(null);
				}
				else
				{
					if (dbmsType.equals("serial"))
					{
						col.setDbmsType("integer");
					}
					if (dbmsType.equals("bigserial"))
					{
						col.setDbmsType("bigint");
					}
				}
			}
		}
	}

	private void updateArrayTypes(TableDefinition table, WbConnection conn)
	{
		int arrayCols = 0;
		for (ColumnIdentifier col : table.getColumns())
		{
			if (col.getDataType() == Types.ARRAY)
			{
				arrayCols ++;
			}
		}

		if (arrayCols == 0) return;

		String sql =
			"select att.attname, att.attndims, pg_catalog.format_type(atttypid, NULL) as display_type \n" +
			"from pg_attribute att \n" +
			"  join pg_class tbl on tbl.oid = att.attrelid  \n" +
			"  join pg_namespace ns on tbl.relnamespace = ns.oid  \n" +
			"where tbl.relname = ?   \n" +
			"  and ns.nspname = ? \n" +
			"  and att.attndims > 0";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresColumnEnhancer.updateArrayTypes()", "PostgresColumnEnhancer using SQL=\n" + SqlUtil.replaceParameters(sql, table.getTable().getTableName(), table.getTable().getSchema()));
		}

		PreparedStatement stmt = null;
		ResultSet rs = null;

		HashMap<String, ArrayDef> dims = new HashMap<>(table.getColumnCount());

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTable().getTableName());
			stmt.setString(2, table.getTable().getSchema());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				ArrayDef def = new ArrayDef();
				String colname = rs.getString(1);
				def.numDims = rs.getInt(2);
				def.formattedType = rs.getString(3);
				dims.put(colname, def);
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("PostgresColumnEnhancer.updateArrayTypes()", "Could not read column collations", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
		for (ColumnIdentifier col : table.getColumns())
		{
			ArrayDef def = dims.get(col.getColumnName());
			if (def == null) continue;
			String type = def.formattedType;
			for (int i=0; i < def.numDims - 1; i++)
			{
				type += "[]";
			}
			col.setDbmsType(type);
		}
	}

	private void readColumnInfo(TableDefinition table, WbConnection conn)
	{
		PreparedStatement stmt = null;
		ResultSet rs = null;

		String sql =
			"select att.attname, col.collcollate,  \n" +
			"       case  \n" +
			"          when att.attlen = -1 then att.attstorage \n" +
			"          else null \n" +
			"       end as attstorage \n" +
			"from pg_attribute att  \n" +
			"  join pg_class tbl on tbl.oid = att.attrelid   \n" +
			"  join pg_namespace ns on tbl.relnamespace = ns.oid   \n" +
			"  left join pg_collation col on att.attcollation = col.oid \n" +
			"where tbl.relname = ? \n" +
			"  and ns.nspname = ? \n" +
			"  and not att.attisdropped";

		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("PostgresColumnEnhancer.readCollations()", "Retrieving column information using SQL=\n" + sql);
		}

		try
		{
			stmt = conn.getSqlConnection().prepareStatement(sql);
			stmt.setString(1, table.getTable().getTableName());
			stmt.setString(2, table.getTable().getSchema());
			rs = stmt.executeQuery();
			while (rs.next())
			{
				String colname = rs.getString(1);
				String collation = rs.getString(2);
				String storage = rs.getString(3);
				ColumnIdentifier col = table.findColumn(colname);
				if (col == null) continue;

				if (StringUtil.isNonEmpty(collation))
				{
					col.setCollation(collation);
					col.setCollationExpression(" COLLATE \"" + collation + "\"");
				}

				if (storage != null && !storage.isEmpty())
				{
					switch (storage.charAt(0))
					{
						case 'p':
							col.setPgStorage(STORAGE_PLAIN);
							break;
						case 'm':
							col.setPgStorage(STORAGE_MAIN);
							break;
						case 'e':
							col.setPgStorage(STORAGE_EXTERNAL);
							break;
						case 'x':
							col.setPgStorage(STORAGE_EXTENDED);
							break;
					}
				}
			}
		}
		catch (SQLException ex)
		{
			LogMgr.logError("PostgresColumnEnhancer.readColumnInfo()", "Could not read column collations", ex);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}
	}
	class ArrayDef
	{
		int numDims;
		String formattedType;
	}

}
