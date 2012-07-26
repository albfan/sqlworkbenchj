/*
 * PgCopyImporter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.db.postgres;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;
import java.util.List;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import workbench.db.ColumnIdentifier;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.StreamImporter;
import workbench.db.importer.TextImportOptions;
import workbench.log.LogMgr;
import workbench.util.FileUtil;

/**
 * A class to use PostgreSQL's CopyManager to import a file.
 *
 * @author Thomas Kellerer
 */
public class PgCopyImporter
	implements StreamImporter
{
	private WbConnection connection;
	private String sql;
	private Reader data;

	public PgCopyImporter(WbConnection conn)
	{
		this.connection = conn;
	}

	@Override
	public void setup(TableIdentifier table, List<ColumnIdentifier> columns, Reader in, TextImportOptions options)
	{
		sql = createCopyStatement(table, columns, options);
		data = in;
	}

	@Override
	public long processStreamData()
		throws SQLException, IOException
	{
		if (data == null || sql == null)
		{
			throw new IllegalStateException("CopyImporter not initialized");
		}

		try
		{
			CopyManager copyMgr = new CopyManager((BaseConnection)connection.getSqlConnection());
			LogMgr.logDebug("PgCopyImporter.processStreamData()", "Sending file contents using: " + this.sql);
			return copyMgr.copyIn(sql, data);
		}
		catch (ClassCastException ce)
		{
			LogMgr.logError("PgCopyImporter.processStreamData()", "Not a Postgres connection!", ce);
			throw new SQLException("No Postgres connection!");
		}
		finally
		{
			FileUtil.closeQuietely(data);
			data = null;
		}
	}

	public final String createCopyStatement(TableIdentifier table, List<ColumnIdentifier> columns, TextImportOptions options)
	{
		StringBuilder copySql = new StringBuilder(100);
		copySql.append("COPY ");
		copySql.append(table.getTableExpression(connection));
		copySql.append(" (");
		for (int i=0; i < columns.size(); i++)
		{
			if (i > 0) copySql.append(',');
			copySql.append(columns.get(i).getColumnName());
		}
		copySql.append(") FROM stdin WITH (format csv");
		copySql.append(", delimiter '");
		copySql.append(options.getTextDelimiter());
		copySql.append("'");
		copySql.append(", header ");
		copySql.append(Boolean.toString(options.getContainsHeader()));
		copySql.append(")");
		return copySql.toString();
	}
}
