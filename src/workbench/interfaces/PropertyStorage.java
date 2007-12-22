/*
 * PropertyStorage.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2008, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.interfaces;

/**
 * @author support@sql-workbench.net
 */
public interface PropertyStorage
{
	Object setProperty(String property, String value);
	void setProperty(String property, int value);
	void setProperty(String property, boolean value);
	boolean getBoolProperty(String property, boolean defaultValue);
	int getIntProperty(String property, int defaultValue);
	String getProperty(String property, String defaultValue);
}
