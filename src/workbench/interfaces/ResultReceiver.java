/*
 * ResultReceiver.java
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

/**
 * @author support@sql-workbench.net
 */
public interface ResultReceiver
{
	public static enum ShowType
	{
		showNone,
		appendText,
		replaceText,
		logText;
	}
	
	void showResult(String sql, String comment, ShowType how);
	String getTitle();
	
}
