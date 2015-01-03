/*
 * SummaryLabel.java
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
package workbench.gui.dbobjects;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import workbench.resource.ResourceMgr;

import workbench.gui.components.DataStoreTableModel;

/**
 *
 * @author Thomas Kellerer
 */
public class SummaryLabel
	extends JLabel
{
	private static final Border DEFAULT_BORDER = new CompoundBorder(new EmptyBorder(2, 0, 0, 0), new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(1, 1, 1, 0)));

	public SummaryLabel(String text)
	{
		super(text);
		Font f = getFont();
		FontMetrics fm = null;
		if (f != null) fm = getFontMetrics(f);
		int height = fm == null ? 0 : fm.getHeight() + 8;
		int width = fm == null ? 0 : fm.charWidth('W');
		height = Math.min(22, height);
		width = Math.max(80, width * 10);
		Dimension d = new Dimension(width, height);
		setMinimumSize(d);
		setPreferredSize(d);
		setBorder(DEFAULT_BORDER);
	}

	public void showObjectListInfo(DataStoreTableModel model)
	{
		int filteredCount = model.getFilteredCount();
		int rowCount = model.getRowCount();
		int totalCount = filteredCount + rowCount;
		String info;
		if (filteredCount == 0)
		{
			info = ResourceMgr.getFormattedString("TxtTableListObjects", rowCount);
		}
		else
		{
			info = ResourceMgr.getFormattedString("TxtTableListFiltered", rowCount, totalCount);
		}
		this.setText(info);
	}

}
