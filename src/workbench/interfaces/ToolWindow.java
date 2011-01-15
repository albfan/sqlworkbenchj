/*
 * ToolWindow.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;
import javax.swing.JFrame;
import workbench.db.WbConnection;
/**
 * @author Thomas Kellerer
 */
public interface ToolWindow
{
	/**
	 * Disconnect and close the tool window
	 */
	void closeWindow();

	/**
	 * Disconnect the tool window
	 */
	void disconnect();

	void activate();
	
	WbConnection getConnection();

	JFrame getWindow();
}
