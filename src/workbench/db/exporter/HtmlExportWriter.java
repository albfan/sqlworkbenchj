/*
 * HtmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 * An ExportWriter to generate HTML output.
 * 
 * @author  support@sql-workbench.net
 * @see HtmlRowDataConverter
 */
public class HtmlExportWriter
	extends ExportWriter
{
	
	public HtmlExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		return new HtmlRowDataConverter();
	}

	public void configureConverter()
	{
		HtmlRowDataConverter conv = (HtmlRowDataConverter)this.converter;
		conv.setPageTitle(this.exporter.getPageTitle());
		conv.setCreateFullPage(exporter.getCreateFullHtmlPage());
		conv.setEscapeHtml(exporter.getEscapeHtml());
		conv.setHeading(exporter.getHtmlHeading());
		conv.setTrailer(exporter.getHtmlTrailer());
		
	}
}
