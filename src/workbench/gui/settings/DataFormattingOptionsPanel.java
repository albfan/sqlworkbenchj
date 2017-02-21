/*
 * DataFormattingOptionsPanel.java
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

import java.awt.event.MouseListener;

import javax.swing.JPanel;

import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.NumberField;
import workbench.gui.help.HelpManager;

import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class DataFormattingOptionsPanel
	extends JPanel
	implements Restoreable, ValidatingComponent, MouseListener
{
	public DataFormattingOptionsPanel()
	{
		super();
		initComponents();
    WbSwingUtilities.setMinimumSize(decimalField, 5);
    WbSwingUtilities.setMinimumSize(groupSeparator, 5);
    WbSwingUtilities.setMinimumSize(maxDigitsField, 5);
    WbSwingUtilities.setMinimumSize(intFormat, 25);
    WbSwingUtilities.setMinimumSize(decimalFormat, 25);
    WbSwingUtilities.setMinimumSize(timeFormat, 25);
    WbSwingUtilities.setMinimumSize(timestampFormatTextField, 25);
    WbSwingUtilities.setMinimumSize(dateFormatTextField, 25);
	}

	@Override
	public void restoreSettings()
	{
		dateFormatTextField.setText(Settings.getInstance().getDefaultDateFormat());
		dateFormatTextField.setCaretPosition(0);
		timestampFormatTextField.setText(Settings.getInstance().getDefaultTimestampFormat());
		timestampFormatTextField.setCaretPosition(0);
		decimalField.setText(Settings.getInstance().getDecimalSymbol());
		timeFormat.setText(Settings.getInstance().getDefaultTimeFormat());
		maxDigitsField.setText(Integer.toString(Settings.getInstance().getMaxFractionDigits()));
		oraDateFix.setSelected(Settings.getInstance().fixOracleDateType());
    groupSeparator.setText(Settings.getInstance().getDecimalGroupCharacter());
    intFormat.setText(Settings.getInstance().getIntegerFormatString());
    decimalFormat.setText(Settings.getInstance().getDecimalFormatString());
	}

	@Override
	public void saveSettings()
	{
		Settings.getInstance().setDefaultDateFormat(this.dateFormatTextField.getText());
		Settings.getInstance().setDefaultTimeFormat(this.timeFormat.getText());
		Settings.getInstance().setDefaultTimestampFormat(this.timestampFormatTextField.getText());
		Settings.getInstance().setMaxFractionDigits(((NumberField)this.maxDigitsField).getValue());
    Settings.getInstance().setDecimalSymbol(StringUtil.trimToNull(this.decimalField.getText()));
		Settings.getInstance().setDecimalGroupCharacter(groupSeparator.getText());
    Settings.getInstance().setDecimalFormatString(StringUtil.trimToNull(decimalFormat.getText()));
    Settings.getInstance().setIntegerFormatString(StringUtil.trimToNull(intFormat.getText()));
		Settings.getInstance().setFixOracleDateType(oraDateFix.isSelected());
	}


	@Override
	public boolean validateInput()
	{
		String format = dateFormatTextField.getText();

		if (StringUtil.isNonBlank(format))
		{
			String err = StringUtil.isDatePatternValid(format);
			if (err != null)
			{
				String msg = ResourceMgr.getFormattedString("ErrInvalidInput", dateFormatLabel.getText(), err);
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("TxtError"), msg);
				return false;
			}
		}

		format = timestampFormatTextField.getText();
		if (StringUtil.isNonBlank(format))
		{
			String err = StringUtil.isDatePatternValid(format);
			if (err != null)
			{
				String msg = ResourceMgr.getFormattedString("ErrInvalidInput", timestampFormatLabel.getText(), err);
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("TxtError"), msg);
				return false;
			}
		}

		format = timeFormat.getText();
		if (StringUtil.isNonBlank(format))
		{
			String err = StringUtil.isDatePatternValid(format);
			if (err != null)
			{
				String msg = ResourceMgr.getFormattedString("ErrInvalidInput", timeFormatLabel.getText(), err);
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("TxtError"), msg);
				return false;
			}
		}

		return true;
	}

  @Override
  public void componentWillBeClosed()
  {
		// nothing to do
  }

	@Override
	public void componentDisplayed()
	{
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

    dateFormatLabel = new javax.swing.JLabel();
    dateFormatTextField = new javax.swing.JTextField();
    timestampFormatLabel = new javax.swing.JLabel();
    timestampFormatTextField = new javax.swing.JTextField();
    timeFormatLabel = new javax.swing.JLabel();
    timeFormat = new javax.swing.JTextField();
    oraDateFix = new javax.swing.JCheckBox();
    helpLabel = new javax.swing.JLabel();
    jSeparator1 = new javax.swing.JSeparator();
    jPanel2 = new javax.swing.JPanel();
    decimalLabel = new javax.swing.JLabel();
    decimalField = new javax.swing.JTextField();
    groupSeparator = new javax.swing.JTextField();
    groupLabel = new javax.swing.JLabel();
    maxDigitsLabel = new javax.swing.JLabel();
    intFormatLabel = new javax.swing.JLabel();
    decimalFormatLabel = new javax.swing.JLabel();
    decimalFormat = new javax.swing.JTextField();
    intFormat = new javax.swing.JTextField();
    maxDigitsField = new NumberField();
    numberFormatHelp = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    dateFormatLabel.setText(ResourceMgr.getString("LblDateFormat")); // NOI18N
    dateFormatLabel.setToolTipText(ResourceMgr.getString("d_LblDateFormat")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    add(dateFormatLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 7, 0, 11);
    add(dateFormatTextField, gridBagConstraints);

    timestampFormatLabel.setText(ResourceMgr.getString("LblTimestampFormat")); // NOI18N
    timestampFormatLabel.setToolTipText(ResourceMgr.getDescription("LblTimestampFormat"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(timestampFormatLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 7, 0, 11);
    add(timestampFormatTextField, gridBagConstraints);

    timeFormatLabel.setText(ResourceMgr.getString("LblTimeFormat")); // NOI18N
    timeFormatLabel.setToolTipText(ResourceMgr.getDescription("LblTimeFormat"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    add(timeFormatLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(7, 7, 0, 11);
    add(timeFormat, gridBagConstraints);

    oraDateFix.setText(ResourceMgr.getString("LblOraDataTS")); // NOI18N
    oraDateFix.setToolTipText(ResourceMgr.getString("d_LblOraDataTS")); // NOI18N
    oraDateFix.setBorder(null);
    oraDateFix.setDoubleBuffered(true);
    oraDateFix.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    oraDateFix.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
    oraDateFix.setIconTextGap(5);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
    add(oraDateFix, gridBagConstraints);

    helpLabel.setText(ResourceMgr.getString("LblFmtHelp")); // NOI18N
    helpLabel.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    helpLabel.addMouseListener(this);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
    add(helpLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(11, 0, 8, 0);
    add(jSeparator1, gridBagConstraints);

    jPanel2.setLayout(new java.awt.GridBagLayout());

    decimalLabel.setText(ResourceMgr.getString("LblDecimalSymbol")); // NOI18N
    decimalLabel.setToolTipText(ResourceMgr.getDescription("LblDecimalSymbol"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(9, 0, 0, 0);
    jPanel2.add(decimalLabel, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 7, 0, 9);
    jPanel2.add(decimalField, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 7, 0, 9);
    jPanel2.add(groupSeparator, gridBagConstraints);

    groupLabel.setText(ResourceMgr.getString("LblDecimalGroup")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    jPanel2.add(groupLabel, gridBagConstraints);

    maxDigitsLabel.setText(ResourceMgr.getString("LblMaxDigits")); // NOI18N
    maxDigitsLabel.setToolTipText(ResourceMgr.getDescription("LblMaxDigits"));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    jPanel2.add(maxDigitsLabel, gridBagConstraints);

    intFormatLabel.setText(ResourceMgr.getString("LblIntegerFormat")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    jPanel2.add(intFormatLabel, gridBagConstraints);

    decimalFormatLabel.setText(ResourceMgr.getString("LblDecimalFormat")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
    jPanel2.add(decimalFormatLabel, gridBagConstraints);

    decimalFormat.setToolTipText(ResourceMgr.getString("d_LblDecimalFormat")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 7, 0, 9);
    jPanel2.add(decimalFormat, gridBagConstraints);

    intFormat.setToolTipText(ResourceMgr.getString("d_LblIntegerFormat")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 7, 0, 9);
    jPanel2.add(intFormat, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.insets = new java.awt.Insets(6, 7, 0, 9);
    jPanel2.add(maxDigitsField, gridBagConstraints);

    numberFormatHelp.setText(ResourceMgr.getString("LblFmtHelp")); // NOI18N
    numberFormatHelp.addMouseListener(this);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
    jPanel2.add(numberFormatHelp, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 56);
    add(jPanel2, gridBagConstraints);
  }

  // Code for dispatching events from components to event handlers.

  public void mouseClicked(java.awt.event.MouseEvent evt)
  {
    if (evt.getSource() == helpLabel)
    {
      DataFormattingOptionsPanel.this.helpLabelMouseClicked(evt);
    }
    else if (evt.getSource() == numberFormatHelp)
    {
      DataFormattingOptionsPanel.this.numberFormatHelpMouseClicked(evt);
    }
  }

  public void mouseEntered(java.awt.event.MouseEvent evt)
  {
  }

  public void mouseExited(java.awt.event.MouseEvent evt)
  {
  }

  public void mousePressed(java.awt.event.MouseEvent evt)
  {
  }

  public void mouseReleased(java.awt.event.MouseEvent evt)
  {
  }// </editor-fold>//GEN-END:initComponents

  private void helpLabelMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_helpLabelMouseClicked
  {//GEN-HEADEREND:event_helpLabelMouseClicked
    HelpManager.showDateFormatHelp();
  }//GEN-LAST:event_helpLabelMouseClicked

  private void numberFormatHelpMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_numberFormatHelpMouseClicked
  {//GEN-HEADEREND:event_numberFormatHelpMouseClicked
    HelpManager.showNumberFormatHelp();
  }//GEN-LAST:event_numberFormatHelpMouseClicked


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JLabel dateFormatLabel;
  private javax.swing.JTextField dateFormatTextField;
  private javax.swing.JTextField decimalField;
  private javax.swing.JTextField decimalFormat;
  private javax.swing.JLabel decimalFormatLabel;
  private javax.swing.JLabel decimalLabel;
  private javax.swing.JLabel groupLabel;
  private javax.swing.JTextField groupSeparator;
  private javax.swing.JLabel helpLabel;
  private javax.swing.JTextField intFormat;
  private javax.swing.JLabel intFormatLabel;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JTextField maxDigitsField;
  private javax.swing.JLabel maxDigitsLabel;
  private javax.swing.JLabel numberFormatHelp;
  private javax.swing.JCheckBox oraDateFix;
  private javax.swing.JTextField timeFormat;
  private javax.swing.JLabel timeFormatLabel;
  private javax.swing.JLabel timestampFormatLabel;
  private javax.swing.JTextField timestampFormatTextField;
  // End of variables declaration//GEN-END:variables

}
