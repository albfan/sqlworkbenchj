/*
 * SpreadSheetOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

/**
 *
 * @author Thomas Kellerer
 */
public interface SpreadSheetOptions
{
	String getPageTitle();
	void setPageTitle(String title);
	boolean getExportHeaders();
	void setExportHeaders(boolean flag);
	boolean getCreateInfoSheet();
	void setCreateInfoSheet(boolean flag);
	boolean getCreateFixedHeaders();
	void setCreateFixedHeaders(boolean flag);
	boolean getCreateAutoFilter();
	void setCreateAutoFilter(boolean flag);
}
