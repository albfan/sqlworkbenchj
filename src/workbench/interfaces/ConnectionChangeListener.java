package workbench.interfaces;

import workbench.db.WbConnection;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public interface ConnectionChangeListener
{
	void connectionChanged(WbConnection newConnection);
}
