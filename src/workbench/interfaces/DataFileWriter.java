/*
 * DataFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.io.File;
import java.io.IOException;
import workbench.storage.ColumnData;

/**
 *
 * @author Thomas Kellerer
 */
public interface DataFileWriter
{
	/**
	 * Creates File object which can be used to write the
	 * BLOB data to an external file
	 */
	File generateDataFileName(ColumnData column)
		throws IOException;
	
	/**
	 * Write the data contained in the value object
	 * to the File object specified by outputFile
	 */
	long writeBlobFile(Object value, File outputFile)
		throws IOException;
	
	/**
	 * Write the String to the external file, using the given encoding
	 */
	void writeClobFile(String value, File outputFile, String encoding)
		throws IOException;

	/**
	 * Returns the base directory in which blob
	 * files should be created
	 */
	File getBaseDir();
}
