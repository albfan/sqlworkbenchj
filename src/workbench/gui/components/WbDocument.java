/*
 * WbDocument.java
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
package workbench.gui.components;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument.Content;
import javax.swing.text.PlainDocument;

/**
 *
 * @author Thomas Kellerer
 */
public class WbDocument
	extends PlainDocument
	implements DocumentListener
{
	private boolean modified;
	public WbDocument(Content c)
	{
		super(c);
		addDocumentListener(this);
	}

	public WbDocument()
	{
		super();
		addDocumentListener(this);
	}

	public void resetModified()
	{
		modified = false;
	}

	public boolean isModified()
	{
		return modified;
	}

	@Override
	public void insertUpdate(DocumentEvent e)
	{
		modified = true;
	}

	@Override
	public void removeUpdate(DocumentEvent e)
	{
		modified = true;
	}

	@Override
	public void changedUpdate(DocumentEvent e)
	{
		modified = true;
	}

}
