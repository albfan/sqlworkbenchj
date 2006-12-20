/*
 * ResultContainer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */

package workbench.interfaces;

/**
 *
 * @author thomas
 */
public interface ResultReceiver
{
	public static enum ShowType
	{
		showNone,
		appendText,
		replaceText;
	}
	
	void showResult(String sql, String comment, ShowType how);
	String getTitle();
	
}
