/*
 * SqlExportWriter.java
 *
 * Created on August 26, 2004, 10:39 PM
 */

package workbench.db.exporter;

import workbench.storage.ResultInfo;
import workbench.storage.RowDataConverter;

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
		return null;
	}
	
}
