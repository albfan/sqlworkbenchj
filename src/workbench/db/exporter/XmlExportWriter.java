/*
 * XmlExportWriter.java
 *
 * Created on August 26, 2004, 10:37 PM
 */

package workbench.db.exporter;

import java.io.Writer;
import java.sql.ResultSet;
import javax.xml.transform.TransformerException;
import workbench.log.LogMgr;
import workbench.storage.ResultInfo;
import workbench.storage.RowDataConverter;
import workbench.storage.XmlRowDataConverter;
import workbench.util.XsltTransformer;

/**
 *
 * @author  workbench@kellerer.org
 */
public class XmlExportWriter
	extends ExportWriter
{
	
	public XmlExportWriter(DataExporter exp)
	{
		super(exp);
	}
	
	public RowDataConverter createConverter(ResultInfo info)
	{
		return new XmlRowDataConverter(info);
	}

	public void exportFinished()
	{
		super.exportFinished();
		String exportFile = this.exporter.getFullOutputFilename();
		String xsltFile = this.exporter.getXsltTransformation();
		String output = this.exporter.getXsltTransformationOutput();
		if (xsltFile != null && output != null)
		{
			try
			{
				XsltTransformer.transformFile(exportFile, output, xsltFile);
			}
			catch (Exception e)
			{
				LogMgr.logError("DataSpooler.startExport()", "Error when transforming " + output + " using " + xsltFile, e);
			}
		}
	}
}
