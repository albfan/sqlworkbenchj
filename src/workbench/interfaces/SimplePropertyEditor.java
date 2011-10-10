/*
 * SimplePropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.beans.PropertyChangeListener;

/**
 *
 * @author  Thomas Kellerer
 */
public interface SimplePropertyEditor
{
	void setSourceObject(Object source, String property);
	void applyChanges();
	boolean isChanged();
	void addPropertyChangeListener(PropertyChangeListener listener);
	void removePropertyChangeListener(PropertyChangeListener listener);
	void setImmediateUpdate(boolean flag);
	boolean getImmediateUpdate();
}
