/*
 * Connectable.java
 *
 * Created on September 24, 2004, 9:14 PM
 */

package workbench.interfaces;

import workbench.db.ConnectionProfile;
import workbench.db.WbConnection;

/**
 *
 * @author  workbench@kellerer.org
 */
public interface Connectable
{
	void connectCancelled();
	void connectBegin(ConnectionProfile profile);
	String getConnectionId(ConnectionProfile profile);
	void connectFailed(String error);
	void connected(WbConnection conn);
}
