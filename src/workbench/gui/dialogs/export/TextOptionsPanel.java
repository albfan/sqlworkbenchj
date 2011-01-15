/*
 * TextOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code may be reused without the permission of the author
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
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import workbench.gui.components.WbComboBox;
import workbench.gui.dialogs.QuoteEscapeSelector;
import workbench.gui.dialogs.QuoteSettingVerifier;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.CharacterRange;
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
	private int preferredWidth;
	private QuoteSettingVerifier verifier;

	public TextOptionsPanel()
	{
		super();
		initComponents();
		CharacterRange[] ranges = CharacterRange.getRanges();

		Font f = escapeRange.getFont();

		int width = 0;
		int maxwidth = 0;

		for (int i=0; i < ranges.length; i++)
		{
			escapeRange.addItem(ranges[i]);
			if (f != null)
			{
				FontMetrics fm = escapeRange.getFontMetrics(f);
				if (fm != null)
				{
					int w = fm.stringWidth(ranges[i].toString());
					if (i == 0)
					{
						width = w;
					}
					if (w > maxwidth) maxwidth = w;
				}
			}
		}

		if (width == 0) width = 50;

		Dimension pref = escapeRange.getPreferredSize();
		preferredWidth = (int)pref.getWidth();

		int add = preferredWidth - maxwidth;
		width += add;

		Dimension max = new Dimension(width, (int)pref.getHeight());
		escapeRange.setMaximumSize(max);
		escapeRange.setPreferredSize(max);
		((WbComboBox)escapeRange).setPopupWidth(preferredWidth);
		verifier = new QuoteSettingVerifier(quoteEscape, quoteAlways);
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
	}

	public void restoreSettings()
	{
		Settings s = Settings.getInstance();
		//this.setCleanupCarriageReturns(s.getBoolProperty("workbench.export.text.cleanup"));
		this.setExportHeaders(s.getBoolProperty("workbench.export.text.includeheader"));
		this.setQuoteAlways(s.getBoolProperty("workbench.export.text.quotealways"));
		int id = s.getIntProperty("workbench.export.text.escaperange",0);
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
		quoteEscape.setEscapeType(escape);
	}

	@Override
	public QuoteEscapeType getQuoteEscaping()
	{
		return quoteEscape.getEscapeType();
	}

	public void setDecimalSymbol(String symbol)
	{
		this.decimalChar.setText(symbol);
	}

	public String getDecimalSymbol()
	{
		return decimalChar.getText();
	}

	public boolean getExportHeaders()
	{
		return this.exportHeaders.isSelected();
	}

	public String getTextDelimiter()
	{
		return this.delimiter.getText();
	}

	public String getTextQuoteChar()
	{
		return this.quoteChar.getText();
	}

	public void setExportHeaders(boolean flag)
	{
		this.exportHeaders.setSelected(flag);
	}

	public void setTextDelimiter(String delim)
	{
		this.delimiter.setText(delim);
	}

	public void setTextQuoteChar(String quote)
	{
		this.quoteChar.setText(quote);
	}

	public boolean getQuoteAlways()
	{
		if (!quoteAlways.isEnabled()) return false;
		return this.quoteAlways.isSelected();
	}

	public void setQuoteAlways(boolean flag)
	{
		this.quoteAlways.setSelected(flag);
	}

	public void setEscapeRange(CharacterRange range)
	{
		this.escapeRange.setSelectedItem(range);
	}

	public CharacterRange getEscapeRange()
	{
		return (CharacterRange)this.escapeRange.getSelectedItem();
	}

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

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
		GridBagConstraints gridBagConstraints;

    delimiterLabel = new JLabel();
    delimiter = new JTextField();
    exportHeaders = new JCheckBox();
    quoteCharLabel = new JLabel();
    quoteChar = new JTextField();
    jPanel1 = new JPanel();
    quoteAlways = new JCheckBox();
    escapeRange = new WbComboBox();
    escapeLabel = new JLabel();
    lineEndingLabel = new JLabel();
    lineEnding = new JComboBox();
    decimalLabel = new JLabel();
    decimalChar = new JTextField();
    quoteEscape = new QuoteEscapeSelector();

    setLayout(new GridBagLayout());

    delimiterLabel.setText(ResourceMgr.getString("LblFieldDelimiter")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(delimiterLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(delimiter, gridBagConstraints);

    exportHeaders.setText(ResourceMgr.getString("LblExportIncludeHeaders")); // NOI18N
    exportHeaders.setToolTipText("");
    exportHeaders.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 4, 0, 0);
    add(exportHeaders, gridBagConstraints);

    quoteCharLabel.setText(ResourceMgr.getString("LblQuoteChar")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 4, 0, 4);
    add(quoteCharLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(quoteChar, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    add(jPanel1, gridBagConstraints);

    quoteAlways.setText(ResourceMgr.getString("LblExportQuoteAlways")); // NOI18N
    quoteAlways.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(8, 4, 0, 0);
    add(quoteAlways, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(4, 4, 0, 4);
    add(escapeRange, gridBagConstraints);

    escapeLabel.setText(ResourceMgr.getString("LblExportEscapeType")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(4, 4, 0, 4);
    add(escapeLabel, gridBagConstraints);

    lineEndingLabel.setText(ResourceMgr.getString("LblExportLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(lineEndingLabel, gridBagConstraints);

    lineEnding.setModel(new DefaultComboBoxModel(new String[] { "LF", "CRLF" }));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(lineEnding, gridBagConstraints);

    decimalLabel.setText(ResourceMgr.getString("LblDecimalSymbol")); // NOI18N
    decimalLabel.setToolTipText(ResourceMgr.getDescription("LblDecimalSymbol"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 4, 0, 4);
    add(decimalLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(2, 4, 0, 4);
    add(decimalChar, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(7, 4, 0, 4);
    add(quoteEscape, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JTextField decimalChar;
  private JLabel decimalLabel;
  private JTextField delimiter;
  private JLabel delimiterLabel;
  private JLabel escapeLabel;
  private JComboBox escapeRange;
  private JCheckBox exportHeaders;
  private JPanel jPanel1;
  private JComboBox lineEnding;
  private JLabel lineEndingLabel;
  private JCheckBox quoteAlways;
  private JTextField quoteChar;
  private JLabel quoteCharLabel;
  private QuoteEscapeSelector quoteEscape;
  // End of variables declaration//GEN-END:variables


}
