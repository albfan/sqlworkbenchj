package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;

import workbench.db.ConnectionProfile;
import workbench.gui.MainWindow;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.DataPumper;
import workbench.resource.ResourceMgr;


/**
 *	Action to clear the contents of a entry field
 *	@author  workbench@kellerer.org
 */
public class DataPumperAction extends WbAction
{
	private MainWindow parent;
	
	public DataPumperAction(MainWindow parent)
	{
		super();
		this.parent = parent;
		this.initMenuDefinition("MnuTxtDataPumper");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon(ResourceMgr.getImage("DataPumper"));
	}

	public void executeAction(ActionEvent e)
	{
		if (parent == null) 
		{
			return;
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				WbSwingUtilities.showWaitCursor(parent);
				ConnectionProfile profile = parent.getCurrentProfile();
				DataPumper p = new DataPumper(profile, null);
				p.showWindow(parent);
				WbSwingUtilities.showDefaultCursor(parent);
			}
		});
	}
	
}
