/*
 * JobErrorHandler.java
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
