/*
 * WbPersistence.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
			PropertyDescriptor propertyDescriptors[] = info.getPropertyDescriptors();
			for (int i = 0; i < propertyDescriptors.length; i++)
			{
				PropertyDescriptor pd = propertyDescriptors[i];
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
		InputStream in = new BufferedInputStream(new FileInputStream(filename), 32*1024);
		return readObject(in);
	}

	public Object readObject(InputStream in)
		throws Exception
	{
		try
		{
			XMLDecoder e = new XMLDecoder(in, null, this);
			Object result = e.readObject();
			e.close();
			return result;
		}
		finally
		{
			try { in.close(); } catch (Throwable th) {}
		}
	}

	public void writeObject(Object aValue)
		throws IOException
	{
		if (aValue == null) return;

		BufferedOutputStream out = null;
		try
		{
			out = new BufferedOutputStream(new FileOutputStream(filename), 32*1024);
			XMLEncoder e = new XMLEncoder(out);
			e.writeObject(aValue);
			e.close();
		}
//		catch (Throwable e)
//		{
//			LogMgr.logError("WbPersistence.writeObject()", "Error writing " + filename, e);
//			return false;
//		}
		finally
		{
			try { out.close(); } catch (Throwable th) {}
		}
//		return true;
	}

	public void exceptionThrown(Exception e)
	{
		LogMgr.logError("WbPersistence", "Error reading file " + filename, e);
	}
	
}
