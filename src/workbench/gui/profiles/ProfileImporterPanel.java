/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.gui.profiles;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;

import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ProfileManager;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.WbAction;
import workbench.gui.components.ExtensionFileFilter;

import workbench.util.CollectionUtil;
import workbench.util.StringUtil;
import workbench.util.WbFile;


/**
 *
 * @author Thomas Kellerer
 */
public class ProfileImporterPanel
  extends JPanel
  implements ActionListener, ValidatingComponent
{
  private static final String CONFIG_PREFIX = "workbench.gui.profileimporter.";
  private static final String DIRKEY_SOURCE = "lastsourcedir";
  private static final String DIRKEY_CURRENT = "lastprofilesdir";
  private static final String CMD_SAVE_CURRENT = "saveCurrent";
  private static final String CMD_OPEN_CURRENT = "openCurrent";
  private static final String CMD_SAVE_SOURCE = "saveSource";
  private static final String CMD_OPEN_SOURCE = "openSource";

  private WbAction openSourceAction;
  private WbAction saveSourceAction;
  private WbAction openCurrentAction;
  private WbAction saveCurrentAction;

  private enum CheckResult
  {
    ok,
    saveFile,
    cancel;
  }

  public ProfileImporterPanel()
  {
    initComponents();
    sourceFilename.setText(ResourceMgr.getString("LblNone"));
    Border b = new EmptyBorder(0,0,0,0);
    sourceFilename.setBorder(b);
    currentFilename.setBorder(b);

    sourceProfiles.setModel(ProfileListModel.emptyModel());
    openSourceAction = new WbAction(this, CMD_OPEN_SOURCE);
    openSourceAction.setIcon("open");
    sourceToolbar.add(openSourceAction);
    saveSourceAction = new WbAction(this, CMD_SAVE_SOURCE);
    saveSourceAction.setIcon("save");
    sourceToolbar.add(saveSourceAction);
    sourceToolbar.addSeparator();

    openCurrentAction = new WbAction(this, CMD_OPEN_CURRENT);
    openCurrentAction.setIcon("open");
    currentToolbar.add(openCurrentAction);
    saveCurrentAction = new WbAction(this, CMD_SAVE_CURRENT);
    saveCurrentAction.setIcon("save");
    currentToolbar.add(saveCurrentAction);
    currentToolbar.addSeparator();
    currentInfopanel.setPreferredSize(sourceInfoPanel.getPreferredSize());
  }

  public void loadCurrentProfiles()
  {
    ProfileListModel model = new ProfileListModel(ConnectionMgr.getInstance().getProfiles());
    model.setSourceFile(ConnectionMgr.getInstance().getProfilesFile());
    WbSwingUtilities.invoke(() ->
    {
      currentProfiles.setModel(model);
      currentFilename.setText(ConnectionMgr.getInstance().getProfilesPath());
    });
  }

  public void applyProfiles()
  {
    ProfileListModel list = (ProfileListModel)currentProfiles.getModel();
    list.applyProfiles();
  }

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getActionCommand().equals(CMD_OPEN_SOURCE))
    {
      ProfileListModel model = loadFile(DIRKEY_SOURCE, sourceFilename);
      if (model != null)
      {
        WbSwingUtilities.invokeLater(() ->
        {
          sourceProfiles.setModel(model);
        });
      }
    }
    if (e.getActionCommand().equals(CMD_OPEN_CURRENT))
    {
      ProfileListModel model = loadFile(DIRKEY_CURRENT, currentFilename);
      if (model != null)
      {
        WbSwingUtilities.invokeLater(() ->
        {
          currentProfiles.setModel(model);
        });
      }
    }
    if (e.getActionCommand().equals(CMD_SAVE_SOURCE))
    {
      saveSource(WbAction.isCtrlPressed(e));
    }
    if (e.getActionCommand().equals(CMD_SAVE_CURRENT))
    {
      saveCurrent(WbAction.isCtrlPressed(e));
    }
  }

  private JFileChooser createFileChooser(String dirkey)
  {
    String settingsKey = CONFIG_PREFIX + dirkey;
    String dir = Settings.getInstance().getProperty(settingsKey, Settings.getInstance().getConfigDir().getAbsolutePath());

    JFileChooser chooser = new JFileChooser(dir);
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    chooser.addChoosableFileFilter(new ExtensionFileFilter("XML Profiles (*.xml)", CollectionUtil.arrayList("xml"), true));
    chooser.addChoosableFileFilter(new ExtensionFileFilter("Properties Profiles (*.properties)", CollectionUtil.arrayList("properties"), true));
    chooser.setMultiSelectionEnabled(false);
    return chooser;
  }

  private ProfileListModel loadFile(String dirkey, JTextField info)
  {
    String settingsKey = CONFIG_PREFIX + dirkey;
    JFileChooser chooser = createFileChooser(dirkey);
    int choice = chooser.showOpenDialog(this);

    WbFile currentDir = new WbFile(chooser.getCurrentDirectory());
    Settings.getInstance().setProperty(settingsKey, currentDir.getFullPath());

    if (choice == JFileChooser.APPROVE_OPTION)
    {
      File f = chooser.getSelectedFile();
      if (f == null) return null;

      if (ConnectionMgr.getInstance().getProfilesFile().equals(f))
      {
        info.setText(ConnectionMgr.getInstance().getProfilesPath());
        ProfileListModel model = new ProfileListModel(ConnectionMgr.getInstance().getProfiles());
        model.setSourceFile(ConnectionMgr.getInstance().getProfilesFile());
      }
      else
      {
        info.setText(new WbFile(f).getFullPath());
        ProfileManager mgr = new ProfileManager(f);
        mgr.load();
        ProfileListModel model = new ProfileListModel(mgr.getProfiles());
        model.setSourceFile(f);
        return model;
      }
    }
    return null;
  }

  private void saveCurrent(boolean doPrompt)
  {
    ProfileListModel model = currentProfiles.getModel();
    File file = model.getSourceFile();
    if (file == null || doPrompt)
    {
      file = selectOutputFile(DIRKEY_CURRENT);
      if (file != null)
      {
        currentFilename.setText(file.getAbsolutePath());
      }
    }
    if (file == null) return;

    if (ConnectionMgr.getInstance().getProfilesFile().equals(file))
    {
      if (model.profilesAreModified() || model.groupsChanged())
      {
        currentProfiles.getModel().saveProfiles();
      }
    }
    else
    {
      model.saveTo(file);
    }
  }

  private File selectOutputFile(String dirkey)
  {
    JFileChooser chooser = createFileChooser(dirkey);
    int choice = chooser.showSaveDialog(this);
    if (choice == JFileChooser.APPROVE_OPTION)
    {
      File f = chooser.getSelectedFile();
      if (f == null) return null;
      WbFile wf = new WbFile(f);
      if (StringUtil.isEmptyString(wf.getExtension()))
      {
        ExtensionFileFilter ff = (ExtensionFileFilter)chooser.getFileFilter();
        String ext = ff.getDefaultExtension();
        String fname = wf.getFullPath() + "." + ext;
        return new File(fname);
      }
      return f;
    }
    return null;
  }

  private boolean saveSource(boolean doPrompt)
  {
    File file = sourceProfiles.getModel().getSourceFile();
    if (file == null || doPrompt)
    {
      file = selectOutputFile(DIRKEY_SOURCE);
      if (file != null)
      {
        sourceFilename.setText(file.getAbsolutePath());
      }
    }

    if (file == null) return false;

    ProfileListModel model = sourceProfiles.getModel();
    try
    {
      model.saveTo(file);
      return true;
    }
    catch (Exception ex)
    {
      LogMgr.logError("ProfileImportPanel.saveSource()", "Could not save profiles", ex);
      return false;
    }
  }

  @Override
  public boolean validateInput()
  {
    CheckResult result = checkUnsaved(currentProfiles.getModel());
    if (result == CheckResult.cancel) return false;

    if (result == CheckResult.saveFile)
    {
      saveCurrent(false);
    }

    result = checkUnsaved(sourceProfiles.getModel());
    if (result == CheckResult.cancel) return false;

    if (result == CheckResult.saveFile)
    {
      saveSource(false);
    }

    return true;
  }

  private CheckResult checkUnsaved(ProfileListModel model)
  {
    if (model == null) return CheckResult.ok;
    if (!model.isChanged()) return CheckResult.ok;

    File f = model.getSourceFile();
    String fname = null;
    if (f == null)
    {
      fname = ResourceMgr.getString("LblNone");
    }
    else
    {
      fname = f.getAbsolutePath();
    }

    String msg = ResourceMgr.getFormattedString("MsgConfirmUnsavedEditorFile", fname);

    int choice = WbSwingUtilities.getYesNoCancel(this, msg);
    if (choice == JOptionPane.YES_OPTION)
    {
      return CheckResult.saveFile;
    }
    if (choice == JOptionPane.CANCEL_OPTION)
    {
      return CheckResult.cancel;
    }
    return CheckResult.ok;
  }

  @Override
  public void componentDisplayed()
  {
    splitPane.setDividerLocation(0.5d);
    loadCurrentProfiles();
    ProfileListModel model = new ProfileListModel();
    sourceProfiles.setModel(model);
  }

  @Override
  public void componentWillBeClosed()
  {
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

    splitPane = new workbench.gui.components.WbSplitPane();
    sourcePanel = new javax.swing.JPanel();
    sourceInfoPanel = new javax.swing.JPanel();
    sourceFilename = new workbench.gui.components.WbLabelField();
    sourceToolbar = new workbench.gui.components.WbToolbar();
    jScrollPane3 = new javax.swing.JScrollPane();
    sourceProfiles = new workbench.gui.profiles.ProfileTree();
    currentPanel = new javax.swing.JPanel();
    currentInfopanel = new javax.swing.JPanel();
    currentFilename = new workbench.gui.components.WbLabelField();
    currentToolbar = new workbench.gui.components.WbToolbar();
    jScrollPane4 = new javax.swing.JScrollPane();
    currentProfiles = new workbench.gui.profiles.ProfileTree();

    setLayout(new java.awt.BorderLayout());

    splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 5, 15, 5));
    splitPane.setDividerLocation(150);

    sourcePanel.setLayout(new java.awt.BorderLayout());

    sourceInfoPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    sourceInfoPanel.setLayout(new java.awt.GridBagLayout());

    sourceFilename.setText("wbLabelField2");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
    sourceInfoPanel.add(sourceFilename, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    sourceInfoPanel.add(sourceToolbar, gridBagConstraints);

    sourcePanel.add(sourceInfoPanel, java.awt.BorderLayout.NORTH);

    sourceProfiles.setName("sourceProfiles"); // NOI18N
    jScrollPane3.setViewportView(sourceProfiles);

    sourcePanel.add(jScrollPane3, java.awt.BorderLayout.CENTER);

    splitPane.setLeftComponent(sourcePanel);

    currentPanel.setLayout(new java.awt.BorderLayout());

    currentInfopanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    currentInfopanel.setLayout(new java.awt.GridBagLayout());

    currentFilename.setText("wbLabelField1");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    gridBagConstraints.weightx = 1.0;
    gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
    currentInfopanel.add(currentFilename, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
    currentInfopanel.add(currentToolbar, gridBagConstraints);

    currentPanel.add(currentInfopanel, java.awt.BorderLayout.NORTH);

    currentProfiles.setName("currentProfiles"); // NOI18N
    jScrollPane4.setViewportView(currentProfiles);

    currentPanel.add(jScrollPane4, java.awt.BorderLayout.CENTER);

    splitPane.setRightComponent(currentPanel);

    add(splitPane, java.awt.BorderLayout.CENTER);
  }// </editor-fold>//GEN-END:initComponents


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private workbench.gui.components.WbLabelField currentFilename;
  private javax.swing.JPanel currentInfopanel;
  private javax.swing.JPanel currentPanel;
  private workbench.gui.profiles.ProfileTree currentProfiles;
  private workbench.gui.components.WbToolbar currentToolbar;
  private javax.swing.JScrollPane jScrollPane3;
  private javax.swing.JScrollPane jScrollPane4;
  private workbench.gui.components.WbLabelField sourceFilename;
  private javax.swing.JPanel sourceInfoPanel;
  private javax.swing.JPanel sourcePanel;
  private workbench.gui.profiles.ProfileTree sourceProfiles;
  private workbench.gui.components.WbToolbar sourceToolbar;
  private workbench.gui.components.WbSplitPane splitPane;
  // End of variables declaration//GEN-END:variables
}
