package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface JobErrorHandler
{
	final int JOB_CONTINUE = 1;
	final int JOB_IGNORE_ALL = 2;
	final int JOB_ABORT = 3;

	/**
	 * Callback function if an error occurs.
	 * @param errorRow the row in which the error occurred
	 * @param errorColumn the column in which the error occurred (-1 if there was a problem in reading the row)
	 * @param data the data which was processed (if errorColumn > -1 the column value, else the row value)
	 * @param errorMessage the errorMessage from the Job
	 *
	 * @return JOB_CONTINUE the job should ignore the error (if possible and continue) or JOB_ABORT
	 */
	int getActionOnError(int errorRow, int errorColumn, String data, String errorMessage);
}