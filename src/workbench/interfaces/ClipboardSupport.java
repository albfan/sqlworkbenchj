/*
 * ClipboardSupport.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author  info@sql-workbench.net
 */
public interface ClipboardSupport
{
	/**
	 *	Copy the currently selected contents into the clipboard
	 */
	void copy();

	/**
	 *	Select the entire Text
	 */
	void selectAll();

	/**
	 *	Delete the currently selected text without copying it
	 *	into the system clipboard
	 */
	void clear();

	/**
	 *	Delete the currently selected text and put it into
	 *	the clipboard
	 */
	void cut();

	/**
	 *	Paste the contents of the clipboard into
	 *	the component
	 */
	void paste();
}

