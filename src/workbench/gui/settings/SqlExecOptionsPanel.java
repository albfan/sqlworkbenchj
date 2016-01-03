/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
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

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import workbench.interfaces.Restoreable;
import workbench.resource.ErrorPromptType;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.components.TextFieldWidthAdjuster;
import workbench.gui.help.HelpManager;


/**
 *
 * @author  Thomas Kellerer
 */
public class SqlExecOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener, ItemListener
{

	public SqlExecOptionsPanel()
	{
		super();
		initComponents();
		TextFieldWidthAdjuster adjuster = new TextFieldWidthAdjuster();
		adjuster.adjustAllFields(this);

	}

	@Override
	public void restoreSettings()
	{
		String[] items = new String[] {
			ResourceMgr.getString("LblErrPromptSimplePrompt"),
			ResourceMgr.getString("LblErrPromptPromptWithErroressage"),
			ResourceMgr.getString("LblErrPromptPromptWithRetry")
		};

    promptType.setModel(new DefaultComboBoxModel(items));

		DbDelimiter[] names = DbDelimiter.getMapping();
		cbxDbName.setModel(new DefaultComboBoxModel(names));

		DbDelimiter def = (DbDelimiter)cbxDbName.getSelectedItem();
		alternateDelimiter.setText(def.getDelimiter());

    String text = altDelimLabel.getText();
    altDelimLabel.setText("<html><u>" + text + "</u></html>");
    altDelimLabel.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent e)
      {
        HelpManager.showHelpFile(HelpManager.TOPIC_ALTERNATE_DELIMITER);
      }
    });

		useCurrentLineStmt.setSelected(GuiSettings.getUseStatementInCurrentLine());

		keepHilite.setSelected(GuiSettings.getKeepCurrentSqlHighlight());
		hiliteCurrent.setSelected(Settings.getInstance().getHighlightCurrentStatement());
		alwaysAllowExecSel.setSelected(!GuiSettings.getExecuteOnlySelected());
		allowEditDuringExec.setSelected(!GuiSettings.getDisableEditorDuringExecution());
		emptyLineDelimiter.setSelected(Settings.getInstance().getEmptyLineIsDelimiter());
		hiliteError.setSelected(GuiSettings.getHighlightErrorStatement());
    jumpToError.setSelected(GuiSettings.jumpToError());
    showStmtEndTime.setSelected(GuiSettings.showScriptStmtFinishTime());
    showScriptEndTime.setSelected(GuiSettings.showScriptFinishTime());
    ErrorPromptType type = GuiSettings.getErrorPromptType();
    promptType.setSelectedIndex(promptTypeToIndex(type));
    setTypeTooltip();
	}

	@Override
	public void saveSettings()
	{
		Settings set = Settings.getInstance();

		// Synchronize current text with the corresponding item in the dropdown
		DbDelimiter delim = (DbDelimiter)cbxDbName.getSelectedItem();
		delim.setDelimiter(alternateDelimiter.getText());

		DbDelimiter defDelim = (DbDelimiter)cbxDbName.getItemAt(0);
		set.setAlternateDelimiter(defDelim.getDelimiter());

		for (int i=1; i < cbxDbName.getItemCount(); i++)
		{
			DbDelimiter dbDelim = (DbDelimiter)cbxDbName.getItemAt(i);
			set.setDbDelimiter(dbDelim.getDbid(), dbDelim.getDelimiter());
		}

		set.setAutoJumpNextStatement(this.autoAdvance.isSelected());
		set.setProperty(Settings.PROPERTY_HIGHLIGHT_CURRENT_STATEMENT, hiliteCurrent.isSelected());
		set.setEmptyLineIsDelimiter(emptyLineDelimiter.isSelected());
		GuiSettings.setKeepCurrentSqlHighlight(keepHilite.isSelected());
		GuiSettings.setExecuteOnlySelected(!alwaysAllowExecSel.isSelected());
		GuiSettings.setDisableEditorDuringExecution(!allowEditDuringExec.isSelected());
		GuiSettings.setHighlightErrorStatement(hiliteError.isSelected());
		GuiSettings.setUseStatementInCurrentLine(useCurrentLineStmt.isSelected());
    int index = promptType.getSelectedIndex();
    GuiSettings.setErrorPromptType(indexToPromptType(index));
    GuiSettings.setJumpToError(jumpToError.isSelected());
    GuiSettings.setShowScriptFinishTime(showScriptEndTime.isSelected());
    GuiSettings.setShowScriptStmtFinishTime(showStmtEndTime.isSelected());
	}

  private void setTypeTooltip()
  {
    int index = promptType.getSelectedIndex();
    ErrorPromptType type = indexToPromptType(index);
    String key = "LblErrPrompt" + type.name();
    String desc = ResourceMgr.getDescription(key);
    promptType.setToolTipText(desc);
  }

	private ErrorPromptType indexToPromptType(int index)
	{
    switch (index)
    {
      case 0:
        return ErrorPromptType.SimplePrompt;
      case 2:
        return ErrorPromptType.PromptWithRetry;
      default:
        return ErrorPromptType.PromptWithErroressage;
    }
	}

	private int promptTypeToIndex(ErrorPromptType value)
	{
    switch (value)
    {
      case SimplePrompt:
        return 0;
      case PromptWithErroressage:
        return 1;
      case PromptWithRetry:
        return 2;
    }
    return 1;
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

    altDelimLabel = new JLabel();
    jPanel2 = new JPanel();
    hiliteCurrent = new JCheckBox();
    keepHilite = new JCheckBox();
    allowEditDuringExec = new JCheckBox();
    alwaysAllowExecSel = new JCheckBox();
    autoAdvance = new JCheckBox();
    emptyLineDelimiter = new JCheckBox();
    hiliteError = new JCheckBox();
    useCurrentLineStmt = new JCheckBox();
    jumpToError = new JCheckBox();
    showStmtEndTime = new JCheckBox();
    showScriptEndTime = new JCheckBox();
    jPanel3 = new JPanel();
    alternateDelimiter = new JTextField();
    cbxDbName = new JComboBox();
    jLabel1 = new JLabel();
    promptType = new JComboBox();

    setLayout(new GridBagLayout());

    altDelimLabel.setText(ResourceMgr.getString("LblAltDelimit")); // NOI18N
    altDelimLabel.setToolTipText(ResourceMgr.getString("d_LblAltDelimit")); // NOI18N
    altDelimLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(8, 0, 0, 0);
    add(altDelimLabel, gridBagConstraints);

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
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(7, 15, 0, 0);
    jPanel2.add(allowEditDuringExec, gridBagConstraints);

    alwaysAllowExecSel.setText(ResourceMgr.getString("LblExecSelOnly")); // NOI18N
    alwaysAllowExecSel.setToolTipText(ResourceMgr.getString("d_LblExecSelOnly")); // NOI18N
    alwaysAllowExecSel.setBorder(null);
    alwaysAllowExecSel.setHorizontalAlignment(SwingConstants.LEFT);
    alwaysAllowExecSel.setHorizontalTextPosition(SwingConstants.RIGHT);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
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
    gridBagConstraints.gridy = 1;
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
    gridBagConstraints.gridy = 2;
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

    useCurrentLineStmt.setText(ResourceMgr.getString("LblUseStmtInCurLine")); // NOI18N
    useCurrentLineStmt.setToolTipText(ResourceMgr.getString("d_LblUseStmtInCurLine")); // NOI18N
    useCurrentLineStmt.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new Insets(7, 15, 0, 0);
    jPanel2.add(useCurrentLineStmt, gridBagConstraints);

    jumpToError.setText(ResourceMgr.getString("LblJumpToError")); // NOI18N
    jumpToError.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(jumpToError, gridBagConstraints);

    showStmtEndTime.setText(ResourceMgr.getString("LblShowStmtEndTime")); // NOI18N
    showStmtEndTime.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(showStmtEndTime, gridBagConstraints);

    showScriptEndTime.setText(ResourceMgr.getString("LblShowScriptEndTime")); // NOI18N
    showScriptEndTime.setBorder(null);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(7, 0, 0, 0);
    jPanel2.add(showScriptEndTime, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(13, 0, 0, 0);
    add(jPanel2, gridBagConstraints);

    jPanel3.setLayout(new GridBagLayout());

    alternateDelimiter.setColumns(10);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new Insets(0, 7, 0, 15);
    jPanel3.add(alternateDelimiter, gridBagConstraints);

    cbxDbName.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    cbxDbName.addItemListener(this);
    cbxDbName.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    jPanel3.add(cbxDbName, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new Insets(6, 11, 0, 0);
    add(jPanel3, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblErrPromptType")); // NOI18N
    jLabel1.setToolTipText(ResourceMgr.getString("d_LblErrPromptType")); // NOI18N
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.insets = new Insets(3, 0, 0, 0);
    add(jLabel1, gridBagConstraints);

    promptType.setModel(new DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    promptType.addActionListener(this);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.insets = new Insets(1, 11, 0, 0);
    add(promptType, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(ActionEvent evt)
  {
    if (evt.getSource() == cbxDbName)
    {
      SqlExecOptionsPanel.this.cbxDbNameActionPerformed(evt);
    }
    else if (evt.getSource() == promptType)
    {
      SqlExecOptionsPanel.this.promptTypeActionPerformed(evt);
    }
  }

  public void itemStateChanged(ItemEvent evt)
  {
    if (evt.getSource() == cbxDbName)
    {
      SqlExecOptionsPanel.this.cbxDbNameItemStateChanged(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

  private void cbxDbNameActionPerformed(ActionEvent evt)//GEN-FIRST:event_cbxDbNameActionPerformed
  {//GEN-HEADEREND:event_cbxDbNameActionPerformed
		DbDelimiter delim = (DbDelimiter)cbxDbName.getSelectedItem();
		alternateDelimiter.setText(delim.getDelimiter());
  }//GEN-LAST:event_cbxDbNameActionPerformed

  private void cbxDbNameItemStateChanged(ItemEvent evt)//GEN-FIRST:event_cbxDbNameItemStateChanged
  {//GEN-HEADEREND:event_cbxDbNameItemStateChanged
		if (evt.getStateChange() == ItemEvent.DESELECTED)
		{
			DbDelimiter def = (DbDelimiter)evt.getItem();
			def.setDelimiter(alternateDelimiter.getText());
		}
  }//GEN-LAST:event_cbxDbNameItemStateChanged

  private void promptTypeActionPerformed(ActionEvent evt)//GEN-FIRST:event_promptTypeActionPerformed
  {//GEN-HEADEREND:event_promptTypeActionPerformed
    setTypeTooltip();
  }//GEN-LAST:event_promptTypeActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private JCheckBox allowEditDuringExec;
  private JLabel altDelimLabel;
  private JTextField alternateDelimiter;
  private JCheckBox alwaysAllowExecSel;
  private JCheckBox autoAdvance;
  private JComboBox cbxDbName;
  private JCheckBox emptyLineDelimiter;
  private JCheckBox hiliteCurrent;
  private JCheckBox hiliteError;
  private JLabel jLabel1;
  private JPanel jPanel2;
  private JPanel jPanel3;
  private JCheckBox jumpToError;
  private JCheckBox keepHilite;
  private JComboBox promptType;
  private JCheckBox showScriptEndTime;
  private JCheckBox showStmtEndTime;
  private JCheckBox useCurrentLineStmt;
  // End of variables declaration//GEN-END:variables

}
