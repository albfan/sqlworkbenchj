/*
 * WbPropertiesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import junit.framework.TestCase;

/**
 *
 * @author thomas
 */
public class WbPropertiesTest
	extends TestCase
	implements PropertyChangeListener
{
  private String changedProperty = null;

	public WbPropertiesTest(String testName)
	{
		super(testName);
	}

	public void testChangeNotification()
	{
    WbProperties props = new WbProperties();
    props.setProperty("test.property", "bla");
    props.setProperty("test2.property", "two");
		props.addPropertyChangeListener(this, "test.property", "test2.property");
    changedProperty = null;
    props.setProperty("test.property", "blub");
    assertEquals("test.property", changedProperty);
    
    changedProperty = null;
    props.setProperty("test2.property", "nothing");
    assertEquals("test2.property", changedProperty);
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		changedProperty = evt.getPropertyName();
	}
}
