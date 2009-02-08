/*
 * EditorOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import workbench.gui.components.DelimiterDefinitionPanel;
import workbench.gui.components.NumberField;
import workbench.gui.components.WbColorPicker;
import workbench.gui.components.WbFontPicker;
import workbench.interfaces.Restoreable;
import workbench.resource.ColumnSortType;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class EditorOptionsPanel
	extends JPanel
	implements Restoreable
{

	/** Creates new form EditorOptionsPanel */
	public EditorOptionsPanel()
	{
		super();
		initComponents();
		editorFont.setListMonospacedOnly(true);
	}

	public void restoreSettings()
	{
		editorFont.setSelectedFont(Settings.getInstance().getEditorFont());

		String[] items = new String[] {
			ResourceMgr.getString("LblLTDefault"),
			ResourceMgr.getString("LblLTDos"),
			ResourceMgr.getString("LblLTUnix")
		};

		this.internalLineEnding.setModel(new DefaultComboBoxModel(items));
		this.externalLineEnding.setModel(new DefaultComboBoxModel(items));

		String[] pasteCase = new String[] {
			ResourceMgr.getString("LblLowercase"),
			ResourceMgr.getString("LblUppercase"),
			ResourceMgr.getString("LblAsIs")
		};
		this.completionPasteCase.setModel(new DefaultComboBoxModel(pasteCase));

		String value = Settings.getInstance().getInteralLineEndingValue();
		internalLineEnding.setSelectedIndex(lineEndingValueToIndex(value));

		value = Settings.getInstance().getExternalLineEndingValue();
		externalLineEnding.setSelectedIndex(lineEndingValueToIndex(value));

		String paste = Settings.getInstance().getAutoCompletionPasteCase();
		if ("lower".equals(paste)) this.completionPasteCase.setSelectedIndex(0);
		else if ("upper".equals(paste)) this.completionPasteCase.setSelectedIndex(1);
		else this.completionPasteCase.setSelectedIndex(2);

		alternateDelim.setDelimiter(Settings.getInstance().getAlternateDelimiter());

		String[] sortItems = new String[] {
			ResourceMgr.getString("LblSortPastColName"),
			ResourceMgr.getString("LblSortPastColPos")
		};
		this.completionColumnSort.setModel(new DefaultComboBoxModel(sortItems));

		ColumnSortType sort = Settings.getInstance().getAutoCompletionColumnSortType();
		if (sort == ColumnSortType.position)
		{
			this.completionColumnSort.setSelectedIndex(1);
		}
		else
		{
			this.completionColumnSort.setSelectedIndex(0);
		}
		noWordSep.setText(Settings.getInstance().getEditorNoWordSep());
	}

	private String indexToLineEndingValue(int index)
	{
		if (index == 1) return Settings.DOS_LINE_TERMINATOR_PROP_VALUE;
		if (index == 2) return Settings.UNIX_LINE_TERMINATOR_PROP_VALUE;
		return Settings.DEFAULT_LINE_TERMINATOR_PROP_VALUE;
	}

	private int lineEndingValueToIndex(String value)
	{
		if (Settings.DOS_LINE_TERMINATOR_PROP_VALUE.equals(value))
		{
			return 1;
		}
		else if (Settings.UNIX_LINE_TERMINATOR_PROP_VALUE.equals(value))
		{
			return 2;
		}
		return 0;
	}

	public void saveSettings()
	{
		Settings set = Settings.getInstance();
		set.setMaxHistorySize(((NumberField)this.historySizeField).getValue());
		int index = this.completionPasteCase.getSelectedIndex();
		if (index == 0)
		{
			set.setAutoCompletionPasteCase("lower");
		}
		else if (index == 1)
		{
			set.setAutoCompletionPasteCase("upper");
		}
		else
		{
			set.setAutoCompletionPasteCase(null);
		}

		set.setCloseAutoCompletionWithSearch(closePopup.isSelected());
		set.setEditorFont(editorFont.getSelectedFont());
		set.setAlternateDelimiter(alternateDelim.getDelimiter());
		set.setRightClickMovesCursor(rightClickMovesCursor.isSelected());
		set.setAutoJumpNextStatement(this.autoAdvance.isSelected());
		set.setEditorTabWidth(StringUtil.getIntValue(this.tabSize.getText(), 2));
		set.setElectricScroll(StringUtil.getIntValue(electricScroll.getText(),-1));
		set.setEditorNoWordSep(noWordSep.getText());
		String value = indexToLineEndingValue(internalLineEnding.getSelectedIndex());
		set.setInternalEditorLineEnding(value);
		value = indexToLineEndingValue(externalLineEnding.getSelectedIndex());
		set.setExternalEditorLineEnding(value);
		index = completionColumnSort.getSelectedIndex();
		if (index == 1)
		{
			set.setAutoCompletionColumnSort(ColumnSortType.position);
		}
		else
		{
			set.setAutoCompletionColumnSort(ColumnSortType.name);
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

    autoAdvance = new JCheckBox();
    editorTabSizeLabel = new JLabel();
    tabSize = new JTextField();
    altDelimLabel = new JLabel();
    historySizeLabel = new JLabel();
    historySizeField = new NumberField();
    electricScrollLabel = new JLabel();
    electricScroll = new JTextField();
    rightClickMovesCursor = new JCheckBox();
    editorFontLabel = new JLabel();
    editorFont = new WbFontPicker();
    closePopup = new JCheckBox();
    completionPasteCase = new JComboBox();
    pasteLabel = new JLabel();
    internalLineEndingLabel = new JLabel();
    internalLineEnding = new JComboBox();
    externalLineEndingLabel = new JLabel();
    externalLineEnding = new JComboBox();
    includeFilesInHistory = new JCheckBox();
    alternateDelim = new DelimiterDefinitionPanel();
    pasterOrderLabel = new JLabel();
    completionColumnSort = new JComboBox();
    noWordSepLabel = new JLabel();
    noWordSep = new JTextField();

    setLayout(new GridBagLayout());

    autoAdvance.setSelected(Settings.getInstance().getAutoJumpNextStatement());
    autoAdvance.setText(ResourceMgr.getString("LblAutoAdvance")); // NOI18N
    autoAdvance.setToolTipText(ResourceMgr.getString("d_LblAutoAdvance")); // NOI18N
    autoAdvance.setHorizontalAlignment(SwingConstants.LEFT);
    autoAdvance.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(8, 9, 0, 11);
    add(autoAdvance, gridBagConstraints);

    editorTabSizeLabel.setText(ResourceMgr.getString("LblTabWidth")); // NOI18N
    editorTabSizeLabel.setToolTipText(ResourceMgr.getString("d_LblTabWidth")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(editorTabSizeLabel, gridBagConstraints);

    tabSize.setHorizontalAlignment(JTextField.LEFT);
    tabSize.setText(Settings.getInstance().getProperty("workbench.editor.tabwidth", "2"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(tabSize, gridBagConstraints);

    altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit")); // NOI18N
    altDelimLabel.setToolTipText(ResourceMgr.getString("d_LblAltDelimit")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(8, 12, 0, 0);
    add(altDelimLabel, gridBagConstraints);

    historySizeLabel.setText(ResourceMgr.getString("LblHistorySize")); // NOI18N
    historySizeLabel.setToolTipText(ResourceMgr.getString("d_LblHistorySize")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(historySizeLabel, gridBagConstraints);

    historySizeField.setText(Integer.toString(Settings.getInstance().getMaxHistorySize()));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(historySizeField, gridBagConstraints);

    electricScrollLabel.setText(ResourceMgr.getString("LblSettingElectricScroll")); // NOI18N
    electricScrollLabel.setToolTipText(ResourceMgr.getString("d_LblSettingElectricScroll")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(electricScrollLabel, gridBagConstraints);

    electricScroll.setText(Integer.toString(Settings.getInstance().getElectricScroll()));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(electricScroll, gridBagConstraints);

    rightClickMovesCursor.setSelected(Settings.getInstance().getRightClickMovesCursor());
    rightClickMovesCursor.setText(ResourceMgr.getString("LblRightClickMove")); // NOI18N
    rightClickMovesCursor.setToolTipText(ResourceMgr.getString("d_LblRightClickMove")); // NOI18N
    rightClickMovesCursor.setBorder(null);
    rightClickMovesCursor.setMaximumSize(new Dimension(93, 15));
    rightClickMovesCursor.setMinimumSize(new Dimension(93, 15));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(8, 0, 0, 10);
    add(rightClickMovesCursor, gridBagConstraints);

    editorFontLabel.setText(ResourceMgr.getString("LblEditorFont")); // NOI18N
    editorFontLabel.setToolTipText(ResourceMgr.getString("d_LblEditorFont")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(editorFontLabel, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(3, 10, 0, 15);
    add(editorFont, gridBagConstraints);

    closePopup.setSelected(Settings.getInstance().getCloseAutoCompletionWithSearch());
    closePopup.setText(ResourceMgr.getString("TxtCloseCompletion"));
    closePopup.setToolTipText(ResourceMgr.getDescription("TxtCloseCompletion"));
    closePopup.setBorder(null);
    closePopup.setHorizontalAlignment(SwingConstants.LEFT);
    closePopup.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(6, 0, 0, 11);
    add(closePopup, gridBagConstraints);

    completionPasteCase.setModel(new DefaultComboBoxModel(new String[] { "Lowercase", "Uppercase", "As is" }));
    completionPasteCase.setToolTipText(ResourceMgr.getDescription("LblPasteCase"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(completionPasteCase, gridBagConstraints);

    pasteLabel.setLabelFor(completionPasteCase);
    pasteLabel.setText(ResourceMgr.getString("LblPasteCase")); // NOI18N
    pasteLabel.setToolTipText(ResourceMgr.getString("d_LblPasteCase")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(8, 12, 0, 0);
    add(pasteLabel, gridBagConstraints);

    internalLineEndingLabel.setText(ResourceMgr.getString("LblIntLineEnding")); // NOI18N
    internalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblIntLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(internalLineEndingLabel, gridBagConstraints);

    internalLineEnding.setToolTipText(ResourceMgr.getDescription("LblIntLineEnding"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(internalLineEnding, gridBagConstraints);

    externalLineEndingLabel.setText(ResourceMgr.getString("LblExtLineEnding")); // NOI18N
    externalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblExtLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(externalLineEndingLabel, gridBagConstraints);

    externalLineEnding.setToolTipText(ResourceMgr.getDescription("LblExtLineEnding"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(externalLineEnding, gridBagConstraints);

    includeFilesInHistory.setSelected(Settings.getInstance().getStoreFilesInHistory());
    includeFilesInHistory.setText(ResourceMgr.getString("TxtHistoryIncFiles")); // NOI18N
    includeFilesInHistory.setToolTipText(ResourceMgr.getString("d_TxtHistoryIncFiles")); // NOI18N
    includeFilesInHistory.setBorder(null);
    includeFilesInHistory.setHorizontalAlignment(SwingConstants.LEFT);
    includeFilesInHistory.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(5, 0, 0, 11);
    add(includeFilesInHistory, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 10, 0, 5);
    add(alternateDelim, gridBagConstraints);

    pasterOrderLabel.setLabelFor(completionColumnSort);
    pasterOrderLabel.setText(ResourceMgr.getString("LblPasteSort")); // NOI18N
    pasterOrderLabel.setToolTipText(ResourceMgr.getString("d_LblPasteSort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 12, 0, 0);
    add(pasterOrderLabel, gridBagConstraints);

    completionColumnSort.setToolTipText(ResourceMgr.getDescription("LblPasteSort"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 11, 0, 15);
    add(completionColumnSort, gridBagConstraints);

    noWordSepLabel.setText(ResourceMgr.getString("LblNoWordSep")); // NOI18N
    noWordSepLabel.setToolTipText(ResourceMgr.getString("d_LblNoWordSep")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(noWordSepLabel, gridBagConstraints);

    noWordSep.setHorizontalAlignment(JTextField.LEFT);
    noWordSep.setName("nowordsep"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(noWordSep, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JLabel altDelimLabel;
  private DelimiterDefinitionPanel alternateDelim;
  private JCheckBox autoAdvance;
  private JCheckBox closePopup;
  private JComboBox completionColumnSort;
  private JComboBox completionPasteCase;
  private WbFontPicker editorFont;
  private JLabel editorFontLabel;
  private JLabel editorTabSizeLabel;
  private JTextField electricScroll;
  private JLabel electricScrollLabel;
  private JComboBox externalLineEnding;
  private JLabel externalLineEndingLabel;
  private JTextField historySizeField;
  private JLabel historySizeLabel;
  private JCheckBox includeFilesInHistory;
  private JComboBox internalLineEnding;
  private JLabel internalLineEndingLabel;
  private JTextField noWordSep;
  private JLabel noWordSepLabel;
  private JLabel pasteLabel;
  private JLabel pasterOrderLabel;
  private JCheckBox rightClickMovesCursor;
  private JTextField tabSize;
  // End of variables declaration//GEN-END:variables

}
