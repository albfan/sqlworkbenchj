package workbench.util;

import java.util.StringTokenizer;

public class SqlUtil
{
	
	/** Creates a new instance of SqlUtil */
	private SqlUtil()
	{
	}

	public static String getSqlVerb(String aStatement)
	{
		StringTokenizer tok = new StringTokenizer(aStatement.trim());
		return tok.nextToken(" \t");
	}
}
