/*
 * Created on December 14, 2002, 2:38 PM
 */
package workbench.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import workbench.log.LogMgr;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ArgumentParser
{

	private Map arguments = new HashMap();
	private ArrayList unknownParameters = new ArrayList();
	private int argCount = 0;

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
		this.reset();
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
		this.reset();
		List words = StringUtil.split(aCmdLine, "-", false, "\"'", false);

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
				this.argCount ++;
			}
			else
			{
				this.unknownParameters.add(arg);
			}
		}
	}

	public boolean hasArguments()
	{
		return this.argCount > 0;
	}
	public int getArgumentCount()
	{
		return this.argCount;
	}

	public boolean hasUnknownArguments()
	{
		return this.unknownParameters.size() > 0;
	}

	public List getUnknownArguments()
	{
		return Collections.unmodifiableList(this.unknownParameters);
	}
	public void reset()
	{
		Iterator keys = this.arguments.keySet().iterator();
		while (keys.hasNext())
		{
			String key = (String)keys.next();
			this.arguments.put(key, null);
		}
		this.argCount = 0;
		this.unknownParameters.clear();
	}
	public boolean getBoolean(String key)
	{
		String value = this.getValue(key);
		return "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value);
	}

	public String getValue(String key)
	{
		String value = (String)this.arguments.get(key.toLowerCase());
		value = StringUtil.trimQuotes(value);
		return value;
	}

	public static void main(String[] args)
	{
		//String test = "spool /type=sql /file=\"d:/temp/test.sql\" /table=my_table;";
		//String test = "/profile=\"HSQLDB - Test Server\" /script=\"d:/temp/test.sql\"";
		//String test = "-quotechar='\"' -file=\"d:/temp/export test.txt\" -delimiter=\" \" -dateformat=dd.MMM.yyyy";
		String test = "-driverjar=\"mysql-jdbc.jar\"";
		ArgumentParser parser = new ArgumentParser();
		parser.addArgument("driverjar");
		parser.parse(test);
		System.out.println("driverjar=>" + parser.getValue("driverjar") + "<");
		System.out.println("done.");
	}

}