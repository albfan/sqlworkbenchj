/*
 * RowDataProducer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
