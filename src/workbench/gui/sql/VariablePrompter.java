/*
 * VariablePrompter.java
 *
 * Created on August 21, 2004, 11:53 AM
 */

package workbench.gui.sql;

import java.awt.Component;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  thomas
 */
public class VariablePrompter
{
	private Component parent;
	private Pattern regex = null;
	private String prefix; 
	private String suffix;
	public VariablePrompter()
	{
		this.prefix = Settings.getInstance().getSqlParameterPrefix();
		this.suffix = Settings.getInstance().getSqlParameterSuffix();
		if (this.suffix == null) this.suffix = StringUtil.EMPTY_STRING;
		String expr = StringUtil.quoteRegexMeta(prefix) + "\\?[\\w]*" + StringUtil.quoteRegexMeta(suffix);
		System.out.println(expr);
		regex = Pattern.compile(expr);
	}
	
	public VariablePrompter(Component aParent)
	{
		this();
		this.parent = aParent;
	}
	
	public boolean hasPrompts(String sql)
	{
		Matcher m = regex.matcher(sql);
		return m.find();
	}
	
	public String checkPrompts(String sql)
	{
		if (!this.hasPrompts(sql)) return sql;
		Matcher m = regex.matcher(sql);
		Set variables = new TreeSet();
		while (m.find())
		{
			int start = m.start() + this.prefix.length() + 1;
			int end = m.end() - this.suffix.length();
			String var = sql.substring(start, end);
			System.out.println("Found varible prompt: " + var);
			variables.add(var);
		}
		return sql;
	}
	
	public static void main(String args[])
	{
		try
		{
			VariablePrompter p = new VariablePrompter();
			p.checkPrompts("select * from a where x = $[?myvar] and y = $[?secondvar]");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.out.println("*** Done.");
	}
	
}
