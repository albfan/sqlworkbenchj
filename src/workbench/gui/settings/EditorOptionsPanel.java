/*
 * EditorOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.settings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DelimiterDefinitionPanel;
import workbench.gui.components.NumberField;
import workbench.gui.components.WbFilePicker;
import workbench.gui.editor.BracketCompleter;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
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
		defaultDir.setSelectDirectoryOnly(true);
	}

	@Override
	public void restoreSettings()
	{

		String[] items = new String[] {
			ResourceMgr.getString("LblLTDefault"),
			ResourceMgr.getString("LblLTDos"),
			ResourceMgr.getString("LblLTUnix")
		};

		this.internalLineEnding.setModel(new DefaultComboBoxModel(items));
		this.externalLineEnding.setModel(new DefaultComboBoxModel(items));

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
		autoCloseBrackets.setText(Settings.getInstance().getProperty(GuiSettings.PROPERTY_COMPLETE_CHARS, ""));
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
		set.setProperty(GuiSettings.PROPERTY_COMPLETE_CHARS, autoCloseBrackets.getText());
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
    autoAdvance = new JCheckBox();
    jPanel2 = new JPanel();
    keepHilite = new JCheckBox();
    hiliteCurrent = new JCheckBox();
    allowEditDuringExec = new JCheckBox();
    rightClickMovesCursor = new JCheckBox();
    jPanel1 = new JPanel();
    followCurrentDir = new JCheckBox();
    jLabel1 = new JLabel();
    defaultDir = new WbFilePicker();
    alwaysAllowExecSel = new JCheckBox();
    emptyLineDelimiter = new JCheckBox();
    jLabel2 = new JLabel();
    autoCloseBrackets = new JTextField();

    setLayout(new GridBagLayout());

    editorTabSizeLabel.setText(ResourceMgr.getString("LblTabWidth"));     editorTabSizeLabel.setToolTipText(ResourceMgr.getString("d_LblTabWidth"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(editorTabSizeLabel, gridBagConstraints);

    tabSize.setColumns(6);
    tabSize.setHorizontalAlignment(JTextField.LEFT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(tabSize, gridBagConstraints);

    altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit"));     altDelimLabel.setToolTipText(ResourceMgr.getString("d_LblAltDelimit"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(8, 12, 0, 0);
    add(altDelimLabel, gridBagConstraints);

    historySizeLabel.setText(ResourceMgr.getString("LblHistorySize"));     historySizeLabel.setToolTipText(ResourceMgr.getString("d_LblHistorySize"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(historySizeLabel, gridBagConstraints);

    historySizeField.setColumns(6);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(historySizeField, gridBagConstraints);

    electricScrollLabel.setText(ResourceMgr.getString("LblSettingElectricScroll"));     electricScrollLabel.setToolTipText(ResourceMgr.getString("d_LblSettingElectricScroll"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 12, 0, 0);
    add(electricScrollLabel, gridBagConstraints);

    electricScroll.setColumns(6);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 11, 0, 15);
    add(electricScroll, gridBagConstraints);

    internalLineEndingLabel.setText(ResourceMgr.getString("LblIntLineEnding"));     internalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblIntLineEnding"));     gridBagConstraints = new GridBagConstraints();
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
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 11, 0, 15);
    add(internalLineEnding, gridBagConstraints);

    externalLineEndingLabel.setText(ResourceMgr.getString("LblExtLineEnding"));     externalLineEndingLabel.setToolTipText(ResourceMgr.getString("d_LblExtLineEnding"));     gridBagConstraints = new GridBagConstraints();
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
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(4, 11, 0, 15);
    add(externalLineEnding, gridBagConstraints);

    includeFilesInHistory.setSelected(Settings.getInstance().getStoreFilesInHistory());
    includeFilesInHistory.setText(ResourceMgr.getString("TxtHistoryIncFiles"));     includeFilesInHistory.setToolTipText(ResourceMgr.getString("d_TxtHistoryIncFiles"));     includeFilesInHistory.setBorder(null);
    includeFilesInHistory.setHorizontalAlignment(SwingConstants.LEFT);
    includeFilesInHistory.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(5, 0, 0, 11);
    add(includeFilesInHistory, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(6, 10, 0, 15);
    add(alternateDelim, gridBagConstraints);

    noWordSepLabel.setText(ResourceMgr.getString("LblNoWordSep"));     noWordSepLabel.setToolTipText(ResourceMgr.getString("d_LblNoWordSep"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 12, 0, 0);
    add(noWordSepLabel, gridBagConstraints);

    useTabs.setText(ResourceMgr.getString("LblEditorUseTabs"));     useTabs.setToolTipText(ResourceMgr.getString("d_LblEditorUseTabs"));     useTabs.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(5, 0, 0, 10);
    add(useTabs, gridBagConstraints);

    noWordSep.setColumns(8);
    noWordSep.setHorizontalAlignment(JTextField.LEFT);
    noWordSep.setName("nowordsep"); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(noWordSep, gridBagConstraints);

    autoAdvance.setSelected(Settings.getInstance().getAutoJumpNextStatement());
    autoAdvance.setText(ResourceMgr.getString("LblAutoAdvance"));     autoAdvance.setToolTipText(ResourceMgr.getString("d_LblAutoAdvance"));     autoAdvance.setBorder(null);
    autoAdvance.setHorizontalAlignment(SwingConstants.LEFT);
    autoAdvance.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 13, 0, 0);
    add(autoAdvance, gridBagConstraints);

    jPanel2.setLayout(new GridBagLayout());

    keepHilite.setText(ResourceMgr.getString("LblKeepHilite"));     keepHilite.setToolTipText(ResourceMgr.getString("d_LblKeepHilite"));     keepHilite.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new Insets(0, 15, 0, 0);
    jPanel2.add(keepHilite, gridBagConstraints);

    hiliteCurrent.setText(ResourceMgr.getString("MnuTxtHighlightCurrent"));     hiliteCurrent.setToolTipText(ResourceMgr.getString("d_MnuTxtHighlightCurrent"));     hiliteCurrent.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    jPanel2.add(hiliteCurrent, gridBagConstraints);

    allowEditDuringExec.setText(ResourceMgr.getString("LblAllowEditExecSQL"));     allowEditDuringExec.setToolTipText(ResourceMgr.getString("d_LblAllowEditExecSQL"));     allowEditDuringExec.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(allowEditDuringExec, gridBagConstraints);

    rightClickMovesCursor.setSelected(Settings.getInstance().getRightClickMovesCursor());
    rightClickMovesCursor.setText(ResourceMgr.getString("LblRightClickMove"));     rightClickMovesCursor.setToolTipText(ResourceMgr.getString("d_LblRightClickMove"));     rightClickMovesCursor.setBorder(null);
    rightClickMovesCursor.setMaximumSize(new Dimension(93, 15));
    rightClickMovesCursor.setMinimumSize(new Dimension(93, 15));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(7, 15, 0, 0);
    jPanel2.add(rightClickMovesCursor, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(9, 13, 0, 0);
    add(jPanel2, gridBagConstraints);

    jPanel1.setLayout(new GridBagLayout());

    followCurrentDir.setText(ResourceMgr.getString("LblEditorFollowDir"));     followCurrentDir.setToolTipText(ResourceMgr.getString("d_LblEditorFollowDir"));     followCurrentDir.setBorder(null);
    followCurrentDir.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    jPanel1.add(followCurrentDir, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblEditorDefaultDir"));     jLabel1.setToolTipText(ResourceMgr.getString("d_LblEditorDefaultDir"));     gridBagConstraints = new GridBagConstraints();
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
    gridBagConstraints.gridy = 14;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(10, 13, 0, 15);
    add(jPanel1, gridBagConstraints);

    alwaysAllowExecSel.setText(ResourceMgr.getString("LblExecSelOnly"));     alwaysAllowExecSel.setToolTipText(ResourceMgr.getString("d_LblExecSelOnly"));     alwaysAllowExecSel.setBorder(null);
    alwaysAllowExecSel.setHorizontalAlignment(SwingConstants.LEFT);
    alwaysAllowExecSel.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 13, 0, 0);
    add(alwaysAllowExecSel, gridBagConstraints);

    emptyLineDelimiter.setText(ResourceMgr.getString("LblEmptyLineDelimiter"));     emptyLineDelimiter.setToolTipText(ResourceMgr.getString("d_LblEmptyLineDelimiter"));     emptyLineDelimiter.setBorder(null);
    emptyLineDelimiter.setHorizontalAlignment(SwingConstants.LEFT);
    emptyLineDelimiter.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 13, 0, 0);
    add(emptyLineDelimiter, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblAutoCloseBrkt"));     jLabel2.setToolTipText(ResourceMgr.getString("d_LblAutoCloseBrkt"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(3, 12, 0, 0);
    add(jLabel2, gridBagConstraints);

    autoCloseBrackets.setToolTipText(ResourceMgr.getString("d_LblAutoCloseBrkt"));     gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(3, 11, 0, 15);
    add(autoCloseBrackets, gridBagConstraints);
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
  private WbFilePicker defaultDir;
  private JLabel editorTabSizeLabel;
  private JTextField electricScroll;
  private JLabel electricScrollLabel;
  private JCheckBox emptyLineDelimiter;
  private JComboBox externalLineEnding;
  private JLabel externalLineEndingLabel;
  private JCheckBox followCurrentDir;
  private JCheckBox hiliteCurrent;
  private JTextField historySizeField;
  private JLabel historySizeLabel;
  private JCheckBox includeFilesInHistory;
  private JComboBox internalLineEnding;
  private JLabel internalLineEndingLabel;
  private JLabel jLabel1;
  private JLabel jLabel2;
  private JPanel jPanel1;
  private JPanel jPanel2;
  private JCheckBox keepHilite;
  private JTextField noWordSep;
  private JLabel noWordSepLabel;
  private JCheckBox rightClickMovesCursor;
  private JTextField tabSize;
  private JCheckBox useTabs;
  // End of variables declaration//GEN-END:variables

}
