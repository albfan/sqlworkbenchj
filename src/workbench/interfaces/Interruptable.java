/*
 * Interruptable.java
 *
 * Created on July 8, 2003, 11:19 AM
 */

package workbench.interfaces;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface Interruptable
{
	void cancelExecution();
	boolean confirmCancel();
}
