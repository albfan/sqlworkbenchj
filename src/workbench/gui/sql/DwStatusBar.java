/*
 * DwStatusBar.java
 *
 * Created on 3. Juli 2002, 00:08
 */

package workbench.gui.sql;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.DividerBorder;
import workbench.gui.components.TextComponentMouseListener;
import workbench.resource.ResourceMgr;
import workbench.util.StringUtil;

/**
 *
 * @author  workbench@kellerer.org
 */
public class DwStatusBar extends javax.swing.JPanel
{
	private JTextField tfRowCount;
	private JTextField tfStatus;
	JTextField tfMaxRows;
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
		Dimension d = new Dimension(40, FIELD_HEIGHT);
		this.tfRowCount = new JTextField();
		this.tfStatus = new JTextField();
		this.tfMaxRows = new JTextField(8);
		this.tfMaxRows.setEditable(true);
		this.tfMaxRows.setMaximumSize(d);
		this.tfMaxRows.setMargin(new java.awt.Insets(0, 2, 0, 2));
		this.tfMaxRows.setText("0");
		this.tfMaxRows.setToolTipText(ResourceMgr.getDescription("TxtMaxRows"));
		this.tfMaxRows.setHorizontalAlignment(SwingConstants.RIGHT);
		this.tfMaxRows.addMouseListener(new TextComponentMouseListener());

		JPanel p = new JPanel();
		p.setBorder(WbSwingUtilities.EMPTY_BORDER);
		FlowLayout fl = new FlowLayout(FlowLayout.RIGHT);
		fl.setHgap(0);
		fl.setVgap(0);
		p.setLayout(fl);
		p.setMaximumSize(new java.awt.Dimension(85, FIELD_HEIGHT));

		this.setLayout(new java.awt.BorderLayout());
		this.setBorder(new javax.swing.border.EtchedBorder());
		this.setMaximumSize(new java.awt.Dimension(32768, BAR_HEIGHT));
		this.setMinimumSize(new java.awt.Dimension(80, BAR_HEIGHT));
		this.setPreferredSize(null);
		tfRowCount.setEditable(false);
		tfRowCount.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
		tfRowCount.setBorder(WbSwingUtilities.EMPTY_BORDER);
		tfRowCount.setDisabledTextColor(java.awt.Color.black);
		tfRowCount.setMargin(new java.awt.Insets(0, 5, 0, 10));
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
		p.add(tfMaxRows);
		p.add(tfRowCount);
		this.add(p, BorderLayout.EAST);
		//this.add(tfMaxRows, BorderLayout.EAST);
		//this.add(tfRowCount, BorderLayout.EAST);

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
		this.doRepaint();
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
		this.doRepaint();
	}

	public void clearStatusMessage()
	{
		this.tfStatus.setText(this.readyMsg);
	}

	public int getMaxRows()
	{
		return StringUtil.getIntValue(this.tfMaxRows.getText(), 0);
	}
  
  public void selectMaxRowsField()
  {
		this.tfMaxRows.selectAll();
    this.tfMaxRows.requestFocusInWindow();
  }
	
	private void doRepaint()
	{
		this.paint(this.getGraphics());
	}
	
}
