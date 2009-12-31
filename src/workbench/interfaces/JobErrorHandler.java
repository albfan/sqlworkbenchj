/*
 * JobErrorHandler.java
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

/**
 *
 * @author  Thomas Kellerer
 */
public interface JobErrorHandler
{
	int JOB_CONTINUE = 1;
	int JOB_IGNORE_ALL = 2;
	int JOB_ABORT = 3;

	/**
	 * Callback function if an error occurs.
	 * @param errorRow the row in which the error occurred
	 * @param errorColumn the column in which the error occurred (null if there was a problem in reading the row)
	 * @param data the data which was processed (if errorColumn != null the column value, else the row value)
	 * @param errorMessage the errorMessage from the Job
	 *
	 * @return JOB_CONTINUE the job should ignore the error (if possible and continue) or JOB_ABORT
	 */
	int getActionOnError(int errorRow, String errorColumn, String data, String errorMessage);
	
	/**
	 * A fatal error has occured and should be displayed to the user. The backend worker
	 * will stop its process after calling this.
	 */
	void fatalError(String msg);
}
