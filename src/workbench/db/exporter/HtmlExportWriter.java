/*
 * HtmlExportWriter.java
 *
 * Created on August 26, 2004, 10:37 PM
 */

package workbench.db.exporter;

import workbench.storage.ResultInfo;
import workbench.storage.RowDataConverter;

/**
 *
 * @author  workbench@kellerer.org
 */
public class HtmlExportWriter
	extends ExportWriter
{
	
	public HtmlExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter(ResultInfo info)
	{
		return null;
	}
	
}
