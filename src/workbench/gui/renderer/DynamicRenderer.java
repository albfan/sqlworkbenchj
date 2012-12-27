/*
 * DynamicRenderer.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2013, Thomas Kellerer
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
package workbench.gui.renderer;

import java.lang.reflect.Method;
import workbench.log.LogMgr;

/**
 * A renderer that uses reflection to obtain the real data value
 * from the object retrieved from the database.
 * <br/>
 * Currently not used.
 *
 * @author Thomas Kellerer
 */
public class DynamicRenderer
	extends ToolTipRenderer
	implements WbRenderer
{
	private Method getter;
	private Class valueClass;

	public DynamicRenderer(Class clz, String getterName)
		throws ClassNotFoundException, NoSuchMethodException
	{
		super();
		this.valueClass = clz;
		this.getter = valueClass.getMethod(getterName, (Class[]) null);
	}

	@SuppressWarnings(value = "unchecked")
	@Override
	public void prepareDisplay(Object aValue)
	{
		// ToolTipRenderer will never pass null
		this.displayValue = null;
		if (valueClass.isAssignableFrom(aValue.getClass()))
		{
			try
			{
				Object value = getter.invoke(aValue);
				if (value instanceof byte[])
				{
					this.displayValue = new String((byte[]) value);
				}
				else if (value != null)
				{
					this.displayValue = value.toString();
				}
			}
			catch (Exception e)
			{
				String msg = e.getMessage();
				Throwable cause = e.getCause();
				while (msg == null && cause != null)
				{
					msg = cause.getMessage();
					if (msg == null && cause != null) cause = cause.getCause();
				}
				LogMgr.logError("DynamicRenderer.prepareDisplay()", "Could not obtain real object value: " + e.getMessage(), null);
				this.displayValue = null;
			}
		}
		else
		{
			displayValue = aValue.toString();
		}
		setTooltip(displayValue);
	}
}
