/*
 * FilterHighlighter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.renderer;

import java.awt.Color;
import workbench.storage.filter.ColumnExpression;

/**
 *
 * @author Thomas Kellerer
 */
public interface FilterHighlighter 
{
	void setFilterHighlightColor(Color highlight);
	void setFilterHighlighter(ColumnExpression filter);
	void clearFilterHighligher();
}
