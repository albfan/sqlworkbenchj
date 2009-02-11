/*
 * ProfileEditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2009, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
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
import workbench.gui.components.ValidatingDialog;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbTraversalPolicy;
import workbench.interfaces.FileActions;
import workbench.interfaces.ValidatingComponent;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ProfileEditorPanel
	extends JPanel
	implements FileActions, ValidatingComponent, PropertyChangeListener
{
	private ProfileListModel model;
	private JToolBar toolbar;
	protected ConnectionEditorPanel connectionEditor;
	private MouseListener listMouseListener;
	private NewListEntryAction newItem;
	private CopyProfileAction copyItem;
	private DeleteListEntryAction deleteItem;
	protected boolean dummyAdded;

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
		newItem.setIcon("NewProfile");
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

		this.listPanel.add(toolbar, BorderLayout.NORTH);

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

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (evt.getPropertyName().equals("driver"))
		{
			EventQueue.invokeLater(new Runnable()
			{
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

	public void fillDrivers()
	{
		List<DbDriver> drivers = ConnectionMgr.getInstance().getDrivers();
		this.connectionEditor.setDrivers(drivers);
	}

	public void restoreSettings()
	{
		int pos = GuiSettings.getProfileDividerLocation();
		if (pos < 210)
		{
			pos = 210; // make sure the whole toolbar for the tree is visible!
		}
		this.jSplitPane.setDividerLocation(pos);
		String groups = Settings.getInstance().getProperty("workbench.profiles.expandedgroups", null);
		List<String> l = StringUtil.stringToList(groups, ",", true, true);
		((ProfileTree)profileTree).expandGroups(l);
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
		List<String> expandedGroups = ((ProfileTree)profileTree).getExpandedGroupNames();
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
		p.add(new JLabel(ResourceMgr.getString("LblDeleteNonEmptyGroup")), BorderLayout.NORTH);
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
	public void deleteItem()
		throws Exception
	{
		TreePath[] path = profileTree.getSelectionPaths();
		if (path == null)
		{
			return;
		}
		if (path.length == 0)
		{
			return;
		}

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

			for (int i = 0; i < path.length; i++)
			{
				DefaultMutableTreeNode node = (DefaultMutableTreeNode)path[i].getLastPathComponent();
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

	public boolean validateInput()
	{
		return this.connectionEditor.validateInput();
	}

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
  private void initComponents() {

    jSplitPane = new WbSplitPane();

    listPanel = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    profileTree = new ProfileTree();

    FormListener formListener = new FormListener();

    setLayout(new java.awt.BorderLayout());

    jSplitPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    jSplitPane.setDividerLocation(110);

    listPanel.setLayout(new java.awt.BorderLayout());

    jScrollPane1.setPreferredSize(null);

    profileTree.setName("profileTree"); // NOI18N
    profileTree.addMouseListener(formListener);
    profileTree.addTreeSelectionListener(formListener);
    jScrollPane1.setViewportView(profileTree);

    listPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

    jSplitPane.setLeftComponent(listPanel);

    add(jSplitPane, java.awt.BorderLayout.CENTER);
  }

  // Code for dispatching events from components to event handlers.

  private class FormListener implements java.awt.event.MouseListener, javax.swing.event.TreeSelectionListener {
    FormListener() {}
    public void mouseClicked(java.awt.event.MouseEvent evt) {
      if (evt.getSource() == profileTree) {
        ProfileEditorPanel.this.profileTreeMouseClicked(evt);
      }
    }

    public void mouseEntered(java.awt.event.MouseEvent evt) {
    }

    public void mouseExited(java.awt.event.MouseEvent evt) {
    }

    public void mousePressed(java.awt.event.MouseEvent evt) {
    }

    public void mouseReleased(java.awt.event.MouseEvent evt) {
    }

    public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
      if (evt.getSource() == profileTree) {
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
					TreePath p = profileTree.getSelectionPath();
					TreeNode n = (TreeNode)(p != null ? p.getLastPathComponent() : null);
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
  protected javax.swing.JScrollPane jScrollPane1;
  protected javax.swing.JSplitPane jSplitPane;
  protected javax.swing.JPanel listPanel;
  protected javax.swing.JTree profileTree;
  // End of variables declaration//GEN-END:variables
}
