/*
 * SqlOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

import java.util.List;

/**
 *
 * @author support@sql-workbench.net
 */
public interface SqlOptions
{
	boolean getCreateTable();
	void setCreateTable(boolean flag);
	void setCommitEvery(int value);
	int getCommitEvery();
	boolean getCreateInsert();
	boolean getCreateUpdate();
	boolean getCreateDeleteInsert();
	void setCreateInsert();
	void setCreateUpdate();
	void setCreateDeleteInsert();
	String getAlternateUpdateTable();
	void setAlternateUpdateTable(String table);
	List<String> getKeyColumns();
	String getDateLiteralType();
}
