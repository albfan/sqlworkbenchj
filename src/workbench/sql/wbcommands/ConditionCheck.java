/*
 * ConditionCheck.java
 *
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
package workbench.sql.wbcommands;

import java.util.List;

import workbench.resource.ResourceMgr;

import workbench.sql.StatementRunnerResult;
import workbench.sql.VariablePool;

import workbench.util.ArgumentParser;
import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ConditionCheck
{
	private static final Result OK = new Result();

	public static final String PARAM_IF_DEF = "ifDefined";
	public static final String PARAM_IF_NOTDEF = "ifNotDefined";
	public static final String PARAM_IF_EQUALS = "ifEquals";
	public static final String PARAM_IF_NOTEQ = "ifNotEquals";
	public static final String PARAM_IF_EMPTY = "ifEmpty";
	public static final String PARAM_IF_NOTEMPTY = "ifNotEmpty";

	private static final List<String> arguments = CollectionUtil.arrayList(
		PARAM_IF_DEF, PARAM_IF_NOTDEF, PARAM_IF_EQUALS, PARAM_IF_NOTEQ, PARAM_IF_EMPTY, PARAM_IF_NOTEMPTY);

	public static void addParameters(ArgumentParser cmdLine)
	{
		for (String arg : arguments)
		{
			cmdLine.addArgument(arg);
		}
	}

	public static boolean conditionSpecified(ArgumentParser cmdLine)
	{
		for (String arg : arguments)
		{
			if (cmdLine.isArgPresent(arg))
			{
				return true;
			}
		}
		return false;
	}

	public static boolean isCommandLineOK(StatementRunnerResult result, ArgumentParser cmdLine)
	{
		int count = 0;
		for (String arg : arguments)
		{
			if (cmdLine.isArgPresent(arg))
			{
				count ++;
			}
		}

		if (count <= 1) return true;

		// more than one argument specified, this is not allowed
		result.addErrorMessageByKey("ErrCondTooMany", StringUtil.listToString(arguments, ','));
		return false;
	}

	/**
	 * Check if the condition specified on the commandline is met.
	 *
	 * @param cmdLine the parameter to check
	 * @return null if the condition is met,
	 *         the parameter where the check failed otherwise
	 */
	public static Result checkConditions(ArgumentParser cmdLine)
	{
		if (cmdLine.isArgPresent(PARAM_IF_DEF))
		{
			String var = cmdLine.getValue(PARAM_IF_DEF);
			if (!VariablePool.getInstance().isDefined(var))
			{
				return new Result(PARAM_IF_DEF, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_NOTDEF))
		{
			String var = cmdLine.getValue(PARAM_IF_NOTDEF);
			if (VariablePool.getInstance().isDefined(var))
			{
				return new Result(PARAM_IF_NOTDEF, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_EMPTY))
		{
			String var = cmdLine.getValue(PARAM_IF_EMPTY);
			String value = VariablePool.getInstance().getParameterValue(var);
			if (StringUtil.isNonEmpty(value))
			{
				return new Result(PARAM_IF_EMPTY, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_NOTEMPTY))
		{
			String var = cmdLine.getValue(PARAM_IF_NOTEMPTY);
			String value = VariablePool.getInstance().getParameterValue(var);
			if (StringUtil.isEmptyString(value))
			{
				return new Result(PARAM_IF_NOTEMPTY, var);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_EQUALS))
		{
			String var = cmdLine.getValue(PARAM_IF_EQUALS);
			String[] elements = var.split("=");
			if (elements.length == 2)
			{
				String value = VariablePool.getInstance().getParameterValue(elements[0]);
				if (value != null && value.equals(elements[1]))
				{
					return OK;
				}
				return new Result(PARAM_IF_EQUALS, elements[0], elements[1]);
			}
		}

		if (cmdLine.isArgPresent(PARAM_IF_NOTEQ))
		{
			String var = cmdLine.getValue(PARAM_IF_NOTEQ);
			String[] elements = var.split("=");
			if (elements.length == 2)
			{
				String value = VariablePool.getInstance().getParameterValue(elements[0]);
				if (value == null || !value.equals(elements[1]))
				{
					return OK;
				}
				return new Result(PARAM_IF_NOTEQ, elements[0], elements[1]);
			}
		}
		return OK;
	}

	public static String getMessage(String msgPrefix, Result check)
	{
		String action = ResourceMgr.getString(msgPrefix + "Action");
		return ResourceMgr.getFormattedString("Err_" + check.getFailedCondition(), action, check.getVariable(), check.getExpectedValue());
	}

	public static class Result
	{
		private boolean conditionIsOK;
		private String failedParameter;
		private String variableName;
		private String expectedValue;

		public Result()
		{
			conditionIsOK = true;
		}

		public Result(String param, String varName)
		{
			this.conditionIsOK = false;
			this.failedParameter = param;
			this.variableName = varName;
		}

		public Result(String param, String varName, String value)
		{
			this.conditionIsOK = false;
			this.failedParameter = param;
			this.variableName = varName;
			this.expectedValue = value;
		}

		public boolean isOK()
		{
			return conditionIsOK;
		}

		public String getExpectedValue()
		{
			return this.expectedValue;
		}

		public String getFailedCondition()
		{
			return failedParameter;
		}

		public String getVariable()
		{
			return variableName;
		}
	}
}

