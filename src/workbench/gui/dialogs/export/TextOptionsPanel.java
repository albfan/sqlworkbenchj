/*
 * TextOptionsPanel.java
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
package workbench.gui.dialogs.export;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.exporter.BlobMode;
import workbench.db.exporter.ControlFileFormat;
import workbench.db.exporter.WrongFormatFileException;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.FoldingPanel;
import workbench.gui.components.WbComboBox;
import workbench.gui.dialogs.QuoteEscapeSelector;
import workbench.gui.dialogs.QuoteSettingVerifier;
import workbench.util.CharacterEscapeType;

import workbench.util.CharacterRange;
import workbench.util.CollectionUtil;
import workbench.util.QuoteEscapeType;
import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class TextOptionsPanel
	extends JPanel
	implements TextOptions
{
	public TextOptionsPanel()
	{
		super();
		initComponents();
		populateEscapeRange((WbComboBox)escapeRange);

		// The constructor will setup the necessary actions
		new QuoteSettingVerifier((QuoteEscapeSelector)escapeSelect, quoteAlways);

		StringBuilder formatList = new StringBuilder(20);
		boolean first = true;
		for (ControlFileFormat format : ControlFileFormat.values())
		{
			if (format == ControlFileFormat.none) continue;
			if (first)
			{
				first = false;
			}
			else
			{
				formatList.append(',');
			}
			formatList.append(format.toString());
		}
		String tip = ResourceMgr.getFormattedString("d_LblFmtFiles", formatList.toString());
		controlFiles.setToolTipText(tip);
		ctrlFileLabel.setToolTipText(tip);

		List<String> bTypes = CollectionUtil.arrayList(
			BlobMode.AnsiLiteral.getTypeString(),
			BlobMode.SaveToFile.getTypeString(),
			BlobMode.Base64.getTypeString(),
			BlobMode.DbmsLiteral.getTypeString());

		ComboBoxModel blobModel = new DefaultComboBoxModel(bTypes.toArray());
		blobTypes.setModel(blobModel);
		blobTypes.setSelectedItem(BlobMode.SaveToFile.toString());

		GridBagLayout layout = (GridBagLayout)getLayout();
		GridBagConstraints c = layout.getConstraints(extOptionsPanel);
		remove(extOptionsPanel);
		FoldingPanel p = new FoldingPanel(extOptionsPanel);
		add(p, c);
		invalidate();
	}

	public static void populateEscapeRange(WbComboBox combo)
	{
		CharacterRange[] ranges = CharacterRange.getRanges();

		Font f = combo.getFont();

		int width = 0;
		int maxwidth = 0;

		for (int i = 0; i < ranges.length; i++)
		{
			combo.addItem(ranges[i]);
			if (f != null)
			{
				FontMetrics fm = combo.getFontMetrics(f);
				if (fm != null)
				{
					int w = fm.stringWidth(ranges[i].toString());
					if (i == 0)
					{
						width = w;
					}
					if (w > maxwidth)
					{
						maxwidth = w;
					}
				}
			}
		}

		if (width == 0)
		{
			width = 50;
		}

		Dimension pref = combo.getPreferredSize();
		int prefWidth = (int) pref.getWidth();

		int add = prefWidth - maxwidth;
		width += add;

		Dimension max = new Dimension(width, (int) pref.getHeight());
		combo.setMaximumSize(max);
		combo.setPreferredSize(max);
		combo.setPopupWidth(prefWidth);
	}

	public void saveSettings()
	{
		Settings s = Settings.getInstance();
		//s.setProperty("workbench.export.text.cleanup", this.getCleanupCarriageReturns());
		s.setProperty("workbench.export.text.includeheader", this.getExportHeaders());
		s.setProperty("workbench.export.text.quotealways", this.getQuoteAlways());
		s.setProperty("workbench.export.text.escaperange", this.getEscapeRange().getId());
		s.setProperty("workbench.export.text.lineending", (String)this.lineEnding.getSelectedItem());
		s.setProperty("workbench.export.text.decimal", getDecimalSymbol());
		s.setDefaultTextDelimiter(this.getTextDelimiter());
		s.setQuoteChar(this.getTextQuoteChar());
		s.setProperty("workbench.export.text.quote.escape", getQuoteEscaping().toString());
		s.setProperty("workbench.export.text.formatfiles", controlFiles.getText());
		s.setProperty("workbench.export.text.blobmode", this.getBlobMode().getTypeString());

	}

	public void restoreSettings()
	{
		Settings s = Settings.getInstance();
		//this.setCleanupCarriageReturns(s.getBoolProperty("workbench.export.text.cleanup"));
		this.setExportHeaders(s.getBoolProperty("workbench.export.text.includeheader"));
		this.setQuoteAlways(s.getBoolProperty("workbench.export.text.quotealways"));
		int id = s.getIntProperty("workbench.export.text.escaperange", CharacterRange.RANGE_NONE.getId());
		CharacterRange range = CharacterRange.getRangeById(id);
		this.setEscapeRange(range);
		this.setLineEnding(s.getProperty("workbench.export.text.lineending", "LF"));
		this.setTextQuoteChar(s.getQuoteChar());
		this.setTextDelimiter(s.getDefaultTextDelimiter(true));
		setDecimalSymbol(s.getProperty("workbench.export.text.decimal", s.getDecimalSymbol()));
		String quote = s.getProperty("workbench.export.text.quote.escape", "none");
		QuoteEscapeType escape = null;
		try
		{
			escape = QuoteEscapeType.valueOf(quote);
		}
		catch (Exception e)
		{
			escape = QuoteEscapeType.none;
		}
		((QuoteEscapeSelector)escapeSelect).setEscapeType(escape);
		controlFiles.setText(s.getProperty("workbench.export.text.formatfiles", ""));
		String type = s.getProperty("workbench.export.text.blobmode", BlobMode.SaveToFile.getTypeString());
		this.blobTypes.setSelectedItem(type);
	}

	@Override
	public BlobMode getBlobMode()
	{
		String type = (String)blobTypes.getSelectedItem();
		BlobMode mode = BlobMode.getMode(type);
		return mode;
	}

	@Override
	public QuoteEscapeType getQuoteEscaping()
	{
		return ((QuoteEscapeSelector)escapeSelect).getEscapeType();
	}

	@Override
	public void setDecimalSymbol(String symbol)
	{
		this.decimalChar.setText(symbol);
	}

	@Override
	public String getDecimalSymbol()
	{
		return decimalChar.getText();
	}

	@Override
	public boolean getExportHeaders()
	{
		return this.exportHeaders.isSelected();
	}

	@Override
	public String getTextDelimiter()
	{
		return this.delimiter.getText();
	}

	@Override
	public String getTextQuoteChar()
	{
		return this.quoteChar.getText();
	}

	@Override
	public void setExportHeaders(boolean flag)
	{
		this.exportHeaders.setSelected(flag);
	}

	@Override
	public void setTextDelimiter(String delim)
	{
		this.delimiter.setText(delim);
	}

	@Override
	public void setTextQuoteChar(String quote)
	{
		this.quoteChar.setText(quote);
	}

	@Override
	public boolean getQuoteAlways()
	{
		if (!quoteAlways.isEnabled()) return false;
		return this.quoteAlways.isSelected();
	}

	@Override
	public void setQuoteAlways(boolean flag)
	{
		this.quoteAlways.setSelected(flag);
	}

	@Override
	public void setEscapeType(CharacterEscapeType type)
	{
	}

	@Override
	public CharacterEscapeType getEscapeType()
	{
		return CharacterEscapeType.unicode;
	}

	@Override
	public void setEscapeRange(CharacterRange range)
	{
		this.escapeRange.setSelectedItem(range);
	}

	@Override
	public CharacterRange getEscapeRange()
	{
		return (CharacterRange)this.escapeRange.getSelectedItem();
	}

	@Override
	public String getLineEnding()
	{
		String s = (String)lineEnding.getSelectedItem();
		if ("LF".equals(s))
		{
			return "\n";
		}
		else if ("CRLF".equals(s))
		{
			return "\r\n";
		}
		else
		{
			return StringUtil.LINE_TERMINATOR;
		}
	}

	@Override
	public void setLineEnding(String ending)
	{
		if (ending == null) return;
		if ("\n".equals(ending))
		{
			lineEnding.setSelectedItem("LF");
		}
		else if ("\r\n".equals(ending))
		{
			lineEnding.setSelectedItem("CRLF");
		}
		else
		{
			lineEnding.setSelectedItem(ending.toUpperCase());
		}
	}

	@Override
	public Set<ControlFileFormat> getControlFiles()
	{
		String values = controlFiles.getText();
		try
		{
			Set<ControlFileFormat> formats = ControlFileFormat.parseCommandLine(values);
			return formats;
		}
		catch (WrongFormatFileException wf)
		{
			String error = ResourceMgr.getFormattedString("ErrExpWrongCtl", wf.getFormat());
			WbSwingUtilities.showErrorMessage(error);
		}
		return Collections.emptySet();
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

    delimiterLabel = new JLabel();
    delimiter = new JTextField();
    exportHeaders = new JCheckBox();
    quoteCharLabel = new JLabel();
    quoteChar = new JTextField();
    quoteAlways = new JCheckBox();
    lineEndingLabel = new JLabel();
    lineEnding = new JComboBox();
    decimalLabel = new JLabel();
    decimalChar = new JTextField();
    escapeSelect = new QuoteEscapeSelector();
    jLabel1 = new JLabel();
    extOptionsPanel = new JPanel();
    ctrlFileLabel = new JLabel();
    controlFiles = new JTextField();
    blobTypes = new JComboBox();
    blobTypesLabel = new JLabel();
    escapeLabel = new JLabel();
    escapeRange = new WbComboBox();

    setMinimumSize(new Dimension(200, 50));
    setLayout(new GridBagLayout());

    delimiterLabel.setText(ResourceMgr.getString("LblFieldDelimiter")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(delimiterLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(delimiter, gridBagConstraints);

    exportHeaders.setText(ResourceMgr.getString("LblExportIncludeHeaders")); // NOI18N
    exportHeaders.setToolTipText("");
    exportHeaders.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.insets = new Insets(5, 4, 1, 0);
    add(exportHeaders, gridBagConstraints);

    quoteCharLabel.setText(ResourceMgr.getString("LblQuoteChar")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 4, 0, 4);
    add(quoteCharLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(1, 4, 0, 4);
    add(quoteChar, gridBagConstraints);

    quoteAlways.setText(ResourceMgr.getString("LblExportQuoteAlways")); // NOI18N
    quoteAlways.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 4, 2, 0);
    add(quoteAlways, gridBagConstraints);

    lineEndingLabel.setText(ResourceMgr.getString("LblExportLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(1, 4, 0, 4);
    add(lineEndingLabel, gridBagConstraints);

    lineEnding.setModel(new DefaultComboBoxModel(new String[] { "LF", "CRLF" }));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(1, 4, 0, 4);
    add(lineEnding, gridBagConstraints);

    decimalLabel.setText(ResourceMgr.getString("LblDecimalSymbol")); // NOI18N
    decimalLabel.setToolTipText(ResourceMgr.getDescription("LblDecimalSymbol"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 4, 0, 4);
    add(decimalLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(decimalChar, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(escapeSelect, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblQuoteEsc")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 4, 0, 4);
    add(jLabel1, gridBagConstraints);

    extOptionsPanel.setLayout(new GridBagLayout());

    ctrlFileLabel.setText(ResourceMgr.getString("LblFmtFiles")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(1, 4, 0, 4);
    extOptionsPanel.add(ctrlFileLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 4, 0, 4);
    extOptionsPanel.add(controlFiles, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    extOptionsPanel.add(blobTypes, gridBagConstraints);

    blobTypesLabel.setText(ResourceMgr.getString("LblBlobType")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    extOptionsPanel.add(blobTypesLabel, gridBagConstraints);

    escapeLabel.setText(ResourceMgr.getString("LblExportEscapeType")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    extOptionsPanel.add(escapeLabel, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    extOptionsPanel.add(escapeRange, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    add(extOptionsPanel, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JComboBox blobTypes;
  private JLabel blobTypesLabel;
  private JTextField controlFiles;
  private JLabel ctrlFileLabel;
  private JTextField decimalChar;
  private JLabel decimalLabel;
  private JTextField delimiter;
  private JLabel delimiterLabel;
  private JLabel escapeLabel;
  private JComboBox escapeRange;
  private JComboBox escapeSelect;
  private JCheckBox exportHeaders;
  private JPanel extOptionsPanel;
  private JLabel jLabel1;
  private JComboBox lineEnding;
  private JLabel lineEndingLabel;
  private JCheckBox quoteAlways;
  private JTextField quoteChar;
  private JLabel quoteCharLabel;
  // End of variables declaration//GEN-END:variables


}
