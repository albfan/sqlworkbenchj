/*
 * ResourceMgrTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import workbench.WbTestCase;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import workbench.util.FileUtil;
import static org.junit.Assert.*;
import workbench.util.CollectionUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ResourceMgrTest
	extends WbTestCase
{

	public ResourceMgrTest()
	{
		super("ResourceMgrTest");
	}

	/**
	 * Make sure all keys are translated.
	 */
	@Test
	public void testTranslation()
	{
		Locale en = new Locale("en");
		Locale de = new Locale("de");

		ResourceBundle enBundle = ResourceMgr.getResourceBundle(en);
		ResourceBundle deBundle = ResourceMgr.getResourceBundle(de);
		assertNotNull(enBundle);
		assertNotNull(deBundle);

		HashSet<String> wrongKeys = new HashSet<String>();
		InputStream in = getClass().getResourceAsStream("ignore_keys.txt");
		List<String> keys = FileUtil.getLines(new BufferedReader(new InputStreamReader(in)), true);
		HashSet<String> ignoreKeys = new HashSet<String>(keys);

		Enumeration<String> enKeys = enBundle.getKeys();
		Set<String> wrongParameters = CollectionUtil.caseInsensitiveSet();
		while (enKeys.hasMoreElements())
		{
			String key = enKeys.nextElement();
			if (ignoreKeys.contains(key)) continue;
			String enValue = enBundle.getString(key);
			String deValue = deBundle.getString(key);
			if (enValue.equals(deValue))
			{
				wrongKeys.add(key + ", en=" + enValue + ", de=" + deValue);
			}
			else
			{
				Set<String> enParms = getParameter(enValue);
				Set<String> deParms = getParameter(deValue);
				if (!enParms.equals(deParms))
				{
					wrongParameters.add(key + " en: " + enParms + ", de:" + deParms);
				}
			}
		}

		if (wrongKeys.size() > 0)
		{
			System.out.println("Keys not translated:");
			for (String key : wrongKeys)
			{
				System.out.println("  " + key);
			}
			fail("Not all translation keys translated!");
		}

		if (wrongParameters.size() > 0)
		{
			System.out.println("Keys with wrong parameter definition:");
			for (String key : wrongParameters)
			{
				System.out.println("  Parameter mismatch for " + key);
			}
			fail ("Not all parameters match between German and English");
		}
	}

	@Test
	public void testQuoting()
	{
		Locale en = new Locale("en");
		Locale de = new Locale("de");

		ResourceBundle enBundle = ResourceMgr.getResourceBundle(en);
		checkQuoting(enBundle);
		ResourceBundle deBundle = ResourceMgr.getResourceBundle(de);
		checkQuoting(deBundle);
	}

	private void checkQuoting(ResourceBundle bundle)
	{
		Pattern p = Pattern.compile("\\s+'\\{[0-9]+\\}'\\s+");
		for (String key : bundle.keySet())
		{
			String value = bundle.getString(key);
			Matcher m = p.matcher(value);
			if (m.find())
			{
				fail("Key=" + key + " for language " + bundle.getLocale() + " uses incorrect single quotes for parameter marker");
			}
		}
	}

	private Set<String> getParameter(String message)
	{
		Set<String> result = new TreeSet<String>();
		// first find all %foobar% parameters
		Pattern oldParms = Pattern.compile("\\%[a-zA-Z]+\\%");
		Matcher m = oldParms.matcher(message);
		while (m.find())
		{
			String name = message.substring(m.start(), m.end());
			result.add(name);
		}
		Pattern newParms = Pattern.compile("\\{[0-9]+\\}");
		m = newParms.matcher(message);
		while (m.find())
		{
			String name = message.substring(m.start(), m.end());
			result.add(name);
		}
		return result;
	}
}
