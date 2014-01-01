/*
 * EditorOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2014, Thomas Kellerer
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
package workbench.gui.settings;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.resource.StoreableKeyStroke;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DelimiterDefinitionPanel;
import workbench.gui.components.NumberField;
import workbench.gui.components.TextFieldWidthAdjuster;
import workbench.gui.components.WbFilePicker;
import workbench.gui.editor.BracketCompleter;
import workbench.gui.sql.FileReloadType;

import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class EditorOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener, ValidatingComponent
{

	public EditorOptionsPanel()
	{
		super();
		initComponents();
		TextFieldWidthAdjuster adjuster = new TextFieldWidthAdjuster();
		adjuster.adjustAllFields(this);
		defaultDir.setSelectDirectoryOnly(true);
		StoreableKeyStroke space = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
		StoreableKeyStroke shiftSpace = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.SHIFT_MASK));
		StoreableKeyStroke tab = new StoreableKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
		ComboBoxModel model = new DefaultComboBoxModel(new Object[] { space, shiftSpace, tab});
		cbExpansionKey.setModel(model);
	}

	@Override
	public void restoreSettings()
	{

		String[] items = new String[] {
			ResourceMgr.getString("LblLTDefault"),
			ResourceMgr.getString("LblLTDos"),
			ResourceMgr.getString("LblLTUnix")
		};

		internalLineEnding.setModel(new DefaultComboBoxModel(items));
		externalLineEnding.setModel(new DefaultComboBoxModel(items));

		reloadType.setModel(new DefaultComboBoxModel(FileReloadType.values()));
		reloadType.doLayout();

		FileReloadType type = GuiSettings.getReloadType();
		reloadType.setSelectedItem(type);

		String value = Settings.getInstance().getInteralLineEndingValue();
		internalLineEnding.setSelectedIndex(lineEndingValueToIndex(value));

		value = Settings.getInstance().getExternalLineEndingValue();
		externalLineEnding.setSelectedIndex(lineEndingValueToIndex(value));
		alternateDelim.setDelimiter(Settings.getInstance().getAlternateDelimiter());

		noWordSep.setText(Settings.getInstance().getEditorNoWordSep());
		useTabs.setSelected(Settings.getInstance().getEditorUseTabCharacter());
		followCurrentDir.setSelected(GuiSettings.getFollowFileDirectory());
		defaultDir.setFilename(GuiSettings.getDefaultFileDir());
		defaultDir.setEnabled(followCurrentDir.isSelected());
		keepHilite.setSelected(GuiSettings.getKeepCurrentSqlHighlight());
		historySizeField.setText(Integer.toString(Settings.getInstance().getMaxHistorySize()));
		electricScroll.setText(Integer.toString(Settings.getInstance().getElectricScroll()));
		tabSize.setText(Settings.getInstance().getProperty("workbench.editor.tabwidth", "2"));
		hiliteCurrent.setSelected(Settings.getInstance().getHighlightCurrentStatement());
		alwaysAllowExecSel.setSelected(!GuiSettings.getExecuteOnlySelected());
		allowEditDuringExec.setSelected(!GuiSettings.getDisableEditorDuringExecution());
		emptyLineDelimiter.setSelected(Settings.getInstance().getEmptyLineIsDelimiter());
		hiliteError.setSelected(GuiSettings.getHighlightErrorStatement());
		autoCloseBrackets.setText(Settings.getInstance().getProperty(GuiSettings.PROPERTY_COMPLETE_CHARS, ""));
		StoreableKeyStroke key = new StoreableKeyStroke(GuiSettings.getExpansionKey());
		cbExpansionKey.setSelectedItem(key);
		int lines = GuiSettings.getWheelScrollLines();
		if (lines <= 0)
		{
			wheelScrollLines.setText("");
		}
		else
		{
			wheelScrollLines.setText(Integer.toString(lines));
		}
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

	@Override
	public void saveSettings()
	{
		Settings set = Settings.getInstance();
		set.setMaxHistorySize(((NumberField)this.historySizeField).getValue());

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
		set.setEditorUseTabCharacter(useTabs.isSelected());
		set.setProperty(Settings.PROPERTY_HIGHLIGHT_CURRENT_STATEMENT, hiliteCurrent.isSelected());
		set.setEmptyLineIsDelimiter(emptyLineDelimiter.isSelected());
		GuiSettings.setDefaultFileDir(defaultDir.getFilename());
		GuiSettings.setFollowFileDirectory(followCurrentDir.isSelected());
		GuiSettings.setKeepCurrentSqlHighlight(keepHilite.isSelected());
		GuiSettings.setExecuteOnlySelected(!alwaysAllowExecSel.isSelected());
		GuiSettings.setDisableEditorDuringExecution(!allowEditDuringExec.isSelected());
		GuiSettings.setHighlightErrorStatement(hiliteError.isSelected());
		set.setProperty(GuiSettings.PROPERTY_COMPLETE_CHARS, autoCloseBrackets.getText());

		StoreableKeyStroke key = (StoreableKeyStroke) cbExpansionKey.getSelectedItem();
		GuiSettings.setExpansionKey(key.getKeyStroke());

		if (StringUtil.isNumber(wheelScrollLines.getText()))
		{
			int lines = StringUtil.getIntValue(wheelScrollLines.getText(), -1);
			GuiSettings.setWheelScrollLines(lines);
		}

		FileReloadType fileReloadType = (FileReloadType) this.reloadType.getSelectedItem();
		GuiSettings.setReloadType(fileReloadType);
	}

	@Override
	public void componentDisplayed()
	{
	}

	@Override
	public boolean validateInput()
	{
		if (BracketCompleter.isValidDefinition(autoCloseBrackets.getText()))
		{
			return true;
		}
		WbSwingUtilities.showErrorMessageKey(this, "ErrAutoClsBrkt");
		EventQueue.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				autoCloseBrackets.requestFocusInWindow();
			}
		});
		return false;
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

    editorTabSizeLabel = new JLabel();
    tabSize = new JTextField();
    altDelimLabel = new JLabel();
    historySizeLabel = new JLabel();
    historySizeField = new NumberField();
    electricScrollLabel = new JLabel();
    electricScroll = new JTextField();
    internalLineEndingLabel = new JLabel();
    internalLineEnding = new JComboBox();
    externalLineEndingLabel = new JLabel();
    externalLineEnding = new JComboBox();
    includeFilesInHistory = new JCheckBox();
    alternateDelim = new DelimiterDefinitionPanel();
    noWordSepLabel = new JLabel();
    useTabs = new JCheckBox();
    noWordSep = new JTextField();
    jPanel2 = new JPanel();
    hiliteCurrent = new JCheckBox();
    keepHilite = new JCheckBox();
    allowEditDuringExec = new JCheckBox();
    rightClickMovesCursor = new JCheckBox();
    alwaysAllowExecSel = new JCheckBox();
    autoAdvance = new JCheckBox();
    emptyLineDelimiter = new JCheckBox();
    hiliteError = new JCheckBox();
    jPanel1 = new JPanel();
    followCurrentDir = new JCheckBox();
    jLabel1 = new JLabel();
    defaultDir = new WbFilePicker();
    jLabel2 = new JLabel();
    autoCloseBrackets = new JTextField();
    jLabel3 = new JLabel();
    cbExpansionKey = new JComboBox();
    wheelScrollLabel = new JLabel();
    wheelScrollLines = new JTextField();
    reloadLabel = new JLabel();
    reloadType = new JComboBox();

    setLayout(new GridBagLayout());

    editorTabSizeLabel.setLabelFor(tabSize);
    editorTabSizeLabel.setText(ResourceMgr.getString("LblTabWidth")); // NOI18N
    editorTabSizeLabel.setToolTipText(ResourceMgr.getString("d_LblTabWidth")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(editorTabSizeLabel, gridBagConstraints);

    tabSize.setColumns(4);
    tabSize.setHorizontalAlignment(JTextField.LEFT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(tabSize, gridBagConstraints);

    altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit")); // NOI18N
    altDelimLabel.setToolTipText(ResourceMgr.getString("d_LblAltDelimit")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(8, 12, 0, 0);
    add(altDelimLabel, gridBagConstraints);

    historySizeLabel.setText(ResourceMgr.getString("LblHistorySize")); // NOI18N
    historySizeLabel.setToolTipText(ResourceMgr.getString("d_LblHistorySize")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(historySizeLabel, gridBagConstraints);

    historySizeField.setColumns(4);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(historySizeField, gridBagConstraints);

    electricScrollLabel.setText(ResourceMgr.getString("LblSettingElectricScroll")); // NOI18N
    electricScrollLabel.setToolTipText(ResourceMgr.getString("d_LblSettingElectricScroll")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 0, 0, 0);
    add(electricScrollLabel, gridBagConstraints);

    electricScroll.setColumns(4);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(electricScroll, gridBagConstraints);

    internalLineEndingLabel.setText(ResourceMgr.getString("LblIntLineEnding")); // NOI18N
    internalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblIntLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(10, 12, 0, 0);
    add(internalLineEndingLabel, gridBagConstraints);

    internalLineEnding.setToolTipText(ResourceMgr.getDescription("LblIntLineEnding"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 11, 0, 15);
    add(internalLineEnding, gridBagConstraints);

    externalLineEndingLabel.setText(ResourceMgr.getString("LblExtLineEnding")); // NOI18N
    externalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblExtLineEnding")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 12, 0, 0);
    add(externalLineEndingLabel, gridBagConstraints);

    externalLineEnding.setToolTipText(ResourceMgr.getDescription("LblExtLineEnding"));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
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
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 0, 0, 11);
    add(includeFilesInHistory, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 10, 0, 15);
    add(alternateDelim, gridBagConstraints);

    noWordSepLabel.setLabelFor(noWordSep);
    noWordSepLabel.setText(ResourceMgr.getString("LblNoWordSep")); // NOI18N
    noWordSepLabel.setToolTipText(ResourceMgr.getString("d_LblNoWordSep")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 12, 0, 0);
    add(noWordSepLabel, gridBagConstraints);

    useTabs.setText(ResourceMgr.getString("LblEditorUseTabs")); // NOI18N
    useTabs.setToolTipText(ResourceMgr.getString("d_LblEditorUseTabs")); // NOI18N
    useTabs.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 0, 0, 10);
    add(useTabs, gridBagConstraints);

    noWordSep.setColumns(8);
    noWordSep.setHorizontalAlignment(JTextField.LEFT);
    noWordSep.setName("nowordsep"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(noWordSep, gridBagConstraints);

    jPanel2.setLayout(new GridBagLayout());

    hiliteCurrent.setText(ResourceMgr.getString("MnuTxtHighlightCurrent")); // NOI18N
    hiliteCurrent.setToolTipText(ResourceMgr.getString("d_MnuTxtHighlightCurrent")); // NOI18N
    hiliteCurrent.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    jPanel2.add(hiliteCurrent, gridBagConstraints);

    keepHilite.setText(ResourceMgr.getString("LblKeepHilite")); // NOI18N
    keepHilite.setToolTipText(ResourceMgr.getString("d_LblKeepHilite")); // NOI18N
    keepHilite.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 15, 0, 0);
    jPanel2.add(keepHilite, gridBagConstraints);

    allowEditDuringExec.setText(ResourceMgr.getString("LblAllowEditExecSQL")); // NOI18N
    allowEditDuringExec.setToolTipText(ResourceMgr.getString("d_LblAllowEditExecSQL")); // NOI18N
    allowEditDuringExec.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(allowEditDuringExec, gridBagConstraints);

    rightClickMovesCursor.setSelected(Settings.getInstance().getRightClickMovesCursor());
    rightClickMovesCursor.setText(ResourceMgr.getString("LblRightClickMove")); // NOI18N
    rightClickMovesCursor.setToolTipText(ResourceMgr.getString("d_LblRightClickMove")); // NOI18N
    rightClickMovesCursor.setBorder(null);
    rightClickMovesCursor.setMaximumSize(new Dimension(93, 15));
    rightClickMovesCursor.setMinimumSize(new Dimension(93, 15));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(7, 15, 0, 0);
    jPanel2.add(rightClickMovesCursor, gridBagConstraints);

    alwaysAllowExecSel.setText(ResourceMgr.getString("LblExecSelOnly")); // NOI18N
    alwaysAllowExecSel.setToolTipText(ResourceMgr.getString("d_LblExecSelOnly")); // NOI18N
    alwaysAllowExecSel.setBorder(null);
    alwaysAllowExecSel.setHorizontalAlignment(SwingConstants.LEFT);
    alwaysAllowExecSel.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(alwaysAllowExecSel, gridBagConstraints);

    autoAdvance.setSelected(Settings.getInstance().getAutoJumpNextStatement());
    autoAdvance.setText(ResourceMgr.getString("LblAutoAdvance")); // NOI18N
    autoAdvance.setToolTipText(ResourceMgr.getString("d_LblAutoAdvance")); // NOI18N
    autoAdvance.setBorder(null);
    autoAdvance.setHorizontalAlignment(SwingConstants.LEFT);
    autoAdvance.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 15, 0, 0);
    jPanel2.add(autoAdvance, gridBagConstraints);

    emptyLineDelimiter.setText(ResourceMgr.getString("LblEmptyLineDelimiter")); // NOI18N
    emptyLineDelimiter.setToolTipText(ResourceMgr.getString("d_LblEmptyLineDelimiter")); // NOI18N
    emptyLineDelimiter.setBorder(null);
    emptyLineDelimiter.setHorizontalAlignment(SwingConstants.LEFT);
    emptyLineDelimiter.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 15, 0, 0);
    jPanel2.add(emptyLineDelimiter, gridBagConstraints);

    hiliteError.setText(ResourceMgr.getString("LblHiliteErr")); // NOI18N
    hiliteError.setToolTipText(ResourceMgr.getString("d_LblHiliteErr")); // NOI18N
    hiliteError.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(hiliteError, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 13, 0, 0);
    add(jPanel2, gridBagConstraints);

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
    gridBagConstraints.gridy = 12;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(12, 13, 0, 15);
    add(jPanel1, gridBagConstraints);

    jLabel2.setLabelFor(autoCloseBrackets);
    jLabel2.setText(ResourceMgr.getString("LblAutoCloseBrkt")); // NOI18N
    jLabel2.setToolTipText(ResourceMgr.getString("d_LblAutoCloseBrkt")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    add(jLabel2, gridBagConstraints);

    autoCloseBrackets.setColumns(8);
    autoCloseBrackets.setToolTipText(ResourceMgr.getString("d_LblAutoCloseBrkt")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(autoCloseBrackets, gridBagConstraints);

    jLabel3.setLabelFor(cbExpansionKey);
    jLabel3.setText(ResourceMgr.getString("LblExpandKey")); // NOI18N
    jLabel3.setToolTipText(ResourceMgr.getString("d_LblExpandKey")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(3, 12, 0, 0);
    add(jLabel3, gridBagConstraints);

    cbExpansionKey.setModel(new DefaultComboBoxModel(new String[] { "Space", "Shift-Space", "Tab" }));
    cbExpansionKey.setToolTipText(ResourceMgr.getString("d_LblExpandKey")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(cbExpansionKey, gridBagConstraints);

    wheelScrollLabel.setText(ResourceMgr.getString("LblWheelScrLines")); // NOI18N
    wheelScrollLabel.setToolTipText(ResourceMgr.getString("d_LblWheelScrLines")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(wheelScrollLabel, gridBagConstraints);

    wheelScrollLines.setColumns(4);
    wheelScrollLines.setToolTipText(ResourceMgr.getString("d_LblWheelScrLines")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(wheelScrollLines, gridBagConstraints);

    reloadLabel.setLabelFor(reloadType);
    reloadLabel.setText(ResourceMgr.getString("LblRldBehaviour")); // NOI18N
    reloadLabel.setToolTipText(ResourceMgr.getString("d_LblRldBehaviour")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    add(reloadLabel, gridBagConstraints);

    reloadType.setModel(new DefaultComboBoxModel(new String[] { "Never", "Prompt", "Automatic" }));
    reloadType.setToolTipText(ResourceMgr.getString("d_LblRldBehaviour")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(reloadType, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(ActionEvent evt)
  {
    if (evt.getSource() == followCurrentDir)
    {
      EditorOptionsPanel.this.followCurrentDirActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

	private void followCurrentDirActionPerformed(ActionEvent evt)//GEN-FIRST:event_followCurrentDirActionPerformed
	{//GEN-HEADEREND:event_followCurrentDirActionPerformed
		defaultDir.setEnabled(followCurrentDir.isSelected());
	}//GEN-LAST:event_followCurrentDirActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox allowEditDuringExec;
  private JLabel altDelimLabel;
  private DelimiterDefinitionPanel alternateDelim;
  private JCheckBox alwaysAllowExecSel;
  private JCheckBox autoAdvance;
  private JTextField autoCloseBrackets;
  private JComboBox cbExpansionKey;
  private WbFilePicker defaultDir;
  private JLabel editorTabSizeLabel;
  private JTextField electricScroll;
  private JLabel electricScrollLabel;
  private JCheckBox emptyLineDelimiter;
  private JComboBox externalLineEnding;
  private JLabel externalLineEndingLabel;
  private JCheckBox followCurrentDir;
  private JCheckBox hiliteCurrent;
  private JCheckBox hiliteError;
  private JTextField historySizeField;
  private JLabel historySizeLabel;
  private JCheckBox includeFilesInHistory;
  private JComboBox internalLineEnding;
  private JLabel internalLineEndingLabel;
  private JLabel jLabel1;
  private JLabel jLabel2;
  private JLabel jLabel3;
  private JPanel jPanel1;
  private JPanel jPanel2;
  private JCheckBox keepHilite;
  private JTextField noWordSep;
  private JLabel noWordSepLabel;
  private JLabel reloadLabel;
  private JComboBox reloadType;
  private JCheckBox rightClickMovesCursor;
  private JTextField tabSize;
  private JCheckBox useTabs;
  private JLabel wheelScrollLabel;
  private JTextField wheelScrollLines;
  // End of variables declaration//GEN-END:variables

}
