/*
 * RowDataProducer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.interfaces.JobErrorHandler;
import workbench.util.MessageBuffer;
import workbench.util.ValueConverter;


/**
 *
 * @author  support@sql-workbench.net
 */
public interface RowDataProducer
{
	String SKIP_INDICATOR = "$wb_skip$";
	
	void setReceiver(RowDataReceiver receiver);
	void start() throws Exception;
	void cancel();
	void stop();
	MessageBuffer getMessages();
	void setAbortOnError(boolean flag);
	void setErrorHandler(JobErrorHandler handler);
	boolean hasErrors();
	boolean hasWarnings();
	void setValueConverter(ValueConverter converter);
	
	/**
	 * Return the last "raw" record that was sent to the RowDataReceiver.
	 * This is used to log invalid records
	 */
	String getLastRecord();
	
	boolean isCancelled();
}
