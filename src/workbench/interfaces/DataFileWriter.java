/*
 * DataFileWriter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
