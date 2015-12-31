/*
 * SqlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.db.exporter;

import workbench.log.LogMgr;
import workbench.resource.Settings;

import workbench.db.TableIdentifier;

/**
 * Export data as SQL INSERT statements.
 *
 * @author  Thomas Kellerer
 */
public class SqlExportWriter
	extends ExportWriter
{

	/** Creates a new instance of SqlExportWriter
	 * @param exp The exporter to convert the rows for
	 */
	public SqlExportWriter(DataExporter exp)
	{
		super(exp);
		canAppendStart = true;
	}

	@Override
	public RowDataConverter createConverter()
	{
		return new SqlRowDataConverter(exporter.getConnection());
	}

	@Override
	public void configureConverter()
	{
		super.configureConverter();
		SqlRowDataConverter conv = (SqlRowDataConverter)this.converter;
		conv.setIncludeTableOwner(exporter.getUseSchemaInSql());
		conv.setCommitEvery(exporter.getCommitEvery());
		conv.setChrFunction(exporter.getChrFunction());
		conv.setConcatString(exporter.getConcatString());
		conv.setConcatFunction(exporter.getConcatFunction());
		conv.setDateLiteralType(exporter.getDateLiteralType());

		conv.setBlobMode(exporter.getBlobMode());

		if (exporter.getWriteClobAsFile())
		{
			String encoding = exporter.getEncoding();
			if (encoding == null) encoding = Settings.getInstance().getDefaultFileEncoding();
			conv.setClobAsFile(encoding);
		}

		// the key columns need to be set before the createInsert flag!
		conv.setKeyColumnsToUse(exporter.getKeyColumnsToUse());
		try
		{
			conv.setType(exporter.getExportType());
			conv.setMergeType(exporter.getMergeType());
		}
		catch (IllegalArgumentException e)
		{
			LogMgr.logError("SqlExportWriter.createConverter()", "Illegal SQL type requested. Reverting to INSERT", null);
			conv.setCreateInsert();
		}

		String table = exporter.getTableName();
		if (table != null)
		{
			conv.setAlternateUpdateTable(new TableIdentifier(table, exporter.getConnection()));
		}
		conv.setCreateTable(exporter.isIncludeCreateTable());
	}

}
