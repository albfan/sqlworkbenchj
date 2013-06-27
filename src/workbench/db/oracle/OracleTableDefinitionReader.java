/*
 * OracleTableDefinitionReader.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.db.oracle;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionProfile;
import workbench.db.DataTypeResolver;
import workbench.db.DbMetadata;
import workbench.db.DbSettings;
import workbench.db.JdbcTableDefinitionReader;
import workbench.db.JdbcUtils;
import workbench.db.PkDefinition;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;

import workbench.util.CollectionUtil;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;

/**
 * A class to retrieve meta-information from an Oracle database.
 *
 * It fixes some problems with incorrectly returned data types.
 *
 * We will use our own statement only if the Oracle version is 9i or later and
 * if at least one of the following configuration properties are set:
 * <ul>
 *	<li>workbench.db.oracle.fixcharsemantics</li>
 *	<li>workbench.db.oracle.fixnvarchartype</li>
 * </ul>
 *
 * Additionally if the config property <tt>workbench.db.oracle.fixdatetype</tt> is
 * set to true, DATE columns will always be mapped to Timestamp objects when
 * retrieving data (see {@link #getMapDateToTimestamp()} and
 * {@link #fixColumnType(int)}
 *
 * @author Thomas Kellerer
 */
public class OracleTableDefinitionReader
	extends JdbcTableDefinitionReader
{
	private boolean useOwnSql = true;
	private final OracleDataTypeResolver oraTypes;

	public OracleTableDefinitionReader(WbConnection conn)
	{
		super(conn);
		boolean fixNVARCHAR = fixNVARCHARSemantics();
		boolean checkCharSemantics = Settings.getInstance().getBoolProperty("workbench.db.oracle.fixcharsemantics", true);

		useOwnSql = (JdbcUtils.hasMinimumServerVersion(conn, "8.0") && (checkCharSemantics || fixNVARCHAR));

		// The incorrectly reported search string escape bug was fixed with 11.2
		// The 11.1 and earlier drivers do not report the correct escape character and thus
		// escaping in DbMetadata doesn't return anything if the username (schema) contains an underscore
		if (!JdbcUtils.hasMiniumDriverVersion(conn.getSqlConnection(), "11.1")
			&& Settings.getInstance().getBoolProperty("workbench.db.oracle.fixescapebug", true)
			&& Settings.getInstance().getProperty("workbench.db.oracle.searchstringescape", null) == null)
		{
			LogMgr.logWarning("OracleMetadata.<init>", "Old JDBC driver detected. Turning off wildcard handling for objects retrieval to work around driver bug");
			System.setProperty("workbench.db.oracle.metadata.retrieval.wildcards", "false");
			System.setProperty("workbench.db.oracle.escape.searchstrings", "false");
		}
		oraTypes = new OracleDataTypeResolver(conn);
	}

	protected final boolean fixNVARCHARSemantics()
	{
		return Settings.getInstance().getBoolProperty("workbench.db.oracle.fixnvarchartype", true);
	}

	@Override
	public List<ColumnIdentifier> getTableColumns(TableIdentifier table, DataTypeResolver typeResolver)
		throws SQLException
	{
		if (!useOwnSql)
		{
			return super.getTableColumns(table, typeResolver);
		}

		PkDefinition primaryKey = table.getPrimaryKey();
		Set<String> pkColumns = CollectionUtil.caseInsensitiveSet();

		if (primaryKey != null)
		{
			pkColumns.addAll(primaryKey.getColumns());
		}

		DbSettings dbSettings = dbConnection.getDbSettings();
		DbMetadata dbmeta = dbConnection.getMetadata();
		String schema = StringUtil.trimQuotes(table.getSchema());
		String tablename = StringUtil.trimQuotes(table.getTableName());

		List<ColumnIdentifier> columns = new ArrayList<ColumnIdentifier>();

		boolean includeVirtualColumns = JdbcUtils.hasMinimumServerVersion(dbConnection, "11.0");

		ResultSet rs = null;
		PreparedStatement pstmt = null;

		try
		{
			pstmt = prepareColumnsStatement(schema, tablename);
			rs = pstmt.executeQuery();

			while (rs != null && rs.next())
			{
				String colName = rs.getString("COLUMN_NAME");
				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				ColumnIdentifier col = new ColumnIdentifier(dbmeta.quoteObjectname(colName), oraTypes.fixColumnType(sqlType, typeName));

				int size = rs.getInt("COLUMN_SIZE");
				int digits = rs.getInt("DECIMAL_DIGITS");
				if (rs.wasNull()) digits = -1;

				String remarks = rs.getString("REMARKS");
				String defaultValue = rs.getString("COLUMN_DEF");
				if (defaultValue != null && dbSettings.trimDefaults())
				{
					defaultValue = defaultValue.trim();
				}

				int position = rs.getInt("ORDINAL_POSITION");

				String nullable = rs.getString("IS_NULLABLE");
				String byteOrChar = rs.getString("CHAR_USED");
				int charSemantics = oraTypes.getDefaultCharSemantics();

				if (StringUtil.isEmptyString(byteOrChar))
				{
					charSemantics = oraTypes.getDefaultCharSemantics();
				}
				else if ("B".equals(byteOrChar.trim()))
				{
					charSemantics = OracleDataTypeResolver.BYTE_SEMANTICS;
				}
				else if ("C".equals(byteOrChar.trim()))
				{
					charSemantics = OracleDataTypeResolver.CHAR_SEMANTICS;
				}

				boolean isVirtual = false;
				if (includeVirtualColumns)
				{
					String virtual = rs.getString("VIRTUAL_COLUMN");
					isVirtual = StringUtil.stringToBool(virtual);
				}
				String display = oraTypes.getSqlTypeDisplay(typeName, sqlType, size, digits, charSemantics);

				col.setDbmsType(display);
				col.setIsPkColumn(pkColumns.contains(colName));
				col.setIsNullable("YES".equalsIgnoreCase(nullable));

				if (isVirtual && sqlType != Types.OTHER)
				{
					String exp = "GENERATED ALWAYS AS (" + defaultValue + ")";
					col.setComputedColumnExpression(exp);
				}
				else
				{
					col.setDefaultValue(defaultValue);
				}
				col.setComment(remarks);
				col.setColumnSize(size);
				col.setDecimalDigits(digits);
				col.setPosition(position);
				columns.add(col);
			}
		}
		finally
		{
			SqlUtil.closeAll(rs, pstmt);
		}
		return columns;
	}

	public static String getDecodeForDataType(String colname, boolean fixNVARCHAR, boolean mapDateToTimestamp)
	{
			return
			"     DECODE(" + colname + ", \n" +
			"            'CHAR', " + Types.CHAR + ", \n" +
			"            'VARCHAR2', " + Types.VARCHAR + ", \n" +
			"            'NVARCHAR2', " + (fixNVARCHAR ? Types.NVARCHAR : Types.OTHER) + ", \n" +
			"            'NCHAR', " + (fixNVARCHAR ? Types.NCHAR : Types.OTHER) + ", \n" +
			"            'NUMBER', " + Types.DECIMAL + ", \n" +
			"            'LONG', " + Types.LONGVARCHAR + ", \n" +
			"            'DATE', " + (mapDateToTimestamp ? Types.TIMESTAMP : Types.DATE) + ", \n" +
			"            'RAW', " + Types.VARBINARY + ", \n" +
			"            'LONG RAW', " + Types.LONGVARBINARY + ", \n" +
			"            'BLOB', " + Types.BLOB + ", \n" +
			"            'CLOB', " + Types.CLOB + ", \n" +
			"            'NCLOB', " + (fixNVARCHAR ? Types.NCLOB : Types.OTHER) + ", \n" +
			"            'BFILE', -13, \n" +
			"            'FLOAT', " + Types.FLOAT + ", \n" +
			"            'TIMESTAMP(6)', " + Types.TIMESTAMP + ", \n" +
			"            'TIMESTAMP(6) WITH TIME ZONE', -101, \n" +
			"            'TIMESTAMP(6) WITH LOCAL TIME ZONE', -102, \n" +
			"            'INTERVAL YEAR(2) TO MONTH', -103, \n" +
			"            'INTERVAL DAY(2) TO SECOND(6)', -104, \n" +
			"            'BINARY_FLOAT', 100, \n" +
			"            'BINARY_DOUBLE', 101, " + Types.OTHER + ")";
	}

	private PreparedStatement prepareColumnsStatement(String schema, String table)
		throws SQLException
	{
		boolean fixNVARCHAR = fixNVARCHARSemantics();


		// Oracle 9 and above reports a wrong length if NLS_LENGTH_SEMANTICS is set to char
    // this statement fixes this problem and also removes the usage of LIKE
    // to speed up the retrieval.
		final String sql1 =
			"SELECT /* SQLWorkbench */ t.column_name AS column_name,  \n" +
			      getDecodeForDataType("t.data_type", fixNVARCHAR, OracleUtils.getMapDateToTimestamp(dbConnection)) + " AS data_type, \n" +
			"     t.data_type AS type_name,  \n" +
			"     decode(t.data_type, 'VARCHAR', t.char_length, \n" +
			"                         'VARCHAR2', t.char_length, \n" +
			"                         'NVARCHAR', t.char_length, \n" +
			"                         'NVARCHAR2', t.char_length, \n" +
			"                         'CHAR', t.char_length, \n" +
			"                         'NCHAR', t.char_length, \n" +
			"                         'NUMBER', nvl(t.data_precision, 38), \n" +  // if data_precision is NULL for NUMBERs this is the same as 38
			"                         'FLOAT', t.data_precision, \n" +
			"                         'REAL', t.data_precision, \n" +
			"            t.data_length) AS column_size,  \n" +
			"     case \n" +
			"        when t.data_type = 'NUMBER' and t.data_precision is null then -127 \n" +
			"        else t.data_scale \n" +
			"     end AS decimal_digits,  \n" +
			"     DECODE(t.nullable, 'N', 0, 1) AS nullable, \n";

		String sql2 =
			"     t.data_default AS column_def,  \n" +
			"     t.char_used, \n" +
			"     t.column_id AS ordinal_position,   \n" +
			"     DECODE(t.nullable, 'N', 'NO', 'YES') AS is_nullable ";

		boolean includeVirtualColumns = JdbcUtils.hasMinimumServerVersion(dbConnection, "11.0");
		if (includeVirtualColumns)
		{
			// for some reason XMLTYPE columns and "table type" columns are returned with virtual_column = 'YES'
			// which seems like a bug in all_tab_cols....
			sql2 +=
				",\n     case \n" +
				"          when data_type <> 'XMLTYPE' and DATA_TYPE_OWNER is null THEN t.virtual_column \n" +
				"          else 'NO' \n" +
				"      end as virtual_column \n" +
				"FROM all_tab_cols t";
		}
		else
		{
			sql2 += "\nFROM all_tab_columns t";
		}

		String where = "\nWHERE t.owner = ? AND t.table_name = ? \n";
		if (includeVirtualColumns)
		{
			where += "  AND t.hidden_column = 'NO' ";
		}
		final String comment_join = "\n  LEFT JOIN all_col_comments c ON t.owner = c.owner AND t.table_name = c.table_name AND t.column_name = c.column_name";
		final String order = "\nORDER BY t.column_id";

		final String sql_comment = sql1 + "     c.comments AS remarks, \n" + sql2 + comment_join + where + order;
		final String sql_no_comment = sql1 + "       null AS remarks, \n" + sql2 + where + order;

		String sql = null;

		if (OracleUtils.getRemarksReporting(dbConnection))
		{
			sql = sql_comment;
		}
		else
		{
			sql = sql_no_comment;
		}

		int pos = table != null ? table.indexOf('@') : -1;

		if (pos > 0)
		{
			String dblink = table.substring(pos);
			table = table.substring(0, pos);
			sql = StringUtil.replace(sql, "all_tab_columns", "all_tab_columns" + dblink);
			sql = StringUtil.replace(sql, "all_col_comments", "all_col_comments" + dblink);
			String dblinkOwner = this.getDbLinkTargetSchema(dblink.substring(1), schema);
			if (StringUtil.isEmptyString(schema) && !StringUtil.isEmptyString(dblinkOwner))
			{
				schema = dblinkOwner;
			}
		}

		PreparedStatement stmt = dbConnection.getSqlConnection().prepareStatement(sql);
		stmt.setString(1, schema);
		stmt.setString(2, table);
		if (Settings.getInstance().getDebugMetadataSql())
		{
			LogMgr.logDebug("OracleMetadata.prepareColumnsStatement()", "Using: " + sql);
		}
		return stmt;
	}

	private String getDbLinkTargetSchema(String dblink, String owner)
	{
		String sql = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		String linkOwner = null;

		// check if DB Link name contains a domain
		// If yes, use the link name directly
		if (dblink.indexOf('.') > 0)
		{
			sql = "SELECT /* SQLWorkbench */ username FROM all_db_links WHERE db_link = ? AND (owner = ? or owner = 'PUBLIC')";
		}
		else
		{
			// apparently Oracle stores all DB Links with the default domain
			// appended. I did not find a reliable way to retrieve the domain
			// name, so I'm using a like to retrieve the definition
			// hoping that there won't be two dblinks with the same name
			// but different domains
			sql = "SELECT /* SQLWorkbench */ username FROM all_db_links WHERE db_link like ? AND (owner = ? or owner = 'PUBLIC')";
			dblink += ".%";
		}

		try
		{
			synchronized (dbConnection)
			{
				stmt = dbConnection.getSqlConnection().prepareStatement(sql);
				stmt.setString(1, dblink);
				stmt.setString(2, owner);
				rs = stmt.executeQuery();
				if (rs.next())
				{
					linkOwner = rs.getString(1);
				}
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("OracleMetadata.getDblinkSchema()", "Error retrieving target schema for DBLINK " + dblink, e);
		}
		finally
		{
			SqlUtil.closeAll(rs, stmt);
		}

		return linkOwner;
	}

	/**
	 * Checks if the property "remarksReporting" is enabled for the given connection.
	 *
	 * @param con the connection to test
	 * @return true if the driver returns comments for tables and columns
	 */
	public static boolean remarksEnabled(WbConnection con)
	{
		if (con == null) return false;
		ConnectionProfile prof = con.getProfile();
		Properties props = prof.getConnectionProperties();
		String value = "false";
		if (props != null)
		{
			value = props.getProperty("remarksReporting", "false");
		}
		return "true".equals(value);
	}

}
