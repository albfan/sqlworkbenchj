/*
 * WbFormatterOptionsPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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

import java.awt.event.ActionListener;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;

import workbench.interfaces.Restoreable;
import workbench.resource.GeneratedIdentifierCase;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;

import workbench.sql.formatter.JoinWrapStyle;

import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class WbFormatterOptionsPanel
	extends JPanel
	implements Restoreable, ActionListener
{
	public WbFormatterOptionsPanel()
	{
		super();
		initComponents();
		ComboBoxModel model = new DefaultComboBoxModel(JoinWrapStyle.values());
		joinWrappingStyle.setModel(model);
		ComboBoxModel caseModel = new DefaultComboBoxModel(GeneratedIdentifierCase.values());
		keywordCase.setModel(caseModel);
		ComboBoxModel idModel = new DefaultComboBoxModel(GeneratedIdentifierCase.values());
		identifierCase.setModel(idModel);
		ComboBoxModel funcModel = new DefaultComboBoxModel(GeneratedIdentifierCase.values());
		functionCase.setModel(funcModel);
		ComboBoxModel typeModel = new DefaultComboBoxModel(GeneratedIdentifierCase.values());
		dataTypeCase.setModel(typeModel);
		String mnu = ResourceMgr.getPlainString("MnuTxtMakeCharInList");
		String lbl = ResourceMgr.getFormattedString("LblMaxElements", mnu);
		jLabel1.setText(lbl);
		WbSwingUtilities.setMinimumSizeFromCols(subselectMaxLength);
		WbSwingUtilities.setMinimumSizeFromCols(insertColumns);
		WbSwingUtilities.setMinimumSizeFromCols(updateColumns);
		WbSwingUtilities.setMinimumSizeFromCols(selectColumns);
		WbSwingUtilities.setMinimumSizeFromCols(maxCharElements);
		WbSwingUtilities.setMinimumSizeFromCols(maxNumElements);
	}

	@Override
	public void restoreSettings()
	{

		functionCase.setSelectedItem(Settings.getInstance().getFormatterFunctionCase());
    dataTypeCase.setSelectedItem(Settings.getInstance().getFormatterDatatypeCase());
		insertColumns.setText(Integer.toString(Settings.getInstance().getFormatterMaxColumnsInInsert()));
		updateColumns.setText(Integer.toString(Settings.getInstance().getFormatterMaxColumnsInUpdate()));
		keywordCase.setSelectedItem(Settings.getInstance().getFormatterKeywordsCase());
		identifierCase.setSelectedItem(Settings.getInstance().getFormatterIdentifierCase());
		spaceAfterComma.setSelected(Settings.getInstance().getFormatterAddSpaceAfterComma());
		commaAfterLineBreak.setSelected(Settings.getInstance().getFormatterCommaAfterLineBreak());
		addSpaceAfterLineBreakComma.setSelected(Settings.getInstance().getFormatterAddSpaceAfterLineBreakComma());
		addSpaceAfterLineBreakComma.setEnabled(commaAfterLineBreak.isSelected());
		insertWithColumnNames.setSelected(Settings.getInstance().getFormatterAddColumnNameComment());
		JoinWrapStyle style = Settings.getInstance().getFormatterJoinWrapStyle();
		joinWrappingStyle.setSelectedItem(style);
		selectColumns.setText(Integer.toString(Settings.getInstance().getFormatterMaxColumnsInSelect()));
		subselectMaxLength.setText(Integer.toString(Settings.getInstance().getFormatterMaxSubselectLength()));
		maxCharElements.setText(Integer.toString(Settings.getInstance().getMaxCharInListElements()));
		maxNumElements.setText(Integer.toString(Settings.getInstance().getMaxNumInListElements()));
	}

	@Override
	public void saveSettings()
	{
		Settings set = Settings.getInstance();
		set.setMaxNumInListElements(StringUtil.getIntValue(maxNumElements.getText(),-1));
		set.setMaxCharInListElements(StringUtil.getIntValue(maxCharElements.getText(),-1));
		set.setFormatterMaxSubselectLength(StringUtil.getIntValue(subselectMaxLength.getText(),60));
		set.setFormatterMaxColumnsInSelect(StringUtil.getIntValue(selectColumns.getText(),1));
		set.setFormatterFunctionCase((GeneratedIdentifierCase)functionCase.getSelectedItem());
		set.setFormatterMaxColumnsInInsert(StringUtil.getIntValue(insertColumns.getText(),1));
		set.setFormatterMaxColumnsInUpdate(StringUtil.getIntValue(updateColumns.getText(),1));
		set.setFormatterKeywordsCase((GeneratedIdentifierCase)keywordCase.getSelectedItem());
		set.setFormatterIdentifierCase((GeneratedIdentifierCase)identifierCase.getSelectedItem());
		set.setFormatterDatatypeCase((GeneratedIdentifierCase)dataTypeCase.getSelectedItem());
		set.setFormatterAddSpaceAfterComma(spaceAfterComma.isSelected());
		set.setFormatterCommaAfterLineBreak(commaAfterLineBreak.isSelected());
		set.setFormatterAddSpaceAfterLineBreakComma(addSpaceAfterLineBreakComma.isSelected());
		set.setFormatterAddColumnNameComment(insertWithColumnNames.isSelected());

		JoinWrapStyle style = (JoinWrapStyle)joinWrappingStyle.getSelectedItem();
		set.setFormatterJoinWrapStyle(style);
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    spaceAfterComma = new javax.swing.JCheckBox();
    addSpaceAfterLineBreakComma = new javax.swing.JCheckBox();
    commaAfterLineBreak = new javax.swing.JCheckBox();
    joinWrappingStyle = new javax.swing.JComboBox();
    insertWithColumnNames = new javax.swing.JCheckBox();
    jLabel2 = new javax.swing.JLabel();
    jLabel3 = new javax.swing.JLabel();
    jSeparator1 = new javax.swing.JSeparator();
    jSeparator2 = new javax.swing.JSeparator();
    jSeparator3 = new javax.swing.JSeparator();
    jPanel1 = new javax.swing.JPanel();
    subselectMaxLabel = new javax.swing.JLabel();
    subselectMaxLength = new javax.swing.JTextField();
    jPanel2 = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    maxCharElementsLabel = new javax.swing.JLabel();
    maxCharElements = new javax.swing.JTextField();
    maxNumElementsLabel = new javax.swing.JLabel();
    maxNumElements = new javax.swing.JTextField();
    jSeparator4 = new javax.swing.JSeparator();
    colsPerLinePanel = new javax.swing.JPanel();
    selectColumnsLabel = new javax.swing.JLabel();
    selectColumns = new javax.swing.JTextField();
    insertColumnsLabel = new javax.swing.JLabel();
    insertColumns = new javax.swing.JTextField();
    updateColumnsLabel = new javax.swing.JLabel();
    updateColumns = new javax.swing.JTextField();
    jPanel3 = new javax.swing.JPanel();
    keywordCaseLabel = new javax.swing.JLabel();
    keywordCase = new javax.swing.JComboBox();
    functionCase = new javax.swing.JComboBox();
    functionCaseLabel = new javax.swing.JLabel();
    identifierCaseLabel = new javax.swing.JLabel();
    identifierCase = new javax.swing.JComboBox();
    dataTypeCaseLabel = new javax.swing.JLabel();
    dataTypeCase = new javax.swing.JComboBox();

    setLayout(new java.awt.GridBagLayout());

    spaceAfterComma.setText(ResourceMgr.getString("LblSpaceAfterComma")); // NOI18N
    spaceAfterComma.setToolTipText(ResourceMgr.getString("d_LblSpaceAfterComma")); // NOI18N
    spaceAfterComma.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(spaceAfterComma, gridBagConstraints);

    addSpaceAfterLineBreakComma.setText(ResourceMgr.getString("LblSpaceAfterLineBreakComma")); // NOI18N
    addSpaceAfterLineBreakComma.setToolTipText(ResourceMgr.getString("d_LblSpaceAfterLineBreakComma")); // NOI18N
    addSpaceAfterLineBreakComma.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 19, 0, 0);
    add(addSpaceAfterLineBreakComma, gridBagConstraints);

    commaAfterLineBreak.setText(ResourceMgr.getString("LblCommaAfterLineBreak")); // NOI18N
    commaAfterLineBreak.setToolTipText(ResourceMgr.getString("d_LblCommaAfterLineBreak")); // NOI18N
    commaAfterLineBreak.setBorder(null);
    commaAfterLineBreak.addActionListener(this);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(commaAfterLineBreak, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 15);
    add(joinWrappingStyle, gridBagConstraints);

    insertWithColumnNames.setText(ResourceMgr.getString("LblColNameComment")); // NOI18N
    insertWithColumnNames.setToolTipText(ResourceMgr.getString("d_LblColNameComment")); // NOI18N
    insertWithColumnNames.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(insertWithColumnNames, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblJoinWrapStyle")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(jLabel2, gridBagConstraints);

    jLabel3.setText(ResourceMgr.getString("LblMaxCols")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(9, 2, 0, 0);
    add(jLabel3, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    add(jSeparator1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    add(jSeparator2, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(11, 0, 4, 0);
    add(jSeparator3, gridBagConstraints);

    jPanel1.setLayout(new java.awt.GridBagLayout());

    subselectMaxLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    subselectMaxLabel.setText(ResourceMgr.getString("LblMaxSub")); // NOI18N
    subselectMaxLabel.setToolTipText(ResourceMgr.getDescription("LblMaxSub"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.ipadx = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel1.add(subselectMaxLabel, gridBagConstraints);

    subselectMaxLength.setColumns(6);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(1, 9, 0, 8);
    jPanel1.add(subselectMaxLength, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    add(jPanel1, gridBagConstraints);

    jPanel2.setLayout(new java.awt.GridBagLayout());

    jLabel1.setText(ResourceMgr.getString("LblMaxElements")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    jPanel2.add(jLabel1, gridBagConstraints);

    maxCharElementsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    maxCharElementsLabel.setText(ResourceMgr.getString("LblMaxCharEle")); // NOI18N
    maxCharElementsLabel.setToolTipText(ResourceMgr.getString("d_LblMaxCharEle")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 10, 0, 0);
    jPanel2.add(maxCharElementsLabel, gridBagConstraints);

    maxCharElements.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 4, 0, 7);
    jPanel2.add(maxCharElements, gridBagConstraints);

    maxNumElementsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    maxNumElementsLabel.setText(ResourceMgr.getString("LblMaxNumEle")); // NOI18N
    maxNumElementsLabel.setToolTipText(ResourceMgr.getDescription("LblMaxNumEle"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 10, 0, 0);
    jPanel2.add(maxNumElementsLabel, gridBagConstraints);

    maxNumElements.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 4, 0, 15);
    jPanel2.add(maxNumElements, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 0, 0);
    add(jPanel2, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    add(jSeparator4, gridBagConstraints);

    colsPerLinePanel.setLayout(new java.awt.GridBagLayout());

    selectColumnsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    selectColumnsLabel.setText(ResourceMgr.getString("LblSqlFmtColNum")); // NOI18N
    selectColumnsLabel.setToolTipText(ResourceMgr.getDescription("LblSqlFmtColNum"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    colsPerLinePanel.add(selectColumnsLabel, gridBagConstraints);

    selectColumns.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    colsPerLinePanel.add(selectColumns, gridBagConstraints);

    insertColumnsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    insertColumnsLabel.setText(ResourceMgr.getString("LblSqlFmtColNumIns")); // NOI18N
    insertColumnsLabel.setToolTipText(ResourceMgr.getDescription("LblSqlFmtColNumIns"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    colsPerLinePanel.add(insertColumnsLabel, gridBagConstraints);

    insertColumns.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    colsPerLinePanel.add(insertColumns, gridBagConstraints);

    updateColumnsLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    updateColumnsLabel.setText(ResourceMgr.getString("LblSqlFmtColNumUpd")); // NOI18N
    updateColumnsLabel.setToolTipText(ResourceMgr.getDescription("LblSqlFmtColNumUpd"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
    colsPerLinePanel.add(updateColumnsLabel, gridBagConstraints);

    updateColumns.setColumns(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
    colsPerLinePanel.add(updateColumns, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 9, 0, 0);
    add(colsPerLinePanel, gridBagConstraints);

    jPanel3.setLayout(new java.awt.GridBagLayout());

    keywordCaseLabel.setLabelFor(keywordCase);
    keywordCaseLabel.setText(ResourceMgr.getString("LblFmtKeyWord")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 0);
    jPanel3.add(keywordCaseLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 0);
    jPanel3.add(keywordCase, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 0);
    jPanel3.add(functionCase, gridBagConstraints);

    functionCaseLabel.setLabelFor(functionCase);
    functionCaseLabel.setText(ResourceMgr.getString("LblFmtFuncLower")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 10, 0, 0);
    jPanel3.add(functionCaseLabel, gridBagConstraints);

    identifierCaseLabel.setLabelFor(identifierCase);
    identifierCaseLabel.setText(ResourceMgr.getString("LblFmtIdentifiers")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 24, 0, 0);
    jPanel3.add(identifierCaseLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 15);
    jPanel3.add(identifierCase, gridBagConstraints);

    dataTypeCaseLabel.setLabelFor(identifierCase);
    dataTypeCaseLabel.setText(ResourceMgr.getString("LblFmtDataType")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 24, 0, 0);
    jPanel3.add(dataTypeCaseLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(8, 9, 0, 15);
    jPanel3.add(dataTypeCase, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    add(jPanel3, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void actionPerformed(java.awt.event.ActionEvent evt)
  {
    if (evt.getSource() == commaAfterLineBreak)
    {
      WbFormatterOptionsPanel.this.commaAfterLineBreakActionPerformed(evt);
    }
  }// </editor-fold>//GEN-END:initComponents

	private void commaAfterLineBreakActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_commaAfterLineBreakActionPerformed
	{//GEN-HEADEREND:event_commaAfterLineBreakActionPerformed
		addSpaceAfterLineBreakComma.setEnabled(commaAfterLineBreak.isSelected());
	}//GEN-LAST:event_commaAfterLineBreakActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox addSpaceAfterLineBreakComma;
  private javax.swing.JPanel colsPerLinePanel;
  private javax.swing.JCheckBox commaAfterLineBreak;
  private javax.swing.JComboBox dataTypeCase;
  private javax.swing.JLabel dataTypeCaseLabel;
  private javax.swing.JComboBox functionCase;
  private javax.swing.JLabel functionCaseLabel;
  private javax.swing.JComboBox identifierCase;
  private javax.swing.JLabel identifierCaseLabel;
  private javax.swing.JTextField insertColumns;
  private javax.swing.JLabel insertColumnsLabel;
  private javax.swing.JCheckBox insertWithColumnNames;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JPanel jPanel3;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JSeparator jSeparator2;
  private javax.swing.JSeparator jSeparator3;
  private javax.swing.JSeparator jSeparator4;
  private javax.swing.JComboBox joinWrappingStyle;
  private javax.swing.JComboBox keywordCase;
  private javax.swing.JLabel keywordCaseLabel;
  private javax.swing.JTextField maxCharElements;
  private javax.swing.JLabel maxCharElementsLabel;
  private javax.swing.JTextField maxNumElements;
  private javax.swing.JLabel maxNumElementsLabel;
  private javax.swing.JTextField selectColumns;
  private javax.swing.JLabel selectColumnsLabel;
  private javax.swing.JCheckBox spaceAfterComma;
  private javax.swing.JLabel subselectMaxLabel;
  private javax.swing.JTextField subselectMaxLength;
  private javax.swing.JTextField updateColumns;
  private javax.swing.JLabel updateColumnsLabel;
  // End of variables declaration//GEN-END:variables

}
