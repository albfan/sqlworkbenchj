/*
 * PgCopyImporter.java
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

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;

import workbench.log.LogMgr;

import workbench.db.ColumnIdentifier;
import workbench.db.ConnectionMgr;
import workbench.db.TableIdentifier;
import workbench.db.WbConnection;
import workbench.db.importer.StreamImporter;
import workbench.db.importer.TextImportOptions;

import workbench.util.FileUtil;

/**
 * A class to use PostgreSQL's CopyManager to import a file.
 *
 * @author Thomas Kellerer
 */
public class PgCopyImporter
	implements StreamImporter
{
	private final Object lock = new Object();
	private WbConnection connection;
	private String sql;
	private Reader data;
	private Object copyManager;
	private Method copyIn;
	private boolean useDefaultClassloader;

	public PgCopyImporter(WbConnection conn)
	{
		this.connection = conn;
	}

	/**
	 * Instruct this instance to use the default (system) classloader
	 * instead of the the ConnectionMgr.loadClassFromDriverLib().
	 *
	 * During unit testing the classloader in the ConnectionMgr is not initialized
	 * because all drivers are alread on the classpath. Therefor this switch is needed
	 *
	 * @param flag if true, load the CopyManager from the system classpath, otherwise use the ConnectionMgr.
	 */
	public void setUseDefaultClassloader(boolean flag)
	{
		useDefaultClassloader = flag;
	}

	public boolean isSupported()
	{
		try
		{
			initialize();
			return true;
		}
		catch (Throwable th)
		{
			LogMgr.logDebug("PgCopyImporter.isSupported()", "Error", th);
			return false;
		}
	}

	private void initialize()
		throws ClassNotFoundException
	{
		synchronized (lock)
		{
			try
			{
				Class baseConnClass = null;
				Class copyMgrClass = null;
				if (useDefaultClassloader)
				{
					baseConnClass = Class.forName("org.postgresql.core.BaseConnection");
					copyMgrClass = Class.forName("org.postgresql.copy.CopyManager");
				}
				else
				{
					baseConnClass = ConnectionMgr.getInstance().loadClassFromDriverLib(connection.getProfile(), "org.postgresql.core.BaseConnection");
					copyMgrClass = ConnectionMgr.getInstance().loadClassFromDriverLib(connection.getProfile(), "org.postgresql.copy.CopyManager");
				}

				Constructor constr = copyMgrClass.getConstructor(baseConnClass);
				copyManager = constr.newInstance(connection.getSqlConnection());
				copyIn = copyManager.getClass().getMethod("copyIn", String.class, Reader.class);
			}
			catch (Throwable t)
			{
				LogMgr.logError("PgCopyImporter.createCopyManager()", "Could not create CopyManager", t);
				throw new ClassNotFoundException("CopyManager");
			}
		}
	}

	@Override
	public void setup(TableIdentifier table, List<ColumnIdentifier> columns, Reader in, TextImportOptions options, String encoding)
	{
		sql = createCopyStatement(table, columns, options, encoding);
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
			// As the CopyManager is loaded through the ClassLoader of the DbDriver
			// we cannot have any "hardcoded" references to the PostgreSQL classes
			// that will throw a ClassNotFoundException even though the class was actually loaded
			if (copyManager == null)
			{
				initialize();
			}

			LogMgr.logDebug("PgCopyImporter.processStreamData()", "Sending file contents using: " + this.sql);

			if (copyIn != null)
			{
				Object rows = copyIn.invoke(copyManager, sql, data);
				if (rows instanceof Number)
				{
					return ((Number)rows).longValue();
				}
			}
			throw new SQLException("CopyAPI not available");
		}
		catch (ClassNotFoundException e)
		{
			throw new SQLException("CopyAPI not available", e);
		}
		catch (Exception e)
		{
			Throwable realException = e;
			if (e instanceof InvocationTargetException)
			{
				realException = e.getCause();
			}
			if (realException instanceof SQLException)
			{
				throw (SQLException)realException;
			}
			if (realException instanceof IOException)
			{
				throw (IOException)realException;
			}
			throw new SQLException("Could not copy data", e.getCause() == null ? e : e.getCause());
		}
		finally
		{
			FileUtil.closeQuietely(data);
			data = null;
		}
	}

	public final String createCopyStatement(TableIdentifier table, List<ColumnIdentifier> columns, TextImportOptions options, String encoding)
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
		copySql.append(") FROM stdin WITH (format ");

		boolean useText = options.getTextQuoteChar() == null && options.getContainsHeader() == false && options.getDecode();
		String nullString = options.getNullString();

		if (useText)
		{
			copySql.append("text");
		}
		else
		{
			copySql.append("csv");
			copySql.append(", header ");
			copySql.append(Boolean.toString(options.getContainsHeader()));
			String quote = options.getTextQuoteChar();
			if (quote != null)
			{
				copySql.append(", quote '");
				copySql.append(quote);
				copySql.append('\'');
			}
		}

		copySql.append(", delimiter '");
		String delim = options.getTextDelimiter();
		if (delim.equals("\\t")) delim = "\t";
		copySql.append(delim);
		copySql.append("', encoding '");
		copySql.append(encoding);
		if (nullString == null)
		{
			copySql.append("', NULL ''");
		}
		else
		{
			copySql.append("', NULL '");
			copySql.append(nullString);
			copySql.append('\'');
		}
		copySql.append(")");

		return copySql.toString();
	}
}
