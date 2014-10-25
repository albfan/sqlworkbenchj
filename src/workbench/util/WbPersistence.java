/*
 * WbPersistence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.util;

import java.beans.BeanInfo;
import java.beans.ExceptionListener;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import workbench.log.LogMgr;

/**
 * @author Thomas Kellerer
 */
public class WbPersistence
	implements ExceptionListener
{
	private String filename;

	public WbPersistence()
	{
	}

	public WbPersistence(String file)
	{
		filename = file;
	}

	/**
	 * Makes a property of the given class transient, so that it won't be written
	 * into the XML file when saved using WbPersistence
	 * @param clazz
	 * @param property
	 */
	public static void makeTransient(Class clazz, String property)
	{
		try
		{
			BeanInfo info = Introspector.getBeanInfo( clazz );
			PropertyDescriptor[] propertyDescriptors = info.getPropertyDescriptors();
			for (PropertyDescriptor pd : propertyDescriptors)
			{
				if ( pd.getName().equals(property) )
				{
					pd.setValue( "transient", Boolean.TRUE );
				}
			}
		}
		catch ( IntrospectionException e )
		{
		}
	}

	public Object readObject()
		throws Exception
	{
		if (this.filename == null) throw new IllegalArgumentException("No filename specified!");
		InputStream in = new BufferedInputStream(new FileInputStream(filename));
		return readObject(in);
	}

	public Object readObject(InputStream in)
		throws Exception
	{
		try (XMLDecoder e = new XMLDecoder(in, null, this))
		{
			Object result = e.readObject();
			return result;
		}
		finally
		{
			FileUtil.closeQuietely(in);
		}
	}

	public void writeObject(Object aValue)
		throws IOException
	{
		if (aValue == null) return;

		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(filename), 32*1024);
				XMLEncoder e = new XMLEncoder(out))
		{
			e.writeObject(aValue);
		}
	}

	@Override
	public void exceptionThrown(Exception e)
	{
		LogMgr.logError("WbPersistence", "Error reading file " + filename, e);
	}

}
