/*
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
package workbench.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;
import workbench.gui.renderer.ColorUtils;


/**
 *
 * @author Thomas Kellerer
 */
public class FoldingPanel
	extends JPanel
	implements MouseListener
{
	private final JPanel content;
	private ToggleIndicator toggle;
	private boolean showing;
	private String tooltipFolded;
	private String tooltipExpanded;

	public FoldingPanel(JPanel content)
	{
		super(new BorderLayout(0, 2));
		this.content = content;

		toggle = new ToggleIndicator();
		Color bg = toggle.getBackground().darker();
		bg = ColorUtils.blend(toggle.getBackground(), bg, 150);
		toggle.setBackground(bg);

		toggle.setBorder(new EmptyBorder(0,2,0,2));
		Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
		this.toggle.setCursor(cursor);

		this.toggle.addMouseListener(this);

		this.tooltipExpanded = ResourceMgr.getString("TxtExtOptHide");
		this.tooltipFolded = ResourceMgr.getString("TxtExtOptShow");
		this.toggle.setToolTipText(this.tooltipFolded);

		this.add(toggle, BorderLayout.PAGE_START);
	}

	public void setTooltips(String collapsed, String expanded)
	{
		tooltipFolded = collapsed;
		tooltipExpanded = expanded;
	}

	public void showContent()
	{
		this.add(content, BorderLayout.CENTER);
		showing = true;
		toggle.setDirectionUp();
		toggle.setToolTipText(tooltipExpanded);
		updateDisplay(content.getPreferredSize().height);
	}

	public void hideContent()
	{
		this.remove(content);
		showing = false;
		toggle.setDirectionDown();
		toggle.setToolTipText(tooltipFolded);
		updateDisplay(-1 * content.getPreferredSize().height);
	}

	private void updateDisplay(final int delta)
	{
		this.invalidate();
		WbSwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				Window w = SwingUtilities.getWindowAncestor(FoldingPanel.this);
				if (w != null)
				{
					w.validate();
					Dimension d = w.getSize();
					d.height += delta;
					w.setSize(d);
				}
			}
		});
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if (showing)
		{
			hideContent();
		}
		else
		{
			showContent();
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
	}

}
