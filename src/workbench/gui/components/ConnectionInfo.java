/*
 * ConnectionInfo.java
 *
 * Created on August 9, 2002, 4:26 PM
 */

package workbench.gui.components;

import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import workbench.db.WbConnection;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  tkellerer
 */
public class ConnectionInfo
	extends JTextField
	implements ChangeListener
{
	
	private WbConnection sourceConnection;
	
	/** Creates a new instance of ConnectionInfo */
	public ConnectionInfo(Color aBackground)
	{
		this.setBackground(aBackground);
		this.setEditable(false);
		EmptyBorder border = new EmptyBorder(0, 2, 0, 2);
		this.setBorder(border);
		this.addMouseListener(new TextComponentMouseListener());
		this.setDisabledTextColor(Color.black);
		this.setHorizontalAlignment(JTextField.LEFT);
	}
	
	public void setConnection(WbConnection aConnection)
	{
		if (this.sourceConnection != null)
		{
			this.sourceConnection.removeChangeListener(this);
		}
		this.sourceConnection = aConnection;
		if (this.sourceConnection != null)
		{
			this.sourceConnection.addChangeListener(this);
		}
		this.updateDisplay();
	}
	
	private void updateDisplay()
	{
		if (this.sourceConnection != null)
		{
			this.setText(this.sourceConnection.getDisplayString());
			this.setToolTipText(this.sourceConnection.getDatabaseProductName());
		}
		else
		{
			this.setText(ResourceMgr.getString("TxtNotConnected"));
			this.setToolTipText(null);
		}
		this.setCaretPosition(0);
	}
	public void stateChanged(javax.swing.event.ChangeEvent e)
	{
		if (e.getSource() == this.sourceConnection)
		{
			this.updateDisplay();
		}
	}	
	
}
