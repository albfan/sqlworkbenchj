/*
 * ConverterException.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
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
