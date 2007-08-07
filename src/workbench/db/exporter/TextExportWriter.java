/*
 * TextExportWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.exporter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import workbench.log.LogMgr;
import workbench.resource.Settings;
import workbench.storage.ResultInfo;
import workbench.util.CharacterRange;
import workbench.util.SqlUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;

/**
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
		conv.setWriteHeader(exporter.getExportHeaders());
		conv.setQuoteCharacter(exporter.getTextQuoteChar());
		conv.setQuoteAlways(exporter.getQuoteAlways());
		conv.setEscapeRange(exporter.getEscapeRange());
		conv.setLineEnding(exporter.getLineEnding());
		conv.setWriteClobToFile(exporter.getWriteClobAsFile());
		conv.setQuoteEscaping(exporter.getQuoteEscaping());
	}
	
}
