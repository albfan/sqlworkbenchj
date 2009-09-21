/*
 * NewInterface
 * 
 *  This file is part of SQL Workbench/J, http://www.sql-workbench.net
 * 
 *  Copyright 2002-2009, Thomas Kellerer
 *  No part of this code maybe reused without the permission of the author
 * 
 *  To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author Thomas Kellerer
 */
public interface ScriptIterator
{
	int getScriptLength();
	
	boolean hasMoreCommands();
	
	void setCheckForSingleLineCommands(boolean flag);
	
	void setAlternateLineComment(String comment);
	
	void setCheckEscapedQuotes(boolean flag);
	
	void setSupportOracleInclude(boolean flag);
	
	void setEmptyLineIsDelimiter(boolean flag);

	/**
	 * Return the next command from the script.
	 * There are no more commands if this returns null
	 */
	ScriptCommandDefinition getNextCommand();

	void setDelimiter(DelimiterDefinition delim);

	/**
	 * Define the source file to be used and the encoding of the file.
	 * If the encoding is null, the default encoding will be used.
	 * @see #setFile(File, String)
	 * @see workbench.resource.Settings#getDefaultEncoding()
	 */
	void setFile(File f, String enc)
		throws IOException;

	void setReturnStartingWhitespace(boolean flag);

	/**
	 * Define the script to be parsed
	 */
	void setScript(String aScript);

	void reset();

	void done();
}
