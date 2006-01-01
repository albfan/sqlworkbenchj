/*
 * DbData.java
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
 * @author  thomas
 */
public interface DbData
{
	long addRow();
	void deleteRow();
	boolean startEdit();
	int duplicateRow();
	void endEdit();
}
