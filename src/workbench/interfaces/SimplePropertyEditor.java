/*
 * SimplePropertyEditor.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2004, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: info@sql-workbench.net
 *
 */
package workbench.interfaces;

import java.beans.PropertyChangeListener;

/**
 *
 * @author  info@sql-workbench.net
 */
public interface SimplePropertyEditor
{
	void setSourceObject(Object aSource, String aProperty);
	void applyChanges();
	boolean isChanged();
	void addPropertyChangeListener(PropertyChangeListener aListener);
	void removePropertyChangeListener(PropertyChangeListener aListener);
  void setImmediateUpdate(boolean aFlag);
  boolean getImmediateUpdate();
}
