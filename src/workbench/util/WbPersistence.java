package workbench.util;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class WbPersistence
{
	
	/** Creates a new instance of Persistence */
	private WbPersistence()
	{
	}
	
	public static void makeTransient( Class clazz, String property )
	{
		try
		{
			BeanInfo info = Introspector.getBeanInfo( clazz );
			PropertyDescriptor propertyDescriptors[] = info.getPropertyDescriptors();
			
			for ( int i = 0; i < propertyDescriptors.length; i++ )
			{
				PropertyDescriptor pd = propertyDescriptors[i];
				//System.out.println( pd.getName() );
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
	
	public static Object readObject(String aFilename)
	{
		long start,end;
		try
		{
			start = System.currentTimeMillis();
			XMLDecoder e = new XMLDecoder(new BufferedInputStream(new FileInputStream(aFilename)));
			Object result = e.readObject();
			e.close();
			end = System.currentTimeMillis();
			System.out.println("XMLDecode for " + aFilename + " " + (end - start));
			return result;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public static void writeObject(Object aValue, String aFilename)
	{
		if (aValue == null) return;
		long start,end;
		
		try
		{
			start = System.currentTimeMillis();
			XMLEncoder e = new XMLEncoder(new BufferedOutputStream(new FileOutputStream(aFilename)));
			e.writeObject(aValue);
			e.close();
			end = System.currentTimeMillis();
			System.out.println("XMLEncode for " + aFilename + " " + (end - start));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
}
