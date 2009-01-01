/*
 * ConnectionChangeListener.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import workbench.db.WbConnection;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface ConnectionChangeListener
{
	void connectionChanged(WbConnection newConnection);
}
