/*
 * SqlExportWriter.java
 *
 * Created on August 26, 2004, 10:39 PM
 */

package workbench.db.exporter;

import workbench.storage.ResultInfo;
import workbench.db.exporter.RowDataConverter;

/**
 *
 * @author  workbench@kellerer.org
 */
public class SqlExportWriter
	extends ExportWriter
{
	
	/** Creates a new instance of SqlExportWriter */
	public SqlExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter(ResultInfo info)
	{
		SqlRowDataConverter converter = new SqlRowDataConverter(info);
		converter.setOriginalConnection(exporter.getConnection());
		converter.setCommitEvery(exporter.getCommitEvery());
		converter.setChrFunction(exporter.getChrFunction());
		converter.setConcatString(exporter.getConcatString());
		converter.setConcatFunction(exporter.getConcatFunction());
		// the key columns need to be set before the createInsert flag!
		converter.setKeyColumnsToUse(exporter.getKeyColumnsToUse());
		converter.setCreateInsert(exporter.getCreateSqlInsert());
		converter.setSql(exporter.getSql());
		converter.setAlternateUpdateTable(exporter.getTableName());
		converter.setCreateTable(exporter.isIncludeCreateTable());
		return converter;
	}
	
}
