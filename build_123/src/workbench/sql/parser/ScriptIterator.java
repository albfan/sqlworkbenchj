/*
 * ScriptIterator.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench.sql.parser;

import java.io.File;
import java.io.IOException;

import workbench.sql.DelimiterDefinition;
import workbench.sql.ScriptCommandDefinition;

/**
 *
 * @author Thomas Kellerer
 */
public interface ScriptIterator
{
	int getScriptLength();

	void setCheckEscapedQuotes(boolean flag);

	void setEmptyLineIsDelimiter(boolean flag);

	/**
	 * Controls if the actual SQL for each command returned by
	 * #getNextCommand() is stored in the ScriptCommandDefinition
	 * or if only start and end in the script should be stored.
	 *
	 * @param flag if true, the actual SQL is returned otherwise only the start and end
	 */
	void setStoreStatementText(boolean flag);

	boolean supportsSingleLineCommands();

	/**
	 * Return the next command from the script.
	 * There are no more commands if this returns null
	 */
	ScriptCommandDefinition getNextCommand();

	void setDelimiter(DelimiterDefinition delim);
	void setAlternateDelimiter(DelimiterDefinition delim);

	boolean supportsMixedDelimiter();

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
