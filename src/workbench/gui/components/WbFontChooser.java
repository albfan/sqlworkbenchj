/*
 * WbFontChooser.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
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
import java.awt.FontMetrics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;

import workbench.gui.WbSwingUtilities;

import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbFontChooser
	extends JPanel
	implements ValidatingComponent
{
	private boolean updateing;

	public WbFontChooser(boolean monospacedOnly)
	{
		super();
		initComponents();
		fillFontList(monospacedOnly);
	}

	@Override
	public boolean validateInput()
	{
		Object value = this.fontSizeComboBox.getSelectedItem();
		if (value == null)
		{
			value = this.fontSizeComboBox.getEditor().getItem();
		}
		if (StringUtil.isNumber(value.toString()))
		{
			return true;
		}
		String msg = ResourceMgr.getFormattedString("ErrInvalidNumber", value);
		WbSwingUtilities.showErrorMessage(msg);
		return false;
	}

	@Override
	public void componentDisplayed()
	{
		// nothing to do
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	public void setSelectedFont(Font aFont)
	{
		this.updateing = true;
		try
		{
			if (aFont != null)
			{
				String name = aFont.getFamily();
				String size = Integer.toString(aFont.getSize());
				int style = aFont.getStyle();

				this.fontNameList.setSelectedValue(name, true);
				this.fontSizeComboBox.setSelectedItem(size);
				this.boldCheckBox.setSelected((style & Font.BOLD) == Font.BOLD);
				this.italicCheckBox.setSelected((style & Font.ITALIC) == Font.ITALIC);
			}
			else
			{
				this.fontNameList.clearSelection();
				this.boldCheckBox.setSelected(false);
				this.italicCheckBox.setSelected(false);
			}
		}
		catch (Exception e)
		{
		}
		this.updateing = false;
		this.updateFontDisplay();
	}

	public Font getSelectedFont()
	{
		String fontName = (String)this.fontNameList.getSelectedValue();
		if (fontName == null) return null;
		int size = StringUtil.getIntValue((String)this.fontSizeComboBox.getSelectedItem());
		int style = Font.PLAIN;
		if (this.italicCheckBox.isSelected())
			style = style | Font.ITALIC;
		if (this.boldCheckBox.isSelected())
			style = style | Font.BOLD;

		Font f = new Font(fontName, style, size);
		return f;
	}

	public static Font chooseFont(JComponent owner, Font defaultFont, boolean monospacedOnly)
	{
		WbFontChooser chooser = new WbFontChooser(monospacedOnly);
		if (defaultFont != null) chooser.setSelectedFont(defaultFont);
		Dimension d = new Dimension(320, 240);
		chooser.setSize(d);
		chooser.setPreferredSize(d);

		Font result = null;
		JDialog parent = null;
		Window win = SwingUtilities.getWindowAncestor(owner);
		if (win instanceof JDialog)
		{
			parent = (JDialog)win;
		}

		boolean OK = ValidatingDialog.showOKCancelDialog(parent, chooser, ResourceMgr.getString("TxtWindowTitleChooseFont"));

		if (OK)
		{
			result = chooser.getSelectedFont();
		}
		return result;
	}

	private void fillFontList(boolean monospacedOnly)
	{
		String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		DefaultListModel model = new DefaultListModel();

		for (String font : fonts)
		{
			if (monospacedOnly)
			{
				Font f = new Font(font, Font.PLAIN, 10);
				FontMetrics fm = getFontMetrics(f);
				int iWidth = fm.charWidth('i');
				int mWidth = fm.charWidth('M');
				if (iWidth != mWidth) continue;
			}
			model.addElement(font);
		}
		this.fontNameList.setModel(model);
	}

	private void updateFontDisplay()
	{
		if (!this.updateing)
		{
			synchronized (this)
			{
				this.updateing = true;
				try
				{
					Font f = this.getSelectedFont();
					if (f != null)
					{
						this.sampleLabel.setFont(f);
						this.sampleLabel.setText((String)this.fontNameList.getSelectedValue());
					}
					else
					{
						this.sampleLabel.setText("");
					}
				}
				finally
				{
					this.updateing = false;
				}
			}
		}
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    GridBagConstraints gridBagConstraints;

    fontSizeComboBox = new JComboBox();
    jScrollPane1 = new JScrollPane();
    fontNameList = new JList();
    boldCheckBox = new JCheckBox();
    italicCheckBox = new JCheckBox();
    sampleLabel = new JLabel();

    setMinimumSize(new Dimension(320, 240));
    setPreferredSize(new Dimension(320, 240));
    setLayout(new GridBagLayout());

    fontSizeComboBox.setEditable(true);
    fontSizeComboBox.setModel(new DefaultComboBoxModel(new String[] { "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "36" }));
    fontSizeComboBox.setSelectedIndex(4);
    fontSizeComboBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent evt)
      {
        fontSizeComboBoxupdateFontDisplay(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 8, 0, 1);
    add(fontSizeComboBox, gridBagConstraints);

    fontNameList.setModel(new AbstractListModel()
    {
      String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
      public int getSize() { return strings.length; }
      public Object getElementAt(int i) { return strings[i]; }
    });
    fontNameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    fontNameList.addListSelectionListener(new ListSelectionListener()
    {
      public void valueChanged(ListSelectionEvent evt)
      {
        fontNameListValueChanged(evt);
      }
    });
    jScrollPane1.setViewportView(fontNameList);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridheight = 4;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(jScrollPane1, gridBagConstraints);

    boldCheckBox.setText(ResourceMgr.getString("LblBold")); // NOI18N
    boldCheckBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent evt)
      {
        boldCheckBoxupdateFontDisplay(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 5, 0, 0);
    add(boldCheckBox, gridBagConstraints);

    italicCheckBox.setText(ResourceMgr.getString("LblItalic")); // NOI18N
    italicCheckBox.addItemListener(new ItemListener()
    {
      public void itemStateChanged(ItemEvent evt)
      {
        italicCheckBoxupdateFontDisplay(evt);
      }
    });
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 5, 0, 0);
    add(italicCheckBox, gridBagConstraints);

    sampleLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Preview"), BorderFactory.createEmptyBorder(1, 1, 5, 1)));
    sampleLabel.setMaximumSize(new Dimension(43, 100));
    sampleLabel.setMinimumSize(new Dimension(48, 60));
    sampleLabel.setPreferredSize(new Dimension(48, 60));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    add(sampleLabel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

	private void fontNameListValueChanged(javax.swing.event.ListSelectionEvent evt)//GEN-FIRST:event_fontNameListValueChanged
	{//GEN-HEADEREND:event_fontNameListValueChanged
		updateFontDisplay();
	}//GEN-LAST:event_fontNameListValueChanged

	private void italicCheckBoxupdateFontDisplay(java.awt.event.ItemEvent evt)//GEN-FIRST:event_italicCheckBoxupdateFontDisplay
	{//GEN-HEADEREND:event_italicCheckBoxupdateFontDisplay
		updateFontDisplay();
	}//GEN-LAST:event_italicCheckBoxupdateFontDisplay

	private void boldCheckBoxupdateFontDisplay(java.awt.event.ItemEvent evt)//GEN-FIRST:event_boldCheckBoxupdateFontDisplay
	{//GEN-HEADEREND:event_boldCheckBoxupdateFontDisplay
		updateFontDisplay();
	}//GEN-LAST:event_boldCheckBoxupdateFontDisplay

	private void fontSizeComboBoxupdateFontDisplay(java.awt.event.ItemEvent evt)//GEN-FIRST:event_fontSizeComboBoxupdateFontDisplay
	{//GEN-HEADEREND:event_fontSizeComboBoxupdateFontDisplay
		updateFontDisplay();
	}//GEN-LAST:event_fontSizeComboBoxupdateFontDisplay

  // Variables declaration - do not modify//GEN-BEGIN:variables
  public JCheckBox boldCheckBox;
  public JList fontNameList;
  public JComboBox fontSizeComboBox;
  public JCheckBox italicCheckBox;
  public JScrollPane jScrollPane1;
  public JLabel sampleLabel;
  // End of variables declaration//GEN-END:variables

}
