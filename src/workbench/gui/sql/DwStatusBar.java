/*
 * DwStatusBar.java
 *
 * Created on 3. Juli 2002, 00:08
 */

package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import workbench.gui.components.DividerBorder;
import workbench.resource.ResourceMgr;

/**
 *
 * @author  sql.workbench@freenet.de
 */
public class DwStatusBar extends javax.swing.JPanel
{
	private JTextField tfRowCount;
	private JTextField tfStatus;
	private String readyMsg;

	private static final int BAR_HEIGHT = 22;
	private static final int FIELD_HEIGHT = 18;

	/** Creates new form DwStatusBar */
	public DwStatusBar()
	{
		initComponents();
	}

	private void initComponents()
	{
		this.tfRowCount = new JTextField();
		this.tfStatus = new JTextField();

		this.setLayout(new java.awt.BorderLayout());
		this.setBorder(new javax.swing.border.EtchedBorder());
		this.setMaximumSize(new java.awt.Dimension(32768, BAR_HEIGHT));
		this.setMinimumSize(new java.awt.Dimension(80, BAR_HEIGHT));
		this.setPreferredSize(null);
		tfRowCount.setEditable(false);
		tfRowCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
		tfRowCount.setBorder(null);
		tfRowCount.setDisabledTextColor(java.awt.Color.black);
		tfRowCount.setMargin(new java.awt.Insets(0, 5, 0, 10));
		Dimension d = new Dimension(40, FIELD_HEIGHT);
		tfRowCount.setMaximumSize(d);
		tfRowCount.setMinimumSize(d);
		tfRowCount.setPreferredSize(d);
		tfRowCount.setAutoscrolls(false);
		tfRowCount.setEnabled(false);

		tfStatus.setHorizontalAlignment(JTextField.LEFT);
		tfStatus.setBorder(new DividerBorder(DividerBorder.RIGHT));
		tfStatus.setDisabledTextColor(java.awt.Color.black);
		tfStatus.setMaximumSize(new java.awt.Dimension(32768, FIELD_HEIGHT));
		tfStatus.setMinimumSize(new java.awt.Dimension(80, FIELD_HEIGHT));
		tfStatus.setPreferredSize(null);
		tfStatus.setAutoscrolls(false);
		tfStatus.setEnabled(false);
		tfStatus.setEditable(false);

		this.add(tfStatus, BorderLayout.CENTER);
		this.add(tfRowCount, BorderLayout.EAST);

		this.readyMsg = ResourceMgr.getString(ResourceMgr.STAT_READY);
		this.clearStatusMessage();
	}

	public void setRowcount(int maxRow)
	{
		this.tfRowCount.setText(Integer.toString(maxRow));
	}

	public void clearRowcount()
	{
		this.tfRowCount.setText("");
	}

	public void setStatusMessage(String aMsg)
	{
		if (aMsg == null || aMsg.length() == 0)
		{
			this.clearStatusMessage();
		}
		else
		{
			this.tfStatus.setText(aMsg);
		}
	}

	public void clearStatusMessage()
	{
		this.tfStatus.setText(this.readyMsg);
	}

}
