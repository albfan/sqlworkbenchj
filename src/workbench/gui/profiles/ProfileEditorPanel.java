/*
 * ProfileEditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2015, Thomas Kellerer
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
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.interfaces.FileActions;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.db.DbDriver;

import workbench.gui.WbSwingUtilities;
import workbench.gui.actions.CollapseTreeAction;
import workbench.gui.actions.CopyProfileAction;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.ExpandTreeAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.actions.SaveListFileAction;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbLabel;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbTraversalPolicy;

import workbench.util.StringUtil;

/**
 *
 * @author  Thomas Kellerer
 */
public class ProfileEditorPanel
	extends JPanel
	implements FileActions, ValidatingComponent, PropertyChangeListener, KeyListener
{
	private ProfileListModel model;
	private WbToolbar toolbar;
	protected ConnectionEditorPanel connectionEditor;
	private MouseListener listMouseListener;
	private NewListEntryAction newItem;
	private CopyProfileAction copyItem;
	private DeleteListEntryAction deleteItem;
	private JTextField filterValue;
	protected boolean dummyAdded;
	private List<String> orgExpandedGroups;
	private ConnectionProfile initialProfile;

	public ProfileEditorPanel(String lastProfileKey)
	{
		super();
		initComponents(); // will initialize the model!

		this.connectionEditor = new ConnectionEditorPanel();
		JPanel dummy = new JPanel(new BorderLayout());
		dummy.add(connectionEditor, BorderLayout.CENTER);
		this.jSplitPane.setRightComponent(dummy);
		this.fillDrivers();

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		this.toolbar = new WbToolbar();
		newItem = new NewListEntryAction(this, "LblNewProfile");
		newItem.setIcon("new-profile");
		this.toolbar.add(newItem);

		copyItem = new CopyProfileAction(this);
		copyItem.setEnabled(false);
		this.toolbar.add(copyItem);
		ProfileTree tree = (ProfileTree)profileTree;
		tree.setBorder(null);

		this.toolbar.add(new NewGroupAction(tree, "LblNewProfileGroup"));

		this.toolbar.addSeparator();
		deleteItem = new DeleteListEntryAction(this);
		deleteItem.setEnabled(false);
		this.toolbar.add(deleteItem);

		this.toolbar.addSeparator();
		this.toolbar.add(new SaveListFileAction(this));

		this.toolbar.addSeparator();
		this.toolbar.add(new ExpandTreeAction(tree));
		this.toolbar.add(new CollapseTreeAction(tree));

		p.add(toolbar, BorderLayout.PAGE_START);

		if (GuiSettings.enableProfileQuickFilter())
		{
			JPanel filterPanel = new JPanel(new BorderLayout(0, 1));
			filterPanel.setBorder(new DividerBorder(DividerBorder.TOP));
			filterValue = new JTextField();
			WbLabel lbl = new WbLabel();
			lbl.setTextByKey("LblFilter");
			lbl.setLabelFor(filterValue);
			lbl.setBorder(new EmptyBorder(0, 5, 0, 5));
			filterPanel.add(lbl, BorderLayout.LINE_START);
			filterPanel.add(filterValue, BorderLayout.CENTER);
			p.add(filterPanel, BorderLayout.PAGE_END);

			filterValue.setToolTipText(lbl.getToolTipText());
			filterValue.addKeyListener(this);
		}

		this.listPanel.add(p, BorderLayout.NORTH);

		tree.setDeleteAction(deleteItem);

		WbTraversalPolicy policy = new WbTraversalPolicy();
		this.setFocusCycleRoot(false);
		policy.addComponent(this.profileTree);
		policy.addComponent(this.connectionEditor);
		policy.setDefaultComponent(this.profileTree);
		this.setFocusTraversalPolicy(policy);

		buildTree();
		restoreSettings();

		final ProfileKey last = Settings.getInstance().getLastConnection(lastProfileKey);
		((ProfileTree)profileTree).selectProfile(last);
		ConnectionMgr.getInstance().addDriverChangeListener(this);
	}

	private int getSelectedRow()
	{
		if (profileTree.getSelectionCount() != 1) return -1;
		return profileTree.getSelectionRows()[0];
	}

	private void selectPreviousItem()
	{
		int row = getSelectedRow();
		if (row < 0)
		{
			row = 0;
		}
		else if (row > 0)
		{
			row --;
		}
		profileTree.setSelectionRow(row);
	}

	private void selectNextItem()
	{
		int row = getSelectedRow();
		int count = profileTree.getRowCount();
		if (row < 0)
		{
			row = 0;
		}
		else if (row < count - 1)
		{
			row ++;
		}
		profileTree.setSelectionRow(row);
	}

	private void collapseCurrentGroup()
	{
		ProfileTree tree = (ProfileTree)profileTree;
		TreePath path = tree.getSelectionPath();
		if (tree.isGroup(path))
		{
			tree.collapsePath(path);
		}
		else
		{
			TreePath parent = path.getParentPath();
			if (parent != null)
			{
				tree.selectPath(parent);
			}
		}
	}

	private void expandCurrentGroup()
	{
		ProfileTree tree = (ProfileTree)profileTree;
		if (tree.isGroup(tree.getSelectionPath()))
		{
			tree.expandPath(tree.getSelectionPath());
		}
	}

	@Override
	public void keyTyped(final KeyEvent e)
	{
    EventQueue.invokeLater(new Runnable()
    {
      @Override
      public void run()
      {
        applyFilter();
      }
    });
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		if (e.getSource() != this.filterValue || e.getModifiers() != 0) return;

		switch (e.getKeyCode())
		{
			case KeyEvent.VK_UP:
				selectPreviousItem();
				e.consume();
				break;
			case KeyEvent.VK_DOWN:
				selectNextItem();
				e.consume();
				break;
			case KeyEvent.VK_RIGHT:
				expandCurrentGroup();
				e.consume();
				break;
			case KeyEvent.VK_LEFT:
				collapseCurrentGroup();
				e.consume();
				break;
			case KeyEvent.VK_ESCAPE:
				if (StringUtil.isNonBlank(filterValue.getText()))
				{
					e.consume();
					filterValue.setText("");
					applyFilter();
				}
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
	}

	private void applyFilter()
	{
		if (orgExpandedGroups == null)
		{
			// save the groups that were expanded the first time the filter is applied
			// in order to be able to restore this once the filter is reset
			// or the settings are stored.
			orgExpandedGroups = ((ProfileTree)profileTree).getExpandedGroupNames();
		}
		ProfileTree tree = (ProfileTree) profileTree;
		ConnectionProfile profile = tree.getSelectedProfile();
		if (initialProfile == null)
		{
			initialProfile = profile;
		}

		model.applyFilter(filterValue.getText());
		if (model.isFiltered())
		{
			tree.expandAll();
			if (profile != null)
			{
				tree.selectProfile(profile.getKey());
			}
		}
		else if (orgExpandedGroups != null)
		{
			tree.expandGroups(orgExpandedGroups);
			if (initialProfile != null)
			{
				tree.selectProfile(initialProfile.getKey());
			}
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("drivers"))
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					fillDrivers();
				}
			});
		}
	}

	public void done()
	{
		ConnectionMgr.getInstance().removeDriverChangeListener(this);
	}

	public JComponent getInitialFocusComponent()
	{
		if (dummyAdded)
		{
			return connectionEditor.getInitialFocusComponent();
		}
		else
		{
			return profileTree;
		}
	}

	public void addSelectionListener(TreeSelectionListener listener)
	{
		this.profileTree.addTreeSelectionListener(listener);
	}

	public DbDriver getCurrentDriver()
	{
		return this.connectionEditor.getCurrentDriver();
	}

	public final void fillDrivers()
	{
		List<DbDriver> drivers = ConnectionMgr.getInstance().getDrivers();
		this.connectionEditor.setDrivers(drivers);
	}

	public final void restoreSettings()
	{
		int pos = GuiSettings.getProfileDividerLocation();
		if (pos < 200)
		{
			pos = 200; // make sure the whole toolbar for the tree is visible!
		}
		this.jSplitPane.setDividerLocation(pos);
		String groups = Settings.getInstance().getProperty("workbench.profiles.expandedgroups", null);
		List<String> l = StringUtil.stringToList(groups, ",", true, true);
		((ProfileTree)profileTree).expandGroups(l);
	}

	public boolean profilesChanged()
	{
		ProfileListModel list = (ProfileListModel)profileTree.getModel();
		return list.profilesAreModified();
	}

	public boolean groupsChanged()
	{
		ProfileListModel list = (ProfileListModel)profileTree.getModel();
		return list.groupsChanged();
	}

	/**
	 * Hands the complete ProfileList over to the ConnectionMgr.
	 */
	public void applyProfiles()
	{
		this.connectionEditor.updateProfile();
		ProfileListModel list = (ProfileListModel)profileTree.getModel();
		list.applyProfiles();
	}

	public void saveSettings()
	{
		GuiSettings.setProfileDividerLocation(this.jSplitPane.getDividerLocation());
		List<String> expandedGroups = orgExpandedGroups;
		if (expandedGroups == null)
		{
			expandedGroups = ((ProfileTree)profileTree).getExpandedGroupNames();
		}
		Settings.getInstance().setProperty("workbench.profiles.expandedgroups", StringUtil.listToString(expandedGroups, ',', true));
	}

	private void buildTree()
	{
		this.dummyAdded = false;
		this.model = new ProfileListModel();
		if (model.getSize() < 1)
		{
			this.model.addEmptyProfile();
			this.dummyAdded = true;
		}
		this.profileTree.setModel(this.model);
		this.connectionEditor.setSourceList(this.model);
	}

	public ConnectionProfile getSelectedProfile()
	{
		return ((ProfileTree)profileTree).getSelectedProfile();
	}

	public void addListMouseListener(MouseListener aListener)
	{
		this.listMouseListener = aListener;
	}

	private boolean checkGroupWithProfiles(DefaultMutableTreeNode groupNode)
	{
		List<String> groups = model.getGroups();
		JPanel p = new JPanel();
		DefaultComboBoxModel m = new DefaultComboBoxModel(groups.toArray());
		JComboBox groupBox = new JComboBox(m);
		groupBox.setSelectedIndex(0);
		p.setLayout(new BorderLayout(0, 5));
		String groupName = (String)groupNode.getUserObject();
		String lbl = ResourceMgr.getFormattedString("LblDeleteNonEmptyGroup", groupName);
		p.add(new JLabel(lbl), BorderLayout.NORTH);
		p.add(groupBox, BorderLayout.SOUTH);
		String[] options = new String[]{ResourceMgr.getString("LblMoveProfiles"), ResourceMgr.getString("LblDeleteProfiles")};

		Dialog parent = (Dialog)SwingUtilities.getWindowAncestor(this);

		ValidatingDialog dialog = new ValidatingDialog(parent, ResourceMgr.TXT_PRODUCT_NAME, p, options);
		WbSwingUtilities.center(dialog, parent);
		dialog.setVisible(true);
		if (dialog.isCancelled())
		{
			return false;
		}

		int result = dialog.getSelectedOption();
		if (result == 0)
		{
			// move profiles
			String group = (String)groupBox.getSelectedItem();
			if (group == null)
			{
				return false;
			}

			model.moveProfilesToGroup(groupNode, group);
			return true;
		}
		else if (result == 1)
		{
			return true;
		}

		return false;
	}

	/**
	 *	Remove an item from the listmodel.
	 *	This will also remove the profile from the ConnectionMgr's
	 *	profile list.
	 */
	@Override
	public void deleteItem()
		throws Exception
	{
		TreePath[] path = profileTree.getSelectionPaths();
		if (path == null) return;
		if (path.length == 0)	return;

		ProfileTree tree = (ProfileTree)profileTree;
		if (tree.onlyProfilesSelected())
		{
			DefaultMutableTreeNode group = (DefaultMutableTreeNode)path[0].getPathComponent(1);
			DefaultMutableTreeNode firstNode = (DefaultMutableTreeNode)path[0].getLastPathComponent();
			int newIndex = model.getIndexOfChild(group, firstNode);
			if (newIndex > 0)
			{
				newIndex--;
			}

			for (TreePath element : path)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode) element.getLastPathComponent();
				ConnectionProfile prof = (ConnectionProfile)node.getUserObject();
				this.model.deleteProfile(prof);
			}
			if (group.getChildCount() > 0)
			{
				Object newChild = model.getChild(group, newIndex);
				TreePath newPath = new TreePath(new Object[]{model.getRoot(), group, newChild});
				((ProfileTree)profileTree).selectPath(newPath);
			}
		}
		else // delete a group
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path[0].getLastPathComponent();
			if (node.getChildCount() > 0)
			{
				if (!checkGroupWithProfiles(node))
				{
					return;
				}
			}
			model.removeGroupNode(node);
		}
	}

	/**
	 *	Create a new profile. This will be added to the ListModel and the
	 *	ConnectionMgr's profile list.
	 */
	@Override
	public void newItem(boolean createCopy)
		throws Exception
	{
		ConnectionProfile cp = null;

		if (createCopy)
		{
			ConnectionProfile current = getSelectedProfile();
			if (current != null)
			{
				cp = current.createCopy();
				cp.setName(ResourceMgr.getString("TxtCopyOf") + " " + current.getName());
			}
		}

		if (cp == null)
		{
			cp = ConnectionProfile.createEmptyProfile();
			cp.setGroup(getCurrentGroup());
		}
		cp.setNew();

		TreePath newPath = this.model.addProfile(cp);
		((ProfileTree)profileTree).selectPath(newPath);
	}

	private String getCurrentGroup()
	{
		TreePath path = profileTree.getSelectionPath();
		if (path == null)
		{
			return null;
		}


		DefaultMutableTreeNode node = null;
		if (path.getPathCount() == 2)
		{
			// group node selected
			node = (DefaultMutableTreeNode)path.getLastPathComponent();
		}
		if (path.getPathCount() > 2)
		{
			// Get the group of the currently selected profile;
			node = (DefaultMutableTreeNode)path.getPathComponent(1);
		}
		if (node == null)
		{
			return null;
		}

		if (node.getAllowsChildren())
		{
			String g = (String)node.getUserObject();
			return g;
		}
		return null;
	}

	@Override
	public void saveItem()
		throws Exception
	{
		// Synchronize the current profile with the list
		this.connectionEditor.updateProfile();
		ProfileListModel list = (ProfileListModel)profileTree.getModel();
		list.saveProfiles();
	}

	public int getProfileCount()
	{
		return this.profileTree.getRowCount();
	}

	@Override
	public boolean validateInput()
	{
		return this.connectionEditor.validateInput();
	}

	@Override
	public void componentDisplayed()
	{
		// nothing to do
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents()
  {

    jSplitPane = new WbSplitPane();

    listPanel = new JPanel();
    jScrollPane1 = new JScrollPane();
    profileTree = new ProfileTree();

    FormListener formListener = new FormListener();

    setLayout(new BorderLayout());

    jSplitPane.setBorder(BorderFactory.createEtchedBorder());
    jSplitPane.setDividerLocation(110);
    jSplitPane.setMinimumSize(new Dimension(400, 400));

    listPanel.setLayout(new BorderLayout());

    jScrollPane1.setPreferredSize(null);

    profileTree.setMinimumSize(new Dimension(150, 400));
    profileTree.setName("profileTree"); // NOI18N
    profileTree.addMouseListener(formListener);
    profileTree.addTreeSelectionListener(formListener);
    jScrollPane1.setViewportView(profileTree);

    listPanel.add(jScrollPane1, BorderLayout.CENTER);

    jSplitPane.setLeftComponent(listPanel);

    add(jSplitPane, BorderLayout.CENTER);
  }

  // Code for dispatching events from components to event handlers.

  private class FormListener implements MouseListener, TreeSelectionListener
  {
    FormListener() {}
    public void mouseClicked(MouseEvent evt)
    {
      if (evt.getSource() == profileTree)
      {
        ProfileEditorPanel.this.profileTreeMouseClicked(evt);
      }
    }

    public void mouseEntered(MouseEvent evt)
    {
    }

    public void mouseExited(MouseEvent evt)
    {
    }

    public void mousePressed(MouseEvent evt)
    {
    }

    public void mouseReleased(MouseEvent evt)
    {
    }

    public void valueChanged(TreeSelectionEvent evt)
    {
      if (evt.getSource() == profileTree)
      {
        ProfileEditorPanel.this.profileTreeValueChanged(evt);
      }
    }
  }// </editor-fold>//GEN-END:initComponents
	private void profileTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_profileTreeMouseClicked

		if (this.listMouseListener != null)
		{
			Point p = evt.getPoint();
			TreePath path = profileTree.getClosestPathForLocation((int)p.getX(), (int)p.getY());
			TreeNode n = (TreeNode)path.getLastPathComponent();
			if (n.isLeaf())
			{
				this.listMouseListener.mouseClicked(evt);
			}
		}
	}//GEN-LAST:event_profileTreeMouseClicked

	private void profileTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_profileTreeValueChanged

		if (this.connectionEditor == null)
		{
			return;
		}

		if (evt.getSource() == this.profileTree)
		{
			try
			{
				ConnectionProfile newProfile = getSelectedProfile();
				if (newProfile != null)
				{
					if (!this.connectionEditor.isVisible())
					{
						this.connectionEditor.setVisible(true);
					}
					this.connectionEditor.setProfile(newProfile);
					this.deleteItem.setEnabled(true);
					this.copyItem.setEnabled(true);
				}
				else
				{
					this.connectionEditor.setVisible(false);
					this.deleteItem.setEnabled(true);
					this.copyItem.setEnabled(false);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ProfileEditorPanel.valueChanged()", "Error selecting new profile", e);
			}
		}
	}//GEN-LAST:event_profileTreeValueChanged

  // Variables declaration - do not modify//GEN-BEGIN:variables
  protected JScrollPane jScrollPane1;
  protected JSplitPane jSplitPane;
  protected JPanel listPanel;
  protected JTree profileTree;
  // End of variables declaration//GEN-END:variables
}
