package workbench.gui.actions;

import java.awt.event.ActionEvent;
import javax.swing.JFrame;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.tools.VersionCheckDialog;
import workbench.resource.ResourceMgr;

/**
 *	@author  workbench@kellerer.org
 */
public class VersionCheckAction extends WbAction
{

	public VersionCheckAction()
	{
		super();
		this.initMenuDefinition("MnuTxtVersionCheck");
		this.setMenuItemName(ResourceMgr.MNU_TXT_TOOLS);
		this.setIcon(null);
	}

	public void executeAction(ActionEvent e)
	{
		JFrame parent = WbManager.getInstance().getCurrentWindow();
		VersionCheckDialog dialog = new VersionCheckDialog(parent, true);
		WbSwingUtilities.center(dialog, parent);
		dialog.startRetrieveVersions();
		dialog.show();
	}
}
