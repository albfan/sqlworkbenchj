/*
 * HtmlOptions.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2005, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.gui.dialogs.export;

/**
 *
 * @author info@sql-workbench.net
 */
public interface HtmlOptions
{
	String getPageTitle();
	void setPageTitle(String title);
	boolean getCreateFullPage();
	void setCreateFullPage(boolean flag);
	boolean getEscapeHtml();
	void setEscapeHtml(boolean flag);
}
