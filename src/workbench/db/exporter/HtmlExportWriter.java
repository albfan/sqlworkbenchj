/*
 * HtmlExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 * An ExportWriter to generate HTML output.
 * 
 * @author  Thomas Kellerer
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
		super.configureConverter();
		HtmlRowDataConverter conv = (HtmlRowDataConverter)this.converter;
		conv.setCreateFullPage(exporter.getCreateFullHtmlPage());
		conv.setEscapeHtml(exporter.getEscapeHtml());
		conv.setHeading(exporter.getHtmlHeading());
		conv.setTrailer(exporter.getHtmlTrailer());
	}
}
