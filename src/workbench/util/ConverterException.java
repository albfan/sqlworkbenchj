/*
 * ConverterException.java
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
package workbench.util;

import workbench.resource.ResourceMgr;

/**
 * @author Thomas Kellerer
 */
public class ConverterException
	extends Exception
{

	private String value;
	private int dataType;
	public ConverterException(String msg)
	{
		super(msg);
	}

	public ConverterException(Object input, int type,  Exception cause)
	{
		super("Could not convert [" + input + "] for datatype " + SqlUtil.getTypeName(type), cause);
		this.value = (input == null ? "" : input.toString());
		this.dataType = type;

	}

	@Override
	public String getLocalizedMessage()
	{
		String msg = null;
		if (this.getCause() == null)
		{
			msg = getMessage();
		}
		else
		{
			msg = getCause().getMessage();
		}
		if (msg == null)
		{
			msg = ResourceMgr.getFormattedString("MsgConvertErrorDetails", value, SqlUtil.getTypeName(dataType));
		}
		return msg;
	}
}
