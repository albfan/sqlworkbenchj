/*
 * ExecutionListener.java
 *
 * Created on October 16, 2003, 12:40 PM
 */

package workbench.interfaces;

import workbench.db.WbConnection;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface DbExecutionListener
{
	void executionStart(WbConnection conn, Object source);
	void executionEnd(WbConnection conn, Object source);
}
