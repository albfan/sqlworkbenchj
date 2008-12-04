/*
 * TextExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

/**
 * An ExportWriter to generate flat files.
 * 
 * @author  support@sql-workbench.net
 */
public class TextExportWriter
	extends ExportWriter
{
	public TextExportWriter(DataExporter exp)
	{
		super(exp);
	}

	public RowDataConverter createConverter()
	{
		return new TextRowDataConverter();
	}

	public void configureConverter()
	{
		super.configureConverter();
		TextRowDataConverter conv = (TextRowDataConverter)this.converter;
		conv.setDelimiter(exporter.getTextDelimiter());
		conv.setQuoteCharacter(exporter.getTextQuoteChar());
		conv.setQuoteAlways(exporter.getQuoteAlways());
		conv.setEscapeRange(exporter.getEscapeRange());
		conv.setLineEnding(exporter.getLineEnding());
		conv.setWriteClobToFile(exporter.getWriteClobAsFile());
		conv.setQuoteEscaping(exporter.getQuoteEscaping());
		conv.setRowIndexColName(exporter.getRowIndexColumnName());
	}
	
}
