/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.parser;

import workbench.sql.DelimiterDefinition;
import workbench.sql.lexer.SQLToken;

/**
 * A DelimiterTester is a Visitor that is called by the ScriptParser while a script is processed.
 *
 * The DelimiterTester can dynamically change the current statement delimiter according to the
 * context during the parsing phase.
 *
 * This is e.g. used to detect when Oracle's alternate delimiter needs to be used.
 *
 * @author Thomas Kellerer
 */
public interface DelimiterTester
{
	void setAlternateDelimiter(DelimiterDefinition delimiter);
	void setDelimiter(DelimiterDefinition delimiter);

	void currentToken(SQLToken token, boolean isStartOfStatement);

	DelimiterDefinition getCurrentDelimiter();

	void statementFinished();

	boolean supportsMixedDelimiters();
	boolean supportsSingleLineStatements();
	boolean isSingleLineStatement(SQLToken token, boolean isStartOfLine);
	void lineEnd();

}
