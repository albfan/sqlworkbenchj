/*
 * NotifierEvent.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
package workbench.util;

import java.awt.event.ActionListener;

/**
 * @author Thomas Kellerer
 */
public class NotifierEvent
{
	private String iconKey;
	private String message;
	private ActionListener handler;
	private String type;
	private String tooltip;

	public NotifierEvent(String key, String message, ActionListener l)
	{
		this.iconKey = key;
		this.message = message;
		this.handler = l;
	}

	public void setTooltip(String tip)
	{
		tooltip = tip;
	}

	public String getTooltip()
	{
		return tooltip;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public String getType()
	{
		return type;
	}

	public String getIconKey()
	{
		return iconKey;
	}

	public String getMessage()
	{
		return message;
	}

	public ActionListener getHandler()
	{
		return handler;
	}


}
