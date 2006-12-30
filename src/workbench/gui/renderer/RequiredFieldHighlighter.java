/*
 * RequiredFieldHighlighter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;

/**
 * @author support@sql-workbench.net
 */
public interface RequiredFieldHighlighter
{
	void setHighlightColumns(boolean[] cols);
	void setEditingRow(int row);
	void setHighlightBackground(Color requiredColor);
}
