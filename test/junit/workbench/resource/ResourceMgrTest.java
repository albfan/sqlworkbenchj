/*
 * ResourceMgrTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import workbench.WbTestCase;

import workbench.util.CollectionUtil;
import workbench.util.FileUtil;
import workbench.util.StringUtil;

import org.junit.Test;

import static org.junit.Assert.*;

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

		Set<String> wrongKeys = CollectionUtil.caseInsensitiveSet();
		InputStream in = getClass().getResourceAsStream("ignore_keys.txt");
		List<String> keys = FileUtil.getLines(new BufferedReader(new InputStreamReader(in)), true);

		Set<String> ignoredKeys = CollectionUtil.caseInsensitiveSet();
		ignoredKeys.addAll(keys);

		Set<String> allKeys = CollectionUtil.caseInsensitiveSet();
		allKeys.addAll(enBundle.keySet());
		allKeys.addAll(deBundle.keySet());

		Set<String> wrongParameters = CollectionUtil.caseInsensitiveSet();
		for (String key : allKeys)
		{
			if (ignoredKeys.contains(key)) continue;

			String enValue = getString(enBundle, key);
			String deValue = getString(deBundle, key);

			if (StringUtil.isBlank(enValue) || StringUtil.isBlank(deValue) || enValue.equals(deValue))
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

	private String getString(ResourceBundle bundle, String key)
	{
		try
		{
			return bundle.getString(key);
		}
		catch (Exception ex)
		{
			return null;
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
		Set<String> result = CollectionUtil.caseInsensitiveSet();

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
