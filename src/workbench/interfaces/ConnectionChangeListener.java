package workbench.interfaces;

import workbench.db.WbConnection;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface ConnectionChangeListener
{
	void connectionChanged(WbConnection newConnection);
}
