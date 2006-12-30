/*
 * TextContainer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author  support@sql-workbench.net
 */
public interface TextContainer
{
	String getText();
	String getSelectedText();
	void setSelectedText(String aText);
	void setText(String aText);
	void setCaretPosition(int pos);
	void setEditable(boolean flag);
}
