/*
 * PrintPreview.java
 *
 * Created on July 23, 2003, 3:38 PM
 */

package workbench.print;

import java.awt.*;
import java.awt.Stroke;
import java.awt.event.*;
import java.awt.event.WindowListener;
import java.awt.image.*;
import java.util.*;
import java.awt.print.*;
import javax.print.attribute.Attribute;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSizeName;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import workbench.WbManager;
import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;


public class PrintPreview 
	extends JDialog 
	implements ActionListener, WindowListener
{
	protected int pageWidth;
	protected int pageHeight;
	private int scale = 100;
	
	protected TablePrinter printTarget;
	
	protected JComboBox cbZoom;
	private JButton pageSetupButton;
	private JButton printButton;
	private JButton closeButton;
	protected PreviewContainer preview;
	
	public PrintPreview(JFrame owner, TablePrinter target)
	{
		super(owner, ResourceMgr.getString("TxtPrintPreviewWindowTitle"), true);
		
		if (!WbManager.getSettings().restoreWindowSize(this))
		{
			setSize(500, 600);
		}
		WbSwingUtilities.center(this, owner);
		getContentPane().setLayout(new BorderLayout());
		this.printTarget = target;

		WbToolbar tb = new WbToolbar();
		tb.addDefaultBorder();
		
		this.printButton = new WbToolbarButton(ResourceMgr.getString("LabelPrintButton"));
		this.printButton.addActionListener(this);
		tb.add(printButton);
		
		this.pageSetupButton = new WbToolbarButton(ResourceMgr.getString("LabelPageSetupButton"));
		this.pageSetupButton.addActionListener(this);
		tb.add(this.pageSetupButton);
		
		this.closeButton = new WbToolbarButton(ResourceMgr.getString("LabelClose"));
		this.closeButton.addActionListener(this);
		tb.add(this.closeButton);

		String[] scales = { "10%", "25%", "50%", "100%", "150%"};
		this.cbZoom = new JComboBox(scales);
		this.cbZoom.setMaximumSize(this.cbZoom.getPreferredSize());
		this.cbZoom.setEditable(true);
		this.cbZoom.setSelectedItem("100%");
		this.cbZoom.addActionListener(this);
		
		tb.addSeparator();
		tb.add(this.cbZoom);
		getContentPane().add(tb, BorderLayout.NORTH);

		this.addWindowListener(this);
		
		this.preview = new PreviewContainer();
		JScrollPane ps = new JScrollPane(this.preview);
		getContentPane().add(ps, BorderLayout.CENTER);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		updateDisplay();
		setVisible(true);
	}
	
	private void updateDisplay()
	{
		PageFormat pageFormat = this.printTarget.getPageFormat();
		if (pageFormat.getHeight()==0 || pageFormat.getWidth()==0)
		{
			System.err.println("Unable to determine default page size");
			return;
		}
		WbSwingUtilities.showWaitCursor(this);
		this.pageWidth = (int)(pageFormat.getWidth());
		this.pageHeight = (int)(pageFormat.getHeight());
		
		int w = (int)(this.pageWidth * this.scale/100);
		int h = (int)(this.pageHeight* this.scale/100);

		this.preview.removeAll();
		this.doLayout();
		
		int pageIndex = 0;
		try
		{
			while (true)
			{
				
				BufferedImage img = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = img.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, pageWidth, pageHeight);
				g.setColor(Color.LIGHT_GRAY);
				Stroke s = g.getStroke();
				g.setStroke(new BasicStroke(0.2f));
				g.drawRect((int)pageFormat.getImageableX() - 1, (int)pageFormat.getImageableY() - 1, (int)pageFormat.getImageableWidth() + 1, (int)pageFormat.getImageableHeight() + 1);
				g.setStroke(s);
				if (this.printTarget.print(g, pageFormat, pageIndex) != Printable.PAGE_EXISTS)			
					break;

				PagePreview pp = new PagePreview(w, h, img);
				this.preview.add(pp);
				pageIndex++;
			}
		}
		catch (PrinterException e)
		{
			e.printStackTrace();
			System.err.println("Printing error: "+e.toString());
		}
		this.validate();
		this.getContentPane().validate();
		this.getContentPane().repaint();
		this.repaint();
		WbSwingUtilities.showDefaultCursor(this);
	}

	public void doPrint()
	{
		try
		{
			PrinterJob prnJob = PrinterJob.getPrinterJob();
			prnJob.setPrintable(this.printTarget, this.printTarget.getPageFormat());
			prnJob.setPageable(this.printTarget);

			if (!prnJob.printDialog())
				return;
			this.setCursor( Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			prnJob.print();
			this.setCursor( Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			this.dispose();
		}
		catch (PrinterException ex)
		{
			ex.printStackTrace();
			System.err.println("Printing error: "+ex.toString());
		}
	}
	
	private synchronized void showNativePageSetup()
	{
		PrinterJob prnJob = PrinterJob.getPrinterJob();
		PageFormat oldFormat = this.printTarget.getPageFormat();
		PrintUtil.printPageFormat("oldFormat", oldFormat);
		
		PageFormat newFormat = prnJob.pageDialog(oldFormat);
		PrintUtil.printPageFormat("newFormat", oldFormat);
		
		if (!PrintUtil.pageFormatEquals(newFormat, oldFormat))
		{
			System.out.println("settings changed");
			WbManager.getSettings().setPageFormat(newFormat);
			this.printTarget.setPageFormat(newFormat);
			updateDisplay();
		}
	}
	
	private synchronized void showPageSetup()
	{
		PrinterJob prnJob = PrinterJob.getPrinterJob();
		PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
		PageFormat pageFormat = this.printTarget.getPageFormat();
		
		float x = (float)PrintUtil.pointsToMillimeter(pageFormat.getImageableX());
		float y = (float)PrintUtil.pointsToMillimeter(pageFormat.getImageableY());
		float w = (float)PrintUtil.pointsToMillimeter(pageFormat.getImageableWidth());
		float h = (float)PrintUtil.pointsToMillimeter(pageFormat.getImageableHeight());
		attr.add(MediaSizeName.ISO_A4);
		MediaPrintableArea area = new MediaPrintableArea(x,y,w,h,MediaPrintableArea.MM);
		attr.add(area);

		//pageFormat = prnJob.pageDialog(pageFormat);
		//printTarget.setPageFormat(pageFormat);
		PageFormat newFormat = prnJob.pageDialog(attr);
		if (newFormat != null)
		{
			area = (MediaPrintableArea)attr.get(MediaPrintableArea.class);
			
			Attribute[] content = attr.toArray();
			System.out.println("width=" + area.getWidth(MediaPrintableArea.MM));
			MediaSizeName media = null;
			for (int i=0; i < content.length; i++)
			{
				System.out.println("entry=" + i + ",class=" + content[i].getClass().getName());
				if (content[i] instanceof MediaSizeName)
				{
					media = (MediaSizeName)content[i];
					System.out.println("media=" + WbMediaSizeName.getName(media));
				}
			}
			System.out.println("new width (mm) =" + PrintUtil.pointsToMillimeter(newFormat.getWidth()));
			System.out.println("new height (mm) =" + PrintUtil.pointsToMillimeter(newFormat.getHeight()));		
			System.out.println("x = " + PrintUtil.pointsToMillimeter(newFormat.getImageableX()));
			System.out.println("y = " + PrintUtil.pointsToMillimeter(newFormat.getImageableY()));
			System.out.println("iw = " + PrintUtil.pointsToMillimeter(newFormat.getImageableWidth()));
			System.out.println("ih = " + PrintUtil.pointsToMillimeter(newFormat.getImageableHeight()));
			
		}
	}
	
	public void doPageSetup()
	{
		Thread t = new Thread()
		{
			public void run()
			{
				showNativePageSetup();
			}
		};
		t.start();
	}
	
	public void changeZoom()
	{
		WbSwingUtilities.showWaitCursor(this);
		try
		{
			String str = cbZoom.getSelectedItem().toString();
			if (str.endsWith("%")) str = str.substring(0, str.length()-1);
			str = str.trim();
			try
			{
				scale = Integer.parseInt(str);
			}
			catch (NumberFormatException ex)
			{
				return;
			}
			int w = (int)(pageWidth*scale/100);
			int h = (int)(pageHeight*scale/100);

			Component[] comps = this.preview.getComponents();
			for (int k=0; k<comps.length; k++)
			{
				if (!(comps[k] instanceof PagePreview))
					continue;
				PagePreview pp = (PagePreview)comps[k];
				pp.setScaledSize(w, h);
			}
		}
		catch (Throwable th)
		{
			LogMgr.logError("PrintPreview.changeZoom()", "Error when changing the zoom factor", th);
		}
		finally
		{
			this.preview.validate();
			this.preview.doLayout();
			WbSwingUtilities.showDefaultCursor(this);
		}
	}
	
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.printButton)
		{
			this.doPrint();
		}
		else if (e.getSource() == this.pageSetupButton)
		{
			this.doPageSetup();
		}
		else if (e.getSource() == this.cbZoom)
		{
			Thread runner = new Thread()
			{
				public void run()
				{
					changeZoom();
				}
			};
			runner.start();
		}
		else if (e.getSource() == this.closeButton)
		{
			this.saveSettings();
			this.dispose();
		}
	}

	public void saveSettings()
	{
		WbManager.getSettings().storeWindowSize(this);
	}
	
	public void windowActivated(WindowEvent e)
	{
	}
	
	public void windowClosed(WindowEvent e)
	{
	}
	
	public void windowClosing(WindowEvent e)
	{
		this.saveSettings();
	}
	
	public void windowDeactivated(WindowEvent e)
	{
	}
	
	public void windowDeiconified(WindowEvent e)
	{
	}
	
	public void windowIconified(WindowEvent e)
	{
	}
	
	public void windowOpened(WindowEvent e)
	{
	}
	
	class PreviewContainer
		extends JPanel
	{
		protected int H_GAP = 16;
		protected int V_GAP = 10;

		public Dimension getPreferredSize()
		{
			int n = getComponentCount();
			if (n == 0)
				return new Dimension(H_GAP, V_GAP);
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;
			
			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width-H_GAP)/(w+H_GAP), 1);
			int nRow = n/nCol;
			if (nRow*nCol < n)
				nRow++;

			int ww = nCol*(w+H_GAP) + H_GAP;
			int hh = nRow*(h+V_GAP) + V_GAP;
			Insets ins = getInsets();
			return new Dimension(ww+ins.left+ins.right, hh+ins.top+ins.bottom);
		}

		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		public void doLayout()
		{
			Insets ins = getInsets();
			int x = ins.left + H_GAP;
			int y = ins.top + V_GAP;

			int n = getComponentCount();
			if (n == 0)
				return;
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;
			
			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width-H_GAP)/(w+H_GAP), 1);
			int nRow = n/nCol;
			if (nRow*nCol < n)
				nRow++;

			int index = 0;
			for (int k = 0; k<nRow; k++)
			{
				for (int m = 0; m<nCol; m++)
				{
					if (index >= n)
						return;
					comp = getComponent(index++);
					comp.setBounds(x, y, w, h);
					x += w+H_GAP;
				}
				y += h+V_GAP;
				x = ins.left + H_GAP;
			}
		}
	}

	class PagePreview
		extends JPanel
	{
		protected int m_w;
		protected int m_h;
		protected Image m_source;
		protected Image m_img;

		public PagePreview(int w, int h, Image source)
		{
			m_w = w;
			m_h = h;
			m_source= source;
			m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
			m_img.flush();
			setBackground(Color.WHITE);
			setBorder(new MatteBorder(1, 1, 2, 2, Color.BLACK));
		}

		public void setScaledSize(int w, int h)
		{
			m_w = w;
			m_h = h;
			m_img = m_source.getScaledInstance(m_w, m_h, Image.SCALE_SMOOTH);
			repaint();
		}

		public Dimension getPreferredSize()
		{
			Insets ins = getInsets();
			return new Dimension(m_w+ins.left+ins.right, m_h+ins.top+ins.bottom);
		}

		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		public void paint(Graphics g)
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			//g.setColor(Color.LIGHT_GRAY);
			g.drawImage(m_img, 0, 0, this);
			paintBorder(g);
		}
	}
	
}//
