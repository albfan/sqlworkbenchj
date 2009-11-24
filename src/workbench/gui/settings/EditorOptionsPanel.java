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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import workbench.gui.components.DelimiterDefinitionPanel;
import workbench.gui.components.NumberField;
import workbench.gui.components.WbFilePicker;
import workbench.interfaces.Restoreable;
import workbench.resource.ColumnSortType;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class EditorOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener
{

	public EditorOptionsPanel()
	{
		super();
		initComponents();
		defaultDir.setSelectDirectoryOnly(true);
	}

	public void restoreSettings()
	{

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
		useTabs.setSelected(Settings.getInstance().getEditorUseTabCharacter());
		followCurrentDir.setSelected(GuiSettings.getFollowFileDirectory());
		defaultDir.setFilename(GuiSettings.getDefaultFileDir());
		defaultDir.setEnabled(followCurrentDir.isSelected());
		keepHilite.setSelected(GuiSettings.getKeepCurrentSqlHighlight());
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
		set.setEditorUseTabCharacter(useTabs.isSelected());
		GuiSettings.setDefaultFileDir(defaultDir.getFilename());
		GuiSettings.setFollowFileDirectory(followCurrentDir.isSelected());
		GuiSettings.setKeepCurrentSqlHighlight(keepHilite.isSelected());
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
		GridBagConstraints gridBagConstraints;

    editorTabSizeLabel = new JLabel();
    tabSize = new JTextField();
    altDelimLabel = new JLabel();
    historySizeLabel = new JLabel();
    historySizeField = new NumberField();
    electricScrollLabel = new JLabel();
    electricScroll = new JTextField();
    rightClickMovesCursor = new JCheckBox();
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
    useTabs = new JCheckBox();
    jPanel1 = new JPanel();
    followCurrentDir = new JCheckBox();
    jLabel1 = new JLabel();
    defaultDir = new WbFilePicker();
    jPanel2 = new JPanel();
    autoAdvance = new JCheckBox();
    keepHilite = new JCheckBox();

    setLayout(new GridBagLayout());

    editorTabSizeLabel.setText(ResourceMgr.getString("LblTabWidth")); // NOI18N
    editorTabSizeLabel.setToolTipText(ResourceMgr.getString("d_LblTabWidth")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(editorTabSizeLabel, gridBagConstraints);

    tabSize.setHorizontalAlignment(JTextField.LEFT);
    tabSize.setText(Settings.getInstance().getProperty("workbench.editor.tabwidth", "2"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(tabSize, gridBagConstraints);

    altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit")); // NOI18N
    altDelimLabel.setToolTipText(ResourceMgr.getString("d_LblAltDelimit")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(8, 12, 0, 0);
    add(altDelimLabel, gridBagConstraints);

    historySizeLabel.setText(ResourceMgr.getString("LblHistorySize")); // NOI18N
    historySizeLabel.setToolTipText(ResourceMgr.getString("d_LblHistorySize")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(historySizeLabel, gridBagConstraints);

    historySizeField.setText(Integer.toString(Settings.getInstance().getMaxHistorySize()));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(historySizeField, gridBagConstraints);

    electricScrollLabel.setText(ResourceMgr.getString("LblSettingElectricScroll")); // NOI18N
    electricScrollLabel.setToolTipText(ResourceMgr.getString("d_LblSettingElectricScroll")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(electricScrollLabel, gridBagConstraints);

    electricScroll.setText(Integer.toString(Settings.getInstance().getElectricScroll()));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(electricScroll, gridBagConstraints);

    rightClickMovesCursor.setSelected(Settings.getInstance().getRightClickMovesCursor());
    rightClickMovesCursor.setText(ResourceMgr.getString("LblRightClickMove")); // NOI18N
    rightClickMovesCursor.setToolTipText(ResourceMgr.getString("d_LblRightClickMove")); // NOI18N
    rightClickMovesCursor.setBorder(null);
    rightClickMovesCursor.setMaximumSize(new Dimension(93, 15));
    rightClickMovesCursor.setMinimumSize(new Dimension(93, 15));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(10, 12, 0, 11);
    add(rightClickMovesCursor, gridBagConstraints);

    closePopup.setSelected(Settings.getInstance().getCloseAutoCompletionWithSearch());
    closePopup.setText(ResourceMgr.getString("TxtCloseCompletion"));
    closePopup.setToolTipText(ResourceMgr.getDescription("TxtCloseCompletion"));
    closePopup.setBorder(null);
    closePopup.setHorizontalAlignment(SwingConstants.LEFT);
    closePopup.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(9, 0, 0, 11);
    add(closePopup, gridBagConstraints);

    completionPasteCase.setModel(new DefaultComboBoxModel(new String[] { "Lowercase", "Uppercase", "As is" }));
    completionPasteCase.setToolTipText(ResourceMgr.getDescription("LblPasteCase"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(8, 11, 0, 15);
    add(completionPasteCase, gridBagConstraints);

    pasteLabel.setLabelFor(completionPasteCase);
    pasteLabel.setText(ResourceMgr.getString("LblPasteCase")); // NOI18N
    pasteLabel.setToolTipText(ResourceMgr.getString("d_LblPasteCase")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(11, 12, 0, 0);
    add(pasteLabel, gridBagConstraints);

    internalLineEndingLabel.setText(ResourceMgr.getString("LblIntLineEnding")); // NOI18N
    internalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblIntLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(internalLineEndingLabel, gridBagConstraints);

    internalLineEnding.setToolTipText(ResourceMgr.getDescription("LblIntLineEnding"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(internalLineEnding, gridBagConstraints);

    externalLineEndingLabel.setText(ResourceMgr.getString("LblExtLineEnding")); // NOI18N
    externalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblExtLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(externalLineEndingLabel, gridBagConstraints);

    externalLineEnding.setToolTipText(ResourceMgr.getDescription("LblExtLineEnding"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
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
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(5, 0, 0, 11);
    add(includeFilesInHistory, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 10, 0, 15);
    add(alternateDelim, gridBagConstraints);

    pasterOrderLabel.setLabelFor(completionColumnSort);
    pasterOrderLabel.setText(ResourceMgr.getString("LblPasteSort")); // NOI18N
    pasterOrderLabel.setToolTipText(ResourceMgr.getString("d_LblPasteSort")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 12, 0, 0);
    add(pasterOrderLabel, gridBagConstraints);

    completionColumnSort.setToolTipText(ResourceMgr.getDescription("LblPasteSort"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(6, 11, 0, 15);
    add(completionColumnSort, gridBagConstraints);

    noWordSepLabel.setText(ResourceMgr.getString("LblNoWordSep")); // NOI18N
    noWordSepLabel.setToolTipText(ResourceMgr.getString("d_LblNoWordSep")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 12, 0, 0);
    add(noWordSepLabel, gridBagConstraints);

    noWordSep.setHorizontalAlignment(JTextField.LEFT);
    noWordSep.setName("nowordsep"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(noWordSep, gridBagConstraints);

    useTabs.setText(ResourceMgr.getString("LblEditorUseTabs")); // NOI18N
    useTabs.setToolTipText(ResourceMgr.getString("d_LblEditorUseTabs")); // NOI18N
    useTabs.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 0, 0, 10);
    add(useTabs, gridBagConstraints);

    jPanel1.setLayout(new GridBagLayout());

    followCurrentDir.setText(ResourceMgr.getString("LblEditorFollowDir")); // NOI18N
    followCurrentDir.setToolTipText(ResourceMgr.getString("d_LblEditorFollowDir")); // NOI18N
    followCurrentDir.setBorder(null);
    followCurrentDir.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    jPanel1.add(followCurrentDir, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblEditorDefaultDir")); // NOI18N
    jLabel1.setToolTipText(ResourceMgr.getString("d_LblEditorDefaultDir")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 0, 0, 11);
    jPanel1.add(jLabel1, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    jPanel1.add(defaultDir, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(10, 12, 0, 15);
    add(jPanel1, gridBagConstraints);

    jPanel2.setLayout(new GridBagLayout());

    autoAdvance.setSelected(Settings.getInstance().getAutoJumpNextStatement());
    autoAdvance.setText(ResourceMgr.getString("LblAutoAdvance")); // NOI18N
    autoAdvance.setToolTipText(ResourceMgr.getString("d_LblAutoAdvance")); // NOI18N
    autoAdvance.setBorder(null);
    autoAdvance.setHorizontalAlignment(SwingConstants.LEFT);
    autoAdvance.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    jPanel2.add(autoAdvance, gridBagConstraints);

    keepHilite.setText(ResourceMgr.getString("LblKeepHilite")); // NOI18N
    keepHilite.setToolTipText(ResourceMgr.getString("d_LblKeepHilite")); // NOI18N
    keepHilite.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 13, 0, 0);
    jPanel2.add(keepHilite, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(9, 12, 0, 15);
    add(jPanel2, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt) {
    if (evt.getSource() == followCurrentDir) {
      EditorOptionsPanel.this.followCurrentDirActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

	private void followCurrentDirActionPerformed(ActionEvent evt)//GEN-FIRST:event_followCurrentDirActionPerformed
	{//GEN-HEADEREND:event_followCurrentDirActionPerformed
		defaultDir.setEnabled(followCurrentDir.isSelected());
	}//GEN-LAST:event_followCurrentDirActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JLabel altDelimLabel;
  private DelimiterDefinitionPanel alternateDelim;
  private JCheckBox autoAdvance;
  private JCheckBox closePopup;
  private JComboBox completionColumnSort;
  private JComboBox completionPasteCase;
  private WbFilePicker defaultDir;
  private JLabel editorTabSizeLabel;
  private JTextField electricScroll;
  private JLabel electricScrollLabel;
  private JComboBox externalLineEnding;
  private JLabel externalLineEndingLabel;
  private JCheckBox followCurrentDir;
  private JTextField historySizeField;
  private JLabel historySizeLabel;
  private JCheckBox includeFilesInHistory;
  private JComboBox internalLineEnding;
  private JLabel internalLineEndingLabel;
  private JLabel jLabel1;
  private JPanel jPanel1;
  private JPanel jPanel2;
  private JCheckBox keepHilite;
  private JTextField noWordSep;
  private JLabel noWordSepLabel;
  private JLabel pasteLabel;
  private JLabel pasterOrderLabel;
  private JCheckBox rightClickMovesCursor;
  private JTextField tabSize;
  private JCheckBox useTabs;
  // End of variables declaration//GEN-END:variables

}
