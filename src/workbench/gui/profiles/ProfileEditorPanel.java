/*
 * ProfileEditorPanel.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2006, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.profiles;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.List;
import javax.swing.JPanel;

import javax.swing.JToolBar;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import workbench.db.ConnectionMgr;
import workbench.db.ConnectionProfile;
import workbench.gui.actions.CollapseTreeAction;
import workbench.gui.actions.CopyProfileAction;
import workbench.gui.actions.DeleteListEntryAction;
import workbench.gui.actions.ExpandTreeAction;
import workbench.gui.actions.NewListEntryAction;
import workbench.gui.actions.SaveListFileAction;
import workbench.gui.components.WbSplitPane;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbTraversalPolicy;
import workbench.interfaces.FileActions;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.StringUtil;

/**
 *
 * @author  support@sql-workbench.net
 */
public class ProfileEditorPanel
	extends JPanel
	implements FileActions
{
	private ProfileListModel model;
	private JToolBar toolbar;
	private ConnectionEditorPanel connectionEditor;
	private MouseListener listMouseListener;
	private ProfileFilter filter;
	private NewListEntryAction newItem;
	private CopyProfileAction copyItem;
	private DeleteListEntryAction deleteItem;
	private NewGroupAction newGroup;
	private boolean dummyAdded;
	
	/** Creates new form ProfileEditor */
	public ProfileEditorPanel(String lastProfileKey)
	{
		initComponents(); // will initialize the model!
		
		this.connectionEditor = new ConnectionEditorPanel();
		JPanel dummy = new JPanel(new BorderLayout());
		dummy.add(connectionEditor, BorderLayout.CENTER);
		this.jSplitPane1.setRightComponent(dummy);
		this.fillDrivers();
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		this.toolbar = new WbToolbar();
		newItem = new NewListEntryAction(this, "LblNewProfile");
		newItem.setIcon(ResourceMgr.getImage("NewProfile"));
		this.toolbar.add(newItem);
		
		copyItem = new CopyProfileAction(this);
		copyItem.setEnabled(false);
		this.toolbar.add(copyItem);
		ProfileTree tree = (ProfileTree)profileTree;
		
		this.toolbar.add(new NewGroupAction(tree));
		
		this.toolbar.addSeparator();
		deleteItem = new DeleteListEntryAction(this, "LblDeleteProfile");
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
		
		ProfileKey last = Settings.getInstance().getLastConnection(lastProfileKey);
		((ProfileTree)profileTree).selectProfile(last);
		
		restoreSettings();
	}

	public void done()
	{
		if (this.filter != null) this.filter.done();
	}
	
	public void setInitialFocus()
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				if (dummyAdded)
				{
					connectionEditor.setFocusToTitle();
				}
				else
				{
					profileTree.requestFocus();
				}
			}
		});
	}
	
	public void addSelectionListener(TreeSelectionListener listener)
	{
		this.profileTree.addTreeSelectionListener(listener);
	}
	
	private void fillDrivers()
	{
		List drivers = ConnectionMgr.getInstance().getDrivers();
		this.connectionEditor.setDrivers(drivers);
	}

	public void restoreSettings()
	{
		int pos = Settings.getInstance().getProfileDividerLocation();
		if (pos < 210) pos = 210; // make sure the whole toolbar for the tree is visible!
		this.jSplitPane1.setDividerLocation(pos);
		String groups = Settings.getInstance().getProperty("workbench.profiles.expandedgroups", null);
		List l = StringUtil.stringToList(groups, ",", true, true);
		((ProfileTree)profileTree).expandGroups(l);
	}
	
	public void saveSettings()
	{
		Settings.getInstance().setProfileDividerLocation(this.jSplitPane1.getDividerLocation());
		List expandedGroups = ((ProfileTree)profileTree).getExpandedGroupNames();
		Settings.getInstance().setProperty("workbench.profiles.expandedgroups", StringUtil.listToString(expandedGroups,',', true));
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
	
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
  // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
  private void initComponents()
  {
    jSplitPane1 = new WbSplitPane();

    listPanel = new javax.swing.JPanel();
    jScrollPane1 = new javax.swing.JScrollPane();
    profileTree = new ProfileTree();

    setLayout(new java.awt.BorderLayout());

    jSplitPane1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    jSplitPane1.setDividerLocation(110);
    listPanel.setLayout(new java.awt.BorderLayout());

    jScrollPane1.setPreferredSize(null);
    profileTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener()
    {
      public void valueChanged(javax.swing.event.TreeSelectionEvent evt)
      {
        profileTreeValueChanged(evt);
      }
    });
    profileTree.addMouseListener(new java.awt.event.MouseAdapter()
    {
      public void mouseClicked(java.awt.event.MouseEvent evt)
      {
        profileTreeMouseClicked(evt);
      }
    });

    jScrollPane1.setViewportView(profileTree);

    listPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

    jSplitPane1.setLeftComponent(listPanel);

    add(jSplitPane1, java.awt.BorderLayout.CENTER);

  }// </editor-fold>//GEN-END:initComponents

	private void profileTreeValueChanged(javax.swing.event.TreeSelectionEvent evt)//GEN-FIRST:event_profileTreeValueChanged
	{//GEN-HEADEREND:event_profileTreeValueChanged
		if (this.connectionEditor == null) return;
		if (evt.getSource() == this.profileTree)
		{
			try
			{
				ConnectionProfile newProfile = getSelectedProfile();
				if (newProfile != null)
				{
					if (!this.connectionEditor.isVisible()) this.connectionEditor.setVisible(true);
					this.connectionEditor.setProfile(newProfile);
					this.deleteItem.setEnabled(true);
					this.copyItem.setEnabled(true);
				}
				else
				{
					this.connectionEditor.setVisible(false);
					this.deleteItem.setEnabled(false);
					this.copyItem.setEnabled(false);
				}
			}
			catch (Exception e)
			{
				LogMgr.logError("ProfileEditorPanel.valueChanged()", "Error selecting new profile", e);
			}
		}
	}//GEN-LAST:event_profileTreeValueChanged

	private void profileTreeMouseClicked(java.awt.event.MouseEvent evt)//GEN-FIRST:event_profileTreeMouseClicked
	{//GEN-HEADEREND:event_profileTreeMouseClicked
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

	public ConnectionProfile getSelectedProfile()
	{
		return ((ProfileTree)profileTree).getSelectedProfile();
	}
	
	public void addListMouseListener(MouseListener aListener)
	{
		this.listMouseListener = aListener;
	}


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JSplitPane jSplitPane1;
  private javax.swing.JPanel listPanel;
  private javax.swing.JTree profileTree;
  // End of variables declaration//GEN-END:variables


	/**
	 *	Remove an item from the listmodel.
	 *	This will also remove the profile from the ConnectionMgr's
	 *	profile list.
	 */
	public void deleteItem() throws Exception
	{
		TreePath path = profileTree.getSelectionPath();
		if (path == null) return;
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		if (node == null) return;
		Object o = node.getUserObject();
		if (o instanceof ConnectionProfile)
		{
			ConnectionProfile prof = (ConnectionProfile)o;
			DefaultMutableTreeNode group = (DefaultMutableTreeNode)path.getPathComponent(1);
			int index = model.getIndexOfChild(group, node);
			int children = model.getChildCount(group);
			if (index > 0) index --;
			
			this.model.deleteProfile(prof);
			Object newChild = model.getChild(group, index);
			TreePath newPath = new TreePath(new Object[] { model.getRoot(), group, newChild });
			((ProfileTree)profileTree).selectPath(newPath);
		}
	}

	/**
	 *	Create a new profile. This will be added to the ListModel and the
	 *	ConnectionMgr's profile list.
	 */
	public void newItem(boolean createCopy) throws Exception
	{
		ConnectionProfile cp = null;

		if (createCopy)
		{
  		ConnectionProfile current = getSelectedProfile();
  		if (current != null)
  		{
  			cp = current.createCopy();
  			cp.setName(ResourceMgr.getString("TxtCopyOfProfile") + " " + current.getName());
  		}
		}

		if (cp == null)
		{
			cp = new ConnectionProfile();
			cp.setUseSeparateConnectionPerTab(true);
			cp.setName(ResourceMgr.getString("TxtEmptyProfileName"));
			cp.setGroup(getCurrentGroup());
		}
    cp.setNew();
		
		TreePath newPath = this.model.addProfile(cp);
		((ProfileTree)profileTree).selectPath(newPath);
	}

	private String getCurrentGroup()
	{
		TreePath path = profileTree.getSelectionPath();
		if (path == null) return null;
		 
		
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
		if (node == null) return null;
		
		if (node.getAllowsChildren())
		{
			String g = (String)node.getUserObject();
			return g;
		}
		return null;
	}

	public void saveItem() throws Exception
	{
		ConnectionMgr conn = ConnectionMgr.getInstance();
		this.connectionEditor.updateProfile();
		conn.saveProfiles();
	}

	public int getProfileCount()
	{
		return this.profileTree.getRowCount();
	}
	
}
