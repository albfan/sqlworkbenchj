/*
 * ConnectionInfo.java
 *
 * Created on August 9, 2002, 4:26 PM
 */

package workbench.gui.components;

import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import workbench.db.WbConnection;

/**
 *
 * @author  tkellerer
 */
public class ConnectionInfo
	extends JTextField
{
	
	/** Creates a new instance of ConnectionInfo */
	public ConnectionInfo(Color aBackground)
	{
		this.setBackground(aBackground);
		this.setEditable(false);
		EmptyBorder border = new EmptyBorder(0, 2, 0, 2);
		this.setBorder(border);
		this.addMouseListener(new TextComponentMouseListener());
		this.setDisabledTextColor(Color.black);
	}
	
	public void setConnection(WbConnection aConnection)
	{
		this.setText(aConnection.getDisplayString());
		this.setToolTipText(aConnection.getDatabaseProductName());
	}
	
}
