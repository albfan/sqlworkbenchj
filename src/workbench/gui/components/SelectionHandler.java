/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015 Thomas Kellerer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.gui.components;

import java.awt.event.KeyEvent;

/**
 *
 * @author Thomas Kellerer
 */
public class SelectionHandler
{
	private WbTable dataList;

	public SelectionHandler(WbTable list)
	{
		this.dataList = list;
	}

	public void handleKeyPressed(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case KeyEvent.VK_UP:
			case KeyEvent.VK_PAGE_UP:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_PAGE_DOWN:
        dataList.dispatchEvent(e);
				break;
		}
	}

	public void selectRow(int row)
	{
		if (row < 0) row = 0;
		if (row >= dataList.getRowCount()) row = dataList.getRowCount() - 1;
		dataList.getSelectionModel().setSelectionInterval(row, row);
		dataList.scrollToRow(row);
	}

}
