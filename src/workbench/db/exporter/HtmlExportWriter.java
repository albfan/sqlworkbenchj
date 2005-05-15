/*
 * HtmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.storage.ResultInfo;

/**
 *
 * @author  support@sql-workbench.net
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
