/*
 * WbPropertiesTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import workbench.TestUtil;
import static org.junit.Assert.*;
import org.junit.Test;
import workbench.WbTestCase;

/**
 *
 * @author Thomas Kellerer
 */
public class WbPropertiesTest
	extends WbTestCase
	implements PropertyChangeListener
{
  private String changedProperty = null;

	@Test
	public void testMultiLine()
		throws Exception
	{
		TestUtil util = getTestUtil();

		File file = new File(util.getBaseDir(), "multiline.properties");
		TestUtil.writeFile(file,
			"key1=value1 \\\n" +
			"value2\n" +
			"key2=value2\n" +
			"key3=value3\n"
		);
		WbProperties props = new WbProperties();
		props.loadTextFile(file.getAbsolutePath());

		assertEquals("value1 \nvalue2", props.getProperty("key1"));

		File newfile = new File(util.getBaseDir(), "multine_new.properties");
		props.saveToFile(newfile);

		WbProperties newprops = new WbProperties();
		newprops.loadTextFile(newfile.getAbsolutePath());
		assertEquals("value1 \nvalue2", newprops.getProperty("key1"));
	}

	@Test
	public void testComments()
		throws Exception
	{
		TestUtil util = getTestUtil();

		File file = new File(util.getBaseDir(), "myprops.properties");
		TestUtil.writeFile(file,
			"# this is a comment for the first key\n" +
			"# second comment line\n" +
			"firstkey=value1\n" +
			"secondkey=value2\n" +
			"# comment 2\n" +
			"key4=value3\n" +
			"\n" +
			"# no comment\n" +
			"\n" +
			"key5=value5\n" +
			"key6=#\n"
		);
		WbProperties props = new WbProperties();
		props.loadTextFile(file.getAbsolutePath());

		assertEquals("value1", props.getProperty("firstkey"));
		assertEquals("# this is a comment for the first key\n# second comment line", props.getComment("firstkey"));

		assertEquals("value2", props.getProperty("secondkey"));
		assertNull(props.getComment("secondkey"));

		assertEquals("value3", props.getProperty("key4"));
		assertEquals("# comment 2", props.getComment("key4"));

		assertEquals("value5", props.getProperty("key5"));
		assertNull(props.getComment("key5"));

		props.saveToFile(file);

		// Make sure the comments are preserved when saving and re-loading the properties
		props = new WbProperties();
		props.loadTextFile(file.getAbsolutePath());

		assertEquals("value1", props.getProperty("firstkey"));
		assertEquals("# this is a comment for the first key\n# second comment line", props.getComment("firstkey"));

		assertEquals("value2", props.getProperty("secondkey"));
		assertNull(props.getComment("secondkey"));

		assertEquals("value3", props.getProperty("key4"));
		assertEquals("# comment 2", props.getComment("key4"));

		assertEquals("value5", props.getProperty("key5"));
		assertNull(props.getComment("key5"));

		assertEquals("#", props.getProperty("key6"));
	}

	@Test
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
