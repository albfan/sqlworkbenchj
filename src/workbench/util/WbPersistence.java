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
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import workbench.log.LogMgr;

public class WbPersistence
{
	private static final ErrorListener listener = new ErrorListener();

	/** Creates a new instance of Persistence */
	private WbPersistence()
	{
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

	public static void makeTransient(Class clazz, String[] properties)
	{
		try
		{
			BeanInfo info = Introspector.getBeanInfo( clazz );
			PropertyDescriptor propertyDescriptors[] = info.getPropertyDescriptors();
			int count = properties.length;
			Set props = new HashSet(count);
			for (int i=0; i < count; i++) props.add(properties[i]);

			for (int i = 0; i < propertyDescriptors.length; i++)
			{
				PropertyDescriptor pd = propertyDescriptors[i];
				String name = pd.getName();

				if (props.contains(name))
				{
					pd.setValue( "transient", Boolean.TRUE );
				}
			}
		}
		catch ( IntrospectionException e )
		{
		}
	}

	public static Object readObject(String aFilename)
		throws Exception
	{
		//try
		//{
			InputStream in = new BufferedInputStream(new FileInputStream(aFilename));
			return readObject(in, aFilename);
		//}
		//catch (Exception e)
		//{
		//	LogMgr.logDebug("WbPersistence.readObject()", "File " + aFilename + " not found!");
		//	return null;
		//}
	}

	public static Object readObject(InputStream in, String filename)
		throws Exception
	{
		long start, end;
		start = System.currentTimeMillis();
		listener.setFilename(filename);
		XMLDecoder e = new XMLDecoder(in, null, listener);
		Object result = e.readObject();
		e.close();
		end = System.currentTimeMillis();
		//LogMgr.logDebug("WbPersistence.readObject()", "Reading " + filename + " took " + (end - start) + "ms");
		return result;
	}

	public static void writeObject(Object aValue, String aFilename)
	{
		if (aValue == null) return;

		try
		{
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(aFilename));
			listener.setFilename(aFilename);
			XMLEncoder e = new XMLEncoder(out);
			e.writeObject(aValue);
			e.close();
		}
		catch (Throwable e)
		{
			LogMgr.logError("WbPersistence.writeObject()", "Error writing " + aFilename, e);
		}
	}

}

class ErrorListener
	implements ExceptionListener
{
	private String currentFilename;

	public ErrorListener()
	{
	}

	public void setFilename(String file)
	{
		this.currentFilename = file;
	}

	public void exceptionThrown(Exception e)
	{
		LogMgr.logError("WbPersistence", "Error reading file " + currentFilename, e);
	}
}