/*
 * RowDataProducer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.db.importer;

import java.util.Collection;
import java.util.Map;

import workbench.interfaces.JobErrorHandler;

import workbench.util.MessageBuffer;
import workbench.util.ValueConverter;


/**
 *
 * @author  Thomas Kellerer
 */
public interface RowDataProducer
{
	String SKIP_INDICATOR = "$wb_skip$";

	void setReceiver(DataReceiver receiver);
	void start() throws Exception;

	/**
	 * Abort the current import.
	 * 
	 * This is usually called when the user cancels the running SQL statement.
	 */
	void cancel();

	/**
	 * Stop processing the current input file.
	 *
	 * This is used by the DataImporter to signal that all selected rows
	 * were imported (in case not all rows should be imported).
	 */
	void stop();

	MessageBuffer getMessages();
	void setAbortOnError(boolean flag);
	void setErrorHandler(JobErrorHandler handler);
	boolean hasErrors();
	boolean hasWarnings();
	void setValueConverter(ValueConverter converter);

	/**
	 * Return the last "raw" record that was sent to the DataReceiver.
	 * This is used to log invalid records
	 */
	String getLastRecord();

	/**
	 * Return the column value from the input file for each column
	 * passed in to the function.
	 * @param inputFileIndexes the index of each column in the input file
	 * @return for each column index the value in the inputfile
	 */
	Map<Integer, Object> getInputColumnValues(Collection<Integer> inputFileIndexes);

}
