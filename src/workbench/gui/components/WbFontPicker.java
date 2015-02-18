/*
 * WbFontPicker.java
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
package workbench.gui.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.Serializable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;

/**
 *
 * @author Thomas Kellerer
 */
public class WbFontPicker
	extends JPanel
	implements Serializable
{
	private JLabel fontName;
	private JButton resetButton;
	private JButton selectFontButton;
	private Font selectedFont;
	private boolean monospacedOnly;

	public WbFontPicker()
	{
		super();
		initComponents();
		this.setAllowFontReset(false);
	}

	public void setAllowFontReset(boolean flag)
	{
		this.resetButton.setVisible(flag);
		this.resetButton.setEnabled(flag);
		if (flag)
		{
			this.resetButton.setIcon(IconMgr.getInstance().getLabelIcon("Delete"));
			WbSwingUtilities.makeEqualSize(resetButton, selectFontButton);
		}
	}

	public void setListMonospacedOnly(boolean flag)
	{
		this.monospacedOnly = flag;
	}

	private void initComponents()
	{
		GridBagConstraints gc;

		fontName = new JLabel();
		selectFontButton = new FlatButton();
		resetButton = new FlatButton();

		setLayout(new GridBagLayout());

		fontName.setText("Sample Font");
		fontName.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(1, 5, 1, 5)));
		gc = new GridBagConstraints();
		gc.gridx = 0;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.WEST;
		gc.weightx = 1.0;
		gc.weighty = 1.0;
		gc.insets = new Insets(0, 0, 0, 5);
		add(fontName, gc);

		resetButton.setToolTipText(ResourceMgr.getDescription("LblResetFont"));
		resetButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				resetButtonActionPerformed(evt);
			}
		});

		gc = new GridBagConstraints();
		gc.gridx = 1;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;
		gc.insets = new Insets(0, 0, 0, 3);
		add(resetButton, gc);

		selectFontButton.setText("...");
		selectFontButton.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(java.awt.event.MouseEvent evt)
			{
				selectFontButtonMouseClicked(evt);
			}

		});
		gc = new GridBagConstraints();
		gc.gridx = 2;
		gc.gridy = 0;
		gc.fill = GridBagConstraints.BOTH;
		gc.anchor = GridBagConstraints.WEST;
		add(selectFontButton, gc);
	}

	private void resetButtonActionPerformed(java.awt.event.ActionEvent evt)
	{
		this.setSelectedFont(null);
	}

	private void selectFontButtonMouseClicked(java.awt.event.MouseEvent evt)
	{
		WbFontChooser chooser = new WbFontChooser(monospacedOnly);
		chooser.setSelectedFont(getSelectedFont());

		Dimension d = new Dimension(320, 240);
		chooser.setSize(d);
		chooser.setPreferredSize(d);

		Font result = null;
		JDialog parent = null;
		Window win = SwingUtilities.getWindowAncestor(this);
		if (win instanceof JDialog)
		{
			parent = (JDialog) win;
		}

		boolean ok = ValidatingDialog.showOKCancelDialog(parent, chooser, ResourceMgr.getString("TxtWindowTitleChooseFont"));

		if (ok)
		{
			result = chooser.getSelectedFont();
			this.setSelectedFont(result);
		}
	}

	public Font getSelectedFont()
	{
		return this.selectedFont;
	}

	public void setSelectedFont(Font f)
	{
		this.selectedFont = f;
		if (f == null)
		{
			Font df = UIManager.getDefaults().getFont("Label.font");
			this.fontName.setFont(df);
			this.fontName.setText(ResourceMgr.getString("LblDefaultIndicator"));
		}
		else
		{
			this.fontName.setFont(f);
			this.fontName.setText(f.getFontName() + ", " + f.getSize());
		}
	}

}
