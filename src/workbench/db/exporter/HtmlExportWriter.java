/*
 * HtmlExportWriter.java
 *
 * Created on August 26, 2004, 10:37 PM
 */

package workbench.db.exporter;

import workbench.db.exporter.HtmlRowDataConverter;
import workbench.storage.ResultInfo;
import workbench.db.exporter.RowDataConverter;

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
		HtmlRowDataConverter converter = new HtmlRowDataConverter(info);
		converter.setPageTitle(this.exporter.getHtmlTitle());
		converter.setCreateFullPage(exporter.getCreateFullHtmlPage());
		converter.setEscapeHtml(exporter.getEscapeHtml());
		return converter;
	}
	
}
