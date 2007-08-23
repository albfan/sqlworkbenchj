/*
 * WbPropertiesTest.java
 * JUnit based test
 *
 * Created on 23. August 2007, 10:10
 */

package workbench.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
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