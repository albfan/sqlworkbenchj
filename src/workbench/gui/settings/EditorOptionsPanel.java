/*
 * EditorOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;
import workbench.gui.components.NumberField;
import workbench.gui.components.WbCheckBoxLabel;
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
	implements workbench.interfaces.Restoreable
{

	/** Creates new form EditorOptionsPanel */
	public EditorOptionsPanel()
	{
		initComponents();
		editorFont.setListMonospacedOnly(true);
	}

	public void restoreSettings()
	{
		errorColor.setSelectedColor(Settings.getInstance().getEditorErrorColor());
		selectionColor.setSelectedColor(Settings.getInstance().getEditorSelectionColor());
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
		set.setEditorErrorColor(errorColor.getSelectedColor());
		set.setEditorFont(editorFont.getSelectedFont());
		set.setAlternateDelimiter(alternateDelim.getDelimiter());
		set.setRightClickMovesCursor(rightClickMovesCursor.isSelected());
		set.setEditorSelectionColor(selectionColor.getSelectedColor());
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
    java.awt.GridBagConstraints gridBagConstraints;

    autoAdvanceLabel = new WbCheckBoxLabel();
    autoAdvance = new javax.swing.JCheckBox();
    editorTabSizeLabel = new javax.swing.JLabel();
    tabSize = new javax.swing.JTextField();
    altDelimLabel = new javax.swing.JLabel();
    historySizeLabel = new javax.swing.JLabel();
    historySizeField = new NumberField();
    electricScrollLabel = new javax.swing.JLabel();
    electricScroll = new javax.swing.JTextField();
    rightClickLabel = new WbCheckBoxLabel();
    rightClickMovesCursor = new javax.swing.JCheckBox();
    editorFontLabel = new javax.swing.JLabel();
    editorFont = new workbench.gui.components.WbFontPicker();
    labelCloseSearch = new WbCheckBoxLabel();
    closePopup = new javax.swing.JCheckBox();
    completionPasteCase = new javax.swing.JComboBox();
    pasteLabel = new javax.swing.JLabel();
    internalLineEndingLabel = new javax.swing.JLabel();
    internalLineEnding = new javax.swing.JComboBox();
    externalLineEndingLabel = new javax.swing.JLabel();
    externalLineEnding = new javax.swing.JComboBox();
    filesInHistoryLabel = new WbCheckBoxLabel();
    includeFilesInHistory = new javax.swing.JCheckBox();
    alternateDelim = new workbench.gui.components.DelimiterDefinitionPanel();
    pasterOrderLabel = new javax.swing.JLabel();
    completionColumnSort = new javax.swing.JComboBox();
    noWordSepLabel = new javax.swing.JLabel();
    noWordSep = new javax.swing.JTextField();
    jPanel1 = new javax.swing.JPanel();
    errorColorLabel = new javax.swing.JLabel();
    errorColor = new workbench.gui.components.WbColorPicker();
    selectionColorLabel = new javax.swing.JLabel();
    selectionColor = new workbench.gui.components.WbColorPicker();

    setLayout(new java.awt.GridBagLayout());

    autoAdvanceLabel.setLabelFor(autoAdvance);
    autoAdvanceLabel.setText(ResourceMgr.getString("LblAutoAdvance"));
    autoAdvanceLabel.setToolTipText(ResourceMgr.getDescription("LblAutoAdvance"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 12, 2, 0);
    add(autoAdvanceLabel, gridBagConstraints);

    autoAdvance.setSelected(Settings.getInstance().getAutoJumpNextStatement());
    autoAdvance.setText("");
    autoAdvance.setBorder(null);
    autoAdvance.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    autoAdvance.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    autoAdvance.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 10, 2, 11);
    add(autoAdvance, gridBagConstraints);

    editorTabSizeLabel.setText(ResourceMgr.getString("LblTabWidth"));
    editorTabSizeLabel.setToolTipText(ResourceMgr.getDescription("LblTabWidth"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
    add(editorTabSizeLabel, gridBagConstraints);

    tabSize.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    tabSize.setText(Settings.getInstance().getProperty("workbench.editor.tabwidth", "2"));
    tabSize.setMinimumSize(new java.awt.Dimension(72, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 15;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 11, 0, 15);
    add(tabSize, gridBagConstraints);

    altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit"));
    altDelimLabel.setToolTipText(ResourceMgr.getDescription("LblAltDelimit"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 0, 0);
    add(altDelimLabel, gridBagConstraints);

    historySizeLabel.setText(ResourceMgr.getString("LblHistorySize"));
    historySizeLabel.setToolTipText(ResourceMgr.getDescription("LblHistorySize"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
    add(historySizeLabel, gridBagConstraints);

    historySizeField.setText(Integer.toString(Settings.getInstance().getMaxHistorySize()));
    historySizeField.setMinimumSize(new java.awt.Dimension(72, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 11, 0, 15);
    add(historySizeField, gridBagConstraints);

    electricScrollLabel.setText(ResourceMgr.getString("LblSettingElectricScroll"));
    electricScrollLabel.setToolTipText(ResourceMgr.getDescription("LblSettingElectricScroll"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 12, 0, 0);
    add(electricScrollLabel, gridBagConstraints);

    electricScroll.setText(Integer.toString(Settings.getInstance().getElectricScroll()));
    electricScroll.setMinimumSize(new java.awt.Dimension(72, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 11, 0, 15);
    add(electricScroll, gridBagConstraints);

    rightClickLabel.setLabelFor(rightClickMovesCursor);
    rightClickLabel.setText(ResourceMgr.getString("LblRightClickMove"));
    rightClickLabel.setToolTipText(ResourceMgr.getDescription("LblRightClickMove"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 12, 0, 0);
    add(rightClickLabel, gridBagConstraints);

    rightClickMovesCursor.setSelected(Settings.getInstance().getRightClickMovesCursor());
    rightClickMovesCursor.setText("");
    rightClickMovesCursor.setBorder(null);
    rightClickMovesCursor.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    rightClickMovesCursor.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    rightClickMovesCursor.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(3, 10, 0, 11);
    add(rightClickMovesCursor, gridBagConstraints);

    editorFontLabel.setText(ResourceMgr.getString("LblEditorFont"));
    editorFontLabel.setToolTipText(ResourceMgr.getDescription("LblEditorFont"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 12, 0, 0);
    add(editorFontLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 11, 0, 15);
    add(editorFont, gridBagConstraints);

    labelCloseSearch.setLabelFor(closePopup);
    labelCloseSearch.setText(ResourceMgr.getString("TxtCloseCompletion"));
    labelCloseSearch.setToolTipText(ResourceMgr.getDescription("TxtCloseCompletion"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 12, 0, 0);
    add(labelCloseSearch, gridBagConstraints);

    closePopup.setSelected(Settings.getInstance().getCloseAutoCompletionWithSearch());
    closePopup.setText("");
    closePopup.setBorder(null);
    closePopup.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    closePopup.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 10, 0, 11);
    add(closePopup, gridBagConstraints);

    completionPasteCase.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Lowercase", "Uppercase", "As is" }));
    completionPasteCase.setToolTipText(ResourceMgr.getDescription("LblPasteCase"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 11, 0, 15);
    add(completionPasteCase, gridBagConstraints);

    pasteLabel.setLabelFor(completionPasteCase);
    pasteLabel.setText(ResourceMgr.getString("LblPasteCase"));
    pasteLabel.setToolTipText(ResourceMgr.getDescription("LblPasteCase"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(9, 12, 0, 0);
    add(pasteLabel, gridBagConstraints);

    internalLineEndingLabel.setText(ResourceMgr.getString("LblIntLineEnding"));
    internalLineEndingLabel.setToolTipText(ResourceMgr.getDescription("LblIntLineEnding"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 12, 0, 0);
    add(internalLineEndingLabel, gridBagConstraints);

    internalLineEnding.setToolTipText(ResourceMgr.getDescription("LblIntLineEnding"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 11, 0, 15);
    add(internalLineEnding, gridBagConstraints);

    externalLineEndingLabel.setText(ResourceMgr.getString("LblExtLineEnding"));
    externalLineEndingLabel.setToolTipText(ResourceMgr.getDescription("LblExtLineEnding"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 12, 0, 0);
    add(externalLineEndingLabel, gridBagConstraints);

    externalLineEnding.setToolTipText(ResourceMgr.getDescription("LblExtLineEnding"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 11, 0, 15);
    add(externalLineEnding, gridBagConstraints);

    filesInHistoryLabel.setLabelFor(includeFilesInHistory);
    filesInHistoryLabel.setText(ResourceMgr.getString("TxtHistoryIncFiles"));
    filesInHistoryLabel.setToolTipText(ResourceMgr.getDescription("TxtHistoryIncFiles"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
    add(filesInHistoryLabel, gridBagConstraints);

    includeFilesInHistory.setSelected(Settings.getInstance().getStoreFilesInHistory());
    includeFilesInHistory.setText("");
    includeFilesInHistory.setBorder(null);
    includeFilesInHistory.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    includeFilesInHistory.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 10, 0, 11);
    add(includeFilesInHistory, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(2, 10, 0, 5);
    add(alternateDelim, gridBagConstraints);

    pasterOrderLabel.setLabelFor(completionColumnSort);
    pasterOrderLabel.setText(ResourceMgr.getString("LblPasteSort"));
    pasterOrderLabel.setToolTipText(ResourceMgr.getDescription("LblPasteSort"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 12, 0, 0);
    add(pasterOrderLabel, gridBagConstraints);

    completionColumnSort.setToolTipText(ResourceMgr.getDescription("LblPasteSort"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(4, 11, 0, 15);
    add(completionColumnSort, gridBagConstraints);

    noWordSepLabel.setText(ResourceMgr.getString("LblNoWordSep"));
    noWordSepLabel.setToolTipText(ResourceMgr.getDescription("LblNoWordSep"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(5, 12, 0, 0);
    add(noWordSepLabel, gridBagConstraints);

    noWordSep.setHorizontalAlignment(javax.swing.JTextField.LEFT);
    noWordSep.setMinimumSize(new java.awt.Dimension(72, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 16;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(3, 11, 0, 15);
    add(noWordSep, gridBagConstraints);

    jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

    errorColorLabel.setText(ResourceMgr.getString("LblSelectErrorColor"));
    errorColorLabel.setToolTipText(ResourceMgr.getDescription("LblSelectErrorColor"));
    jPanel1.add(errorColorLabel);
    jPanel1.add(errorColor);

    selectionColorLabel.setText(ResourceMgr.getString("LblSelectionColor"));
    selectionColorLabel.setToolTipText(ResourceMgr.getDescription("LblSelectionColor"));
    jPanel1.add(selectionColorLabel);
    jPanel1.add(selectionColor);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 17;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 13);
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel altDelimLabel;
  private workbench.gui.components.DelimiterDefinitionPanel alternateDelim;
  private javax.swing.JCheckBox autoAdvance;
  private javax.swing.JLabel autoAdvanceLabel;
  private javax.swing.JCheckBox closePopup;
  private javax.swing.JComboBox completionColumnSort;
  private javax.swing.JComboBox completionPasteCase;
  private workbench.gui.components.WbFontPicker editorFont;
  private javax.swing.JLabel editorFontLabel;
  private javax.swing.JLabel editorTabSizeLabel;
  private javax.swing.JTextField electricScroll;
  private javax.swing.JLabel electricScrollLabel;
  private workbench.gui.components.WbColorPicker errorColor;
  private javax.swing.JLabel errorColorLabel;
  private javax.swing.JComboBox externalLineEnding;
  private javax.swing.JLabel externalLineEndingLabel;
  private javax.swing.JLabel filesInHistoryLabel;
  private javax.swing.JTextField historySizeField;
  private javax.swing.JLabel historySizeLabel;
  private javax.swing.JCheckBox includeFilesInHistory;
  private javax.swing.JComboBox internalLineEnding;
  private javax.swing.JLabel internalLineEndingLabel;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JLabel labelCloseSearch;
  private javax.swing.JTextField noWordSep;
  private javax.swing.JLabel noWordSepLabel;
  private javax.swing.JLabel pasteLabel;
  private javax.swing.JLabel pasterOrderLabel;
  private javax.swing.JLabel rightClickLabel;
  private javax.swing.JCheckBox rightClickMovesCursor;
  private workbench.gui.components.WbColorPicker selectionColor;
  private javax.swing.JLabel selectionColorLabel;
  private javax.swing.JTextField tabSize;
  // End of variables declaration//GEN-END:variables

}
