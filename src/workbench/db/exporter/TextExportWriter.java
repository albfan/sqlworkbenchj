/*
 * TextExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.db.exporter;

import workbench.storage.ResultInfo;

/**
 *
 * @author  info@sql-workbench.net
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
		TextRowDataConverter converter = new TextRowDataConverter(info);
		converter.setDelimiter(exporter.getTextDelimiter());
		converter.setWriteHeader(exporter.getExportHeaders());
		converter.setQuoteCharacter(exporter.getTextQuoteChar());
		converter.setCleanNonPrintable(exporter.getCleanupCarriageReturns());
		converter.setQuoteAlways(exporter.getQuoteAlways());
		converter.setEscapeRange(exporter.getEscapeRange());
		converter.setEncodingUsed(exporter.getEncoding());
		converter.setLineEnding(exporter.getLineEnding());
		return converter;
	}

}
