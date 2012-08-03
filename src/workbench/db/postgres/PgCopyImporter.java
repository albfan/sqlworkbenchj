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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import org.postgresql.copy.CopyManager;
import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
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

	public boolean isSupported()
	{
		try
		{
			return createCopyManager() != null;
		}
		catch (Throwable th)
		{
			return false;
		}
	}

	private CopyManager createCopyManager()
		throws SQLException
	{
		try
		{
			Class drvClass = connection.getSqlConnection().getClass();

			Method getMgr = drvClass.getMethod("getCopyAPI", (Class[])null);
			Object result = getMgr.invoke(connection.getSqlConnection(), (Object[])null);
			return (CopyManager)result;
		}
		catch (Throwable th)
		{
			LogMgr.logError("PgCopyImporter.createCopyManager()", "Could not create CopyManager", th);
			throw new SQLException("CopyManager not available");
		}
	}

	private CopyManager _createCopyManager()
		throws ClassNotFoundException
	{
		Class copyMgrClass = ConnectionMgr.getInstance().loadClassFromDriverLib(connection.getProfile(), "org.postgresql.copy.CopyManager");
		Class baseConnClass = ConnectionMgr.getInstance().loadClassFromDriverLib(connection.getProfile(), "org.postgresql.core.BaseConnection");

		try
		{
			Constructor constr = copyMgrClass.getConstructor(baseConnClass);
			Object instance = constr.newInstance(connection.getSqlConnection());
			return (CopyManager)instance;
		}
		catch (Throwable t)
		{
			LogMgr.logError("PgCopyImporter.createCopyManager()", "Could not create CopyManager", t);
			throw new ClassNotFoundException("CopyManager");
		}
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
			CopyManager copyMgr = createCopyManager(); //new CopyManager((BaseConnection)connection.getSqlConnection());
			LogMgr.logDebug("PgCopyImporter.processStreamData()", "Sending file contents using: " + this.sql);
			return copyMgr.copyIn(sql, data);
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
