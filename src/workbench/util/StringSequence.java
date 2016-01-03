/*
 * StringSequence.java
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

import workbench.interfaces.CharacterSequence;

/**
 * An implementation of the CharacterSequence interface
 * based on a String as the source.
 *
 * @see FileMappedSequence
 * @author Thomas Kellerer
 */
public class StringSequence
	implements CharacterSequence
{
	private CharSequence source;

	/**
	 * Create a StringSequence based on the given String
	 */
	public StringSequence(CharSequence s)
	{
		this.source = s;
	}

	@Override
	public void done()
	{
		this.source = null;
	}

	@Override
	public int length()
	{
		return source.length();
	}

	@Override
	public char charAt(int index)
	{
		return this.source.charAt(index);
	}

	@Override
	public CharSequence subSequence(int start, int end)
	{
		return this.source.subSequence(start, end);
	}

}
