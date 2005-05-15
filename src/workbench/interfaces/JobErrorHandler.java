/*
 * JobErrorHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface JobErrorHandler
{
	final int JOB_CONTINUE = 1;
	final int JOB_IGNORE_ALL = 2;
	final int JOB_ABORT = 3;

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
}
