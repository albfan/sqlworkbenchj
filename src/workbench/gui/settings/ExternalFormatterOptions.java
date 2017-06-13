/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.settings;

import java.util.Arrays;
import java.util.Map;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPanel;

import workbench.WbManager;
import workbench.interfaces.MainPanel;
import workbench.interfaces.Restoreable;
import workbench.interfaces.ValidatingComponent;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.WbConnection;

import workbench.gui.MainWindow;
import workbench.gui.components.WbFilePicker;

import workbench.sql.formatter.ExternalFormatter;

/**
 *
 * @author Thomas Kellerer
 */
public class ExternalFormatterOptions
  extends JPanel
  implements Restoreable, ValidatingComponent
{
  private ExternalFormatter currentFormatter;

  public ExternalFormatterOptions()
  {
    initComponents();
    ((WbFilePicker)exePath).setLastDirProperty("workbench.gui.formatter.lastexe.dir");
    lblHelp.setText(ResourceMgr.getFormattedString("MsgExtFmtHelp", ExternalFormatter.INPUT_FILE, ExternalFormatter.OUTPUT_FILE));
  }

  private void fillDropDown()
  {
    Map<String, String> map = Settings.getInstance().getDbIdMapping();
    FormatterEntry[] ids = new FormatterEntry[map.size() + 1];

    FormatterEntry def = new FormatterEntry();
    def.dbid = ExternalFormatter.DEFAULT_DBID;
    def.name = "Default";
    def.formatter = ExternalFormatter.getDefinition(ExternalFormatter.DEFAULT_DBID);
    ids[0] = def;

    int i = 1;
    for (Map.Entry<String, String> entry : map.entrySet())
    {
      FormatterEntry fdef = new FormatterEntry();
      fdef.dbid = entry.getKey();
      fdef.name = entry.getValue();
      fdef.formatter = ExternalFormatter.getDefinition(fdef.dbid);
      ids[i] = fdef;
      i++;
    }
    Arrays.sort(ids);
    dbList.setModel(new DefaultComboBoxModel(ids));
    showSelection();
  }

  @Override
  public void restoreSettings()
  {
    fillDropDown();

    MainWindow mainWin = (MainWindow)WbManager.getInstance().getCurrentWindow();
    if (mainWin == null) return;

    WbConnection currentConnection = mainWin.getCurrentPanel().map(MainPanel::getConnection).orElse(null);

    if (currentConnection != null)
    {
      ComboBoxModel model = dbList.getModel();
      int index = -1;
      int count = model.getSize();
      for (int i=0; i < count; i++)
      {
        FormatterEntry entry = (FormatterEntry)model.getElementAt(i);
        if (entry.dbid.equals(currentConnection.getDbId()))
        {
          index = i;
          break;
        }
      }
      if (index != -1)
      {
        dbList.setSelectedIndex(index);
      }
    }
  }

  @Override
  public void saveSettings()
  {
    save(getSelectedFormatter());
    int count = dbList.getItemCount();
    for (int i=0; i < count; i++)
    {
      FormatterEntry entry = (FormatterEntry)dbList.getItemAt(i);
      ExternalFormatter.saveDefinition(entry.formatter, entry.dbid);
    }
  }

  @Override
  public boolean validateInput()
  {
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

  private ExternalFormatter getSelectedFormatter()
  {
    FormatterEntry entry = (FormatterEntry)dbList.getSelectedItem();
    return entry.formatter;
  }

  private void showSelection()
  {
    currentFormatter = getSelectedFormatter();
    show(currentFormatter);
  }

  private void show(ExternalFormatter formatter)
  {
    if (formatter == null) return;

    cbxEnabled.setSelected(formatter.isEnabled());
    exePath.setFilename(formatter.getProgram());
    cmdLine.setText(formatter.getCommandLine());
    supportsScripts.setSelected(formatter.supportsMultipleStatements());
  }

  private void save(ExternalFormatter formatter)
  {
    if (formatter == null) return;
    formatter.setEnabled(cbxEnabled.isSelected());
    formatter.setProgram(exePath.getFilename());
    formatter.setCommandLine(cmdLine.getText());
    formatter.setSupportsMultipleStatements(supportsScripts.isSelected());
  }

  /** This method is called from within the constructor to
   * initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is
   * always regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    java.awt.GridBagConstraints gridBagConstraints;

    dbList = new javax.swing.JComboBox();
    exePath = new workbench.gui.components.WbFilePicker();
    jLabel1 = new javax.swing.JLabel();
    jSeparator1 = new javax.swing.JSeparator();
    jLabel2 = new javax.swing.JLabel();
    cmdLine = new javax.swing.JTextField();
    cbxEnabled = new javax.swing.JCheckBox();
    supportsScripts = new javax.swing.JCheckBox();
    lblHelp = new javax.swing.JLabel();

    setLayout(new java.awt.GridBagLayout());

    dbList.setMaximumRowCount(15);
    dbList.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PostgreSQL" }));
    dbList.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        dbListActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    add(dbList, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 0, 5);
    add(exePath, gridBagConstraints);

    jLabel1.setText(ResourceMgr.getString("LblExePath")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
    add(jLabel1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(8, 0, 6, 0);
    add(jSeparator1, gridBagConstraints);

    jLabel2.setText(ResourceMgr.getString("LblExtFmtArg")); // NOI18N
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(7, 0, 0, 0);
    add(jLabel2, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(7, 5, 0, 5);
    add(cmdLine, gridBagConstraints);

    cbxEnabled.setText(ResourceMgr.getString("LblExtFmtEnabled")); // NOI18N
    cbxEnabled.setBorder(null);
    cbxEnabled.addActionListener(new java.awt.event.ActionListener()
    {
      public void actionPerformed(java.awt.event.ActionEvent evt)
      {
        cbxEnabledActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 7, 0);
    add(cbxEnabled, gridBagConstraints);

    supportsScripts.setText(ResourceMgr.getString("LblExtFmtScript")); // NOI18N
    supportsScripts.setBorder(null);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
    add(supportsScripts, gridBagConstraints);

    lblHelp.setText("jLabel3");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LAST_LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.weighty = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 15, 5);
    add(lblHelp, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  private void cbxEnabledActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cbxEnabledActionPerformed
  {//GEN-HEADEREND:event_cbxEnabledActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_cbxEnabledActionPerformed

  private void dbListActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_dbListActionPerformed
  {//GEN-HEADEREND:event_dbListActionPerformed
    save(currentFormatter);
    showSelection();
  }//GEN-LAST:event_dbListActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JCheckBox cbxEnabled;
  private javax.swing.JTextField cmdLine;
  private javax.swing.JComboBox dbList;
  private workbench.gui.components.WbFilePicker exePath;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JLabel lblHelp;
  private javax.swing.JCheckBox supportsScripts;
  // End of variables declaration//GEN-END:variables

  private static class FormatterEntry
    implements Comparable<FormatterEntry>
  {
    ExternalFormatter formatter;
    String dbid;
    String name;

    @Override
    public String toString()
    {
      return name;
    }

    @Override
    public int compareTo(FormatterEntry o)
    {
      if (this.dbid.equals(ExternalFormatter.DEFAULT_DBID)) return -1;
      if (o.dbid.equals(ExternalFormatter.DEFAULT_DBID)) return 1;
      return name.compareTo(o.name);
    }


  }
}
