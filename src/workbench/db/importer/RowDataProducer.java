/*
 * RowDataProducer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.db.importer;

import workbench.interfaces.JobErrorHandler;


/**
 *
 * @author  support@sql-workbench.net
 */
public interface RowDataProducer
{
	public static final String SKIP_INDICATOR = "$wb_skip$";
	
	void setReceiver(RowDataReceiver receiver);
	void start() throws Exception;
	void cancel();
	void stop();
	String getMessages();
	void setAbortOnError(boolean flag);
	void setErrorHandler(JobErrorHandler handler);
}
