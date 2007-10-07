/*
 * ResourceMgrTest.java
 * 
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 * 
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.resource;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.ResourceBundle;
import junit.framework.TestCase;

/**
 *
 * @author support@sql-workbench.net
 */
public class ResourceMgrTest extends TestCase
{

	public ResourceMgrTest(String testName)
	{
		super(testName);
	}

	/**
	 * Make sure all keys are translated.
	 */
	public void testTranslation()
	{
		Locale en = new Locale("en");
		Locale de = new Locale("de");
		
		ResourceBundle enBundle = ResourceMgr.getResourceBundle(en);
		ResourceBundle deBundle = ResourceMgr.getResourceBundle(de);
		
		HashSet<String> ignoreKeys = new HashSet<String>();
		ignoreKeys.add("TxtBuildDate");
		ignoreKeys.add("TxtBuildNumber");
		
		Enumeration<String> enKeys = enBundle.getKeys();
		while (enKeys.hasMoreElements())
		{
			String key = enKeys.nextElement();
			if (ignoreKeys.contains(key)) continue;
			String enValue = enBundle.getString(key);
			String deValue = deBundle.getString(key);
			assertNotSame("Key " + key + " not translated!", enValue, deValue);
		}
	}
	
}
