/*
 * TextContainer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2010, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 *
 * @author  Thomas Kellerer
 */
public interface TextContainer
{
	String getText();
	String getSelectedText();
	void setSelectedText(String aText);
	void setText(String aText);
	void setCaretPosition(int pos);
	int getCaretPosition();
	int getSelectionStart();
	int getSelectionEnd();
	void select(int start, int end);
	void setEditable(boolean flag);
}
