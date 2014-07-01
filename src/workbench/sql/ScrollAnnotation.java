/*
 * ScrollAnnotation.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.sql;

import java.util.Set;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class ScrollAnnotation
	extends AnnotationReader
{
	public static final String ANNOTATION = "WbScrollTo";
	public static final String END_KEYWORD = "end";

	private final Set<String> endKeywords = CollectionUtil.caseInsensitiveSet("bottom", "last", END_KEYWORD);

	public ScrollAnnotation()
	{
		super(ANNOTATION);
	}

	public static String getScrollToEndAnnotation()
	{
		return "-- @" + ANNOTATION + " " + END_KEYWORD;
	}

	public boolean scrollToEnd(String sql)
	{
		String value = getAnnotationValue(sql);
		return endKeywords.contains(value);
	}

	public int scrollToLine(String sql)
	{
		String value = getAnnotationValue(sql);
		if (StringUtil.isNonBlank(value))
		{
			if (value.startsWith("#"))
			{
				return StringUtil.getIntValue(value.substring(1), -1);
			}
		}
		return -1;
	}
}
