/*
 * Created on December 14, 2002, 2:38 PM
 */
package workbench.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ArgumentParser
{

	private Map arguments = new HashMap();
	
	public ArgumentParser()
	{
	}
	
	public void addArgument(String key)
	{
		if (key == null) throw new NullPointerException("Key may not be null");
		this.arguments.put(key.toLowerCase(), null);
	}
	
	public void parse(String args[])
	{
		StringBuffer line = new StringBuffer(200);
		for (int i=0; i<args.length; i++)
		{
			line.append(args[i]);
			line.append(' ');
		}
		this.parse(line.toString());
	}
	
	public void parse(String aCmdLine)
	{
		List words = StringUtil.split(aCmdLine, "-", false, "\"", true);

		int count = words.size();
		for (int i=0; i < count; i++)
		{
			String word = (String)words.get(i);
			String arg = null;
			String value = null;
			int pos = word.indexOf('=');
			if (pos > -1)
			{
				arg = word.substring(0, pos).trim();
				value = word.substring(pos + 1).trim();
			}
			else
			{
				// ignore parameters without a value
				continue;
			}
			arg = arg.toLowerCase();
			if (arguments.containsKey(arg))
			{
				arguments.put(arg, value);
			}
		}
	}
	
	public String getValue(String key)
	{
		return (String)this.arguments.get(key.toLowerCase());
	}
	
	public static void main(String[] args)
	{
		//String test = "spool /type=sql /file=\"d:/temp/test.sql\" /table=my_table;";
		String test = "/profile=\"HSQLDB - Test Server\" /script=\"d:/temp/test.sql\"";
		ArgumentParser parser = new ArgumentParser();
		parser.addArgument("script");
		parser.addArgument("profile");
		parser.parse(test);
		System.out.println("script=" + parser.getValue("script"));
		System.out.println("profile=" + parser.getValue("profile"));
		System.out.println("done.");
	} 

}
