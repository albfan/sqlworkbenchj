/*
 * TextExportWriter.java
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
public class TextExportWriter
	extends ExportWriter
{
	
	/** Creates a new instance of TextExportWriter */
	public TextExportWriter(DataExporter exp)
	{
		super(exp);
	}
	
	public RowDataConverter createConverter(ResultInfo info)
	{
		return null;
	}
	
}
