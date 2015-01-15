/*
 * ExecutionController.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
package workbench.interfaces;

/**
 *
 * @author  Thomas Kellerer
 */
public interface ExecutionController
{

	/**
	 * Confirm the execution of passed SQL command.
	 *
	 * @return true if the user chose to continue
	 */
	boolean confirmStatementExecution(String command);

	/**
	 * Confirm the execution of the statements with a user visible prompt.
	 * This is similar to the "pause" command in a Windows batch file.
	 *
	 * @param prompt the prompt to be displayed to the user
	 * @param yesText the text to display for the "Yes" option, may be null
	 * @param noText the text to display for the "No" option, may be null
	 * @return true if the user chose to continue
	 */
	boolean confirmExecution(String prompt, String yesText, String noText);

	String getPassword(String prompt);
	String getInput(String prompt);
}
