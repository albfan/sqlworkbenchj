/*
 * ObjectScripterUI.java
 *
 * Created on September 4, 2003, 5:45 PM
 */

package workbench.gui.dbobjects;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowListener;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

import workbench.WbManager;
import workbench.db.ObjectScripter;
import workbench.db.WbConnection;
import workbench.db.oracle.OracleObjectCompiler;
import workbench.gui.WbSwingUtilities;
import workbench.gui.sql.EditorPanel;
import workbench.resource.ResourceMgr;
import workbench.util.WbThread;

/**
 *
 * @author  workbench@kellerer.org
 */
public class ObjectCompilerUI
	extends JPanel
	implements Runnable, WindowListener
{
	private Thread worker;
	private EditorPanel log;
	private JFrame window;
	private List types;
	private List names;
	private OracleObjectCompiler compiler;

	public ObjectCompilerUI(List names, List types, WbConnection conn)
		throws SQLException
	{
		super();
		this.names = names;
		this.types = types;
		this.compiler = new OracleObjectCompiler(conn);
		this.setLayout(new BorderLayout());
		this.log = EditorPanel.createTextEditor();
		this.add(this.log, BorderLayout.CENTER);
	}

	private void startCompile()
	{
		this.worker = new WbThread(this, "ObjectCompiler Thread");
		this.worker.start();
	}

	public void run()
	{
		String msg = ResourceMgr.getString("TxtCompilingObject");
		this.log.setText("");
		int count = this.names.size();


		for (int i=0; i < count; i++)
		{
			String name = (String)this.names.get(i);
			String type = (String)this.types.get(i);
			if (i > 0) this.log.appendLine("\n");
			this.log.appendLine(msg + " " + name + " ... ");
			if (this.compiler.compileObject(name, type))
			{
				this.log.appendLine(ResourceMgr.getString("TxtOK"));
			}
			else
			{
				this.log.appendLine(ResourceMgr.getString("TxtError"));
				this.log.appendLine(this.compiler.getLastError());
			}
		}
		this.log.setCaretPosition(0);
	}

	public void show(Window aParent)
	{
		if (this.window == null)
		{
			this.window = new JFrame(ResourceMgr.getString("TxtWindowTitleCompileStatus"));
			this.window.getContentPane().setLayout(new BorderLayout());
			this.window.getContentPane().add(this, BorderLayout.CENTER);
			this.window.setIconImage(ResourceMgr.getImage("workbench").getImage());
			if (!WbManager.getSettings().restoreWindowSize(this.window, ObjectCompilerUI.class.getName()))
			{
				this.window.setSize(500,400);
			}

			if (!WbManager.getSettings().restoreWindowPosition(this.window, ObjectCompilerUI.class.getName()))
			{
				WbSwingUtilities.center(this.window, aParent);
			}
			this.window.addWindowListener(this);
		}
		this.window.show();
		this.startCompile();
	}

	public void windowActivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosed(java.awt.event.WindowEvent e)
	{
	}

	public void windowClosing(java.awt.event.WindowEvent e)
	{
		if (this.worker != null)
		{
			this.worker.interrupt();
			this.compiler.close();
			this.compiler = null;
			this.worker = null;
		}
		WbManager.getSettings().storeWindowPosition(this.window, ObjectCompilerUI.class.getName());
		WbManager.getSettings().storeWindowSize(this.window, ObjectCompilerUI.class.getName());
		this.window.hide();
		this.window.dispose();
	}

	public void windowDeactivated(java.awt.event.WindowEvent e)
	{
	}

	public void windowDeiconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowIconified(java.awt.event.WindowEvent e)
	{
	}

	public void windowOpened(java.awt.event.WindowEvent e)
	{
	}

}