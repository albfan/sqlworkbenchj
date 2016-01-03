/*
 * PrintPreview.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at.
 *
 *     http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.print;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import javax.print.attribute.PrintRequestAttributeSet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import workbench.WbManager;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.IconMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbFontChooser;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;

import workbench.util.WbThread;

/**
 * @author Thomas Kellerer
 */
public class PrintPreview
	extends JDialog
	implements ActionListener, WindowListener
{
	public static final String PROP_SHOW_SQLHEADER = "workbench.print.showsql";
	public static final String PROP_ALTERNATE_COLORS = "workbench.print.alternate.colors";
	protected int pageWidth;
	protected int pageHeight;
	private int scale = 100;
	protected TablePrinter printTarget;
	protected JComboBox cbZoom;
	private JButton pageSetupButton;
	private JButton printButton;
	private JButton chooseFontButton;
	private JButton closeButton;
	private JButton pageRight;
	private JButton pageLeft;
	private JButton pageDown;
	private JButton pageUp;
	private JCheckBox showSQL;
	private JCheckBox useAlternateColor;
	private boolean hasHorizontalPages;
	private JScrollPane scroll;
	private PreviewContainer preview;
	private PagePreview pageDisplay;
	private int currentPage = 0;

	public PrintPreview(JFrame owner, TablePrinter target)
	{
		super(owner, ResourceMgr.getString("TxtPrintPreviewWindowTitle"), true);

		if (!Settings.getInstance().restoreWindowSize(this))
		{
			setSize(500, 600);
		}
		if (!Settings.getInstance().restoreWindowPosition(this))
		{
			WbSwingUtilities.center(this, owner);
		}
		getContentPane().setLayout(new BorderLayout());
		this.printTarget = target;

		WbToolbar tb = new WbToolbar();
		tb.addDefaultBorder();

		this.printButton = new WbToolbarButton(ResourceMgr.getString("LblPrintButton"));
		this.printButton.addActionListener(this);
		tb.add(printButton);

		tb.addSeparator();

		this.chooseFontButton = new WbToolbarButton(ResourceMgr.getString("LblSelectPrintFont"));
		this.chooseFontButton.addActionListener(this);
		tb.add(this.chooseFontButton);

		tb.addSeparator();

		this.pageSetupButton = new WbToolbarButton(ResourceMgr.getString("LblPageSetupButton"));
		this.pageSetupButton.addActionListener(this);
		tb.add(this.pageSetupButton);

		boolean showHeader = Settings.getInstance().getBoolProperty(PROP_SHOW_SQLHEADER, true);
		printTarget.setShowHeader(showHeader);

		this.showSQL = new JCheckBox(ResourceMgr.getString("LblPrintShowSQL"));
		this.showSQL.setSelected(showHeader);
		this.showSQL.addActionListener(this);
		tb.add(this.showSQL);

		Color cl = GuiSettings.getAlternateRowColor();
		boolean useAlternate = Settings.getInstance().getBoolProperty(PROP_ALTERNATE_COLORS, false);
		this.useAlternateColor = new JCheckBox(ResourceMgr.getString("LblPrintAltColor"));
		this.useAlternateColor.setSelected(useAlternate);
		this.useAlternateColor.setEnabled(cl != null);
		this.useAlternateColor.addActionListener(this);
		tb.add(this.useAlternateColor);

		tb.addSeparator();

		this.pageDown = new WbToolbarButton(IconMgr.getInstance().getLabelIcon("Down"));
		this.pageDown.addActionListener(this);
		this.pageDown.setEnabled(false);
		tb.add(this.pageDown);

		this.pageUp = new WbToolbarButton(IconMgr.getInstance().getLabelIcon("Up"));
		this.pageUp.addActionListener(this);
		this.pageUp.setEnabled(false);
		tb.add(this.pageUp);


		this.pageLeft = new WbToolbarButton(IconMgr.getInstance().getLabelIcon("Back"));
		this.pageLeft.addActionListener(this);
		this.pageLeft.setEnabled(false);
		tb.add(this.pageLeft);

		this.pageRight = new WbToolbarButton(IconMgr.getInstance().getLabelIcon("Forward"));
		this.pageRight.addActionListener(this);
		this.pageRight.setEnabled(false);
		tb.add(this.pageRight);

		this.hasHorizontalPages = this.printTarget.getPagesAcross() > 1;
		this.pageLeft.setVisible(hasHorizontalPages);
		this.pageLeft.setVisible(hasHorizontalPages);

		tb.addSeparator();

		String[] scales = {"10%", "25%", "50%", "100%", "150%"};
		this.cbZoom = new JComboBox(scales);

		// for some reason the dropdown is extended
		// to fill all available space in the toolbar
		// so I'm restricting the max. size to a sensible value
		Dimension pref = cbZoom.getPreferredSize();
		Dimension d = new Dimension((int) pref.getWidth() + 10, (int) pref.getHeight());
		this.cbZoom.setMaximumSize(d);
		this.cbZoom.setEditable(true);
		this.cbZoom.setSelectedItem("100%");
		this.cbZoom.addActionListener(this);
		tb.add(this.cbZoom);
		tb.addSeparator();

		this.closeButton = new WbToolbarButton(ResourceMgr.getString("LblClose"));
		this.closeButton.addActionListener(this);
		tb.add(this.closeButton);

		getContentPane().add(tb, BorderLayout.NORTH);

		this.addWindowListener(this);

		this.preview = new PreviewContainer();
		this.pageDisplay = new PagePreview();
		this.preview.add(this.pageDisplay);
		showCurrentPage();

		this.scroll = new JScrollPane(this.preview);
		adjustScrollbar();

		getContentPane().add(scroll, BorderLayout.CENTER);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void selectPrintFont()
	{
		final Font f = WbFontChooser.chooseFont(this.getRootPane(), this.printTarget.getFont(), false);
		if (f != null)
		{
			Settings.getInstance().setPrintFont(f);
			printTarget.setFont(f);
			currentPage = 0;
			Thread t = new WbThread("PrintPreview update font")
			{
				@Override
				public void run()
				{
					invalidate();
					showCurrentPage();
					repaint();
				}
			};
			t.start();
		}
	}

	private void adjustScrollbar()
	{
		this.scroll.getVerticalScrollBar().setBlockIncrement((int) printTarget.getPageFormat().getImageableHeight());
		Font f = this.printTarget.getFont();
		FontMetrics fm = this.getFontMetrics(f);
		this.scroll.getVerticalScrollBar().setUnitIncrement(fm.getHeight());
	}

	private void showCurrentPage()
	{
		WbSwingUtilities.showWaitCursorOnWindow(this);

		this.hasHorizontalPages = this.printTarget.getPagesAcross() > 1;
		this.pageLeft.setVisible(hasHorizontalPages);
		this.pageLeft.setVisible(hasHorizontalPages);

		try
		{
			PageFormat pageFormat = this.printTarget.getPageFormat();

			this.pageWidth = (int)pageFormat.getWidth();
			this.pageHeight = (int)pageFormat.getHeight();

			int w = (this.pageWidth * this.scale / 100);
			int h = (this.pageHeight * this.scale / 100);

			try
			{
				BufferedImage img = new BufferedImage(pageWidth, pageHeight, BufferedImage.TYPE_INT_RGB);
				Graphics2D g = img.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, 0, pageWidth, pageHeight);
				g.setColor(Color.LIGHT_GRAY);
				Stroke s = g.getStroke();
				g.setStroke(new BasicStroke(0.2f));
				g.drawRect((int) pageFormat.getImageableX() - 1, (int) pageFormat.getImageableY() - 1, (int) pageFormat.getImageableWidth() + 1, (int) pageFormat.getImageableHeight() + 1);
				g.setStroke(s);
				if (this.printTarget.print(g, pageFormat, this.currentPage) == Printable.PAGE_EXISTS)
				{
					this.pageDisplay.setImage(w, h, img);
				}
			}
			catch (PrinterException e)
			{
				LogMgr.logError("PrintPreview.updateDisplay()", "Error when creating preview", e);
				WbSwingUtilities.showErrorMessage(this, ResourceMgr.getString("MsgPrintPreviewError") + "\n" + e.getMessage());
			}
		}
		catch (OutOfMemoryError e)
		{
			WbManager.getInstance().showOutOfMemoryError();
			this.pageDisplay.setImage(0, 0, null);
		}
		finally
		{
			WbSwingUtilities.repaintLater(this);
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}

		this.pageUp.setEnabled(this.printTarget.getPreviousVerticalPage(this.currentPage) != -1);
		this.pageDown.setEnabled(this.printTarget.getNextVerticalPage(this.currentPage) != -1);

		this.pageLeft.setVisible(hasHorizontalPages);
		this.pageRight.setVisible(hasHorizontalPages);

		if (this.hasHorizontalPages)
		{
			this.pageLeft.setEnabled(this.printTarget.getPreviousHorizontalPage(this.currentPage) != -1);
			this.pageRight.setEnabled(this.printTarget.getNextHorizontalPage(this.currentPage) != -1);
		}
	}

	public void doPrint()
	{
		try
		{
			PrinterJob prnJob = PrinterJob.getPrinterJob();
			prnJob.setPrintable(this.printTarget, this.printTarget.getPageFormat());
			prnJob.setPageable(this.printTarget);

			if (!prnJob.printDialog())
			{
				return;
			}
			this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			prnJob.print();
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			this.dispose();
		}
		catch (PrinterException ex)
		{
			LogMgr.logError("PrintPreview.doPrint()", "Error when printing", ex);
		}
	}

	private boolean pageDialogShowing = false;

	protected void showCrossPlatformPageSetup()
	{
		if (pageDialogShowing)
		{
			return;
		}

		pageDialogShowing = true;
		PrinterJob prnJob = PrinterJob.getPrinterJob();
		PageFormat oldFormat = this.printTarget.getPageFormat();
		PrintRequestAttributeSet attr = PrintUtil.getPrintAttributes(oldFormat);
		PageFormat newFormat = prnJob.pageDialog(attr);
		pageDialogShowing = false;
		applyNewPage(newFormat, oldFormat);
	}

	protected void showNativePageSetup()
	{
		if (pageDialogShowing)
		{
			return;
		}
		pageDialogShowing = true;

		PrinterJob prnJob = PrinterJob.getPrinterJob();
		PageFormat oldFormat = this.printTarget.getPageFormat();
		PageFormat newFormat = prnJob.pageDialog(oldFormat);
		pageDialogShowing = false;
		applyNewPage(newFormat, oldFormat);
	}

	protected void applyNewPage(final PageFormat newFormat, final PageFormat oldFormat)
	{
		if (newFormat == null) return;

		if (!PrintUtil.pageFormatEquals(newFormat, oldFormat))
		{
			Settings.getInstance().setPageFormat(newFormat);
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					printTarget.setPageFormat(newFormat);
					showCurrentPage();
					invalidate();
					preview.validate();
					preview.doLayout();
				}
			});

		}
	}

	public void doPageSetup()
	{
		if (Settings.getInstance().getShowNativePageDialog())
		{
			// the native dialog is shown in it's own thread
			// because otherwise the repainting of the preview window
			// does not work properly
			WbThread t = new WbThread("PageSetup Thread")
			{
				@Override
				public void run()
				{
					showNativePageSetup();
				}
			};
			t.start();
		}
		else
		{
			EventQueue.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					showCrossPlatformPageSetup();
				}
			});
		}
	}

	public void changeZoom()
	{
		WbSwingUtilities.showWaitCursor(this);
		try
		{
			String str = cbZoom.getSelectedItem().toString();
			if (str.endsWith("%"))
			{
				str = str.substring(0, str.length() - 1);
			}
			str = str.trim();
			try
			{
				scale = Integer.parseInt(str);
			}
			catch (NumberFormatException ex)
			{
				return;
			}
			int w = (pageWidth * scale / 100);
			int h = (pageHeight * scale / 100);

			Component[] comps = this.preview.getComponents();
			for (Component comp : comps)
			{
				if (comp instanceof PagePreview)
				{
					PagePreview pp = (PagePreview) comp;
					pp.setScaledSize(w, h);
				}
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

	@Override
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == this.showSQL)
		{
			Settings.getInstance().setProperty(PROP_SHOW_SQLHEADER, showSQL.isSelected());
			this.printTarget.setShowHeader(showSQL.isSelected());
			showCurrentPage();
		}
		else if (e.getSource() == this.useAlternateColor)
		{
			Settings.getInstance().setProperty(PROP_ALTERNATE_COLORS, useAlternateColor.isSelected());
			this.printTarget.setUseAlternateColor(useAlternateColor.isSelected());
			showCurrentPage();
		}
		else if (e.getSource() == this.printButton)
		{
			this.doPrint();
		}
		else if (e.getSource() == this.pageSetupButton)
		{
			this.doPageSetup();
		}
		else if (e.getSource() == this.chooseFontButton)
		{
			this.selectPrintFont();
		}
		else if (e.getSource() == this.cbZoom)
		{
			Thread runner = new WbThread("PrintPreview Zoom thread")
			{
				@Override
				public void run()
				{
					changeZoom();
				}
			};
			runner.start();
		}
		else if (e.getSource() == this.pageRight)
		{
			int newIndex = this.printTarget.getNextHorizontalPage(this.currentPage);
			if (newIndex != -1)
			{
				this.currentPage = newIndex;
			}
			this.showCurrentPage();
		}
		else if (e.getSource() == this.pageLeft)
		{
			int newIndex = this.printTarget.getPreviousHorizontalPage(this.currentPage);
			if (newIndex != -1)
			{
				this.currentPage = newIndex;
			}
			this.showCurrentPage();
		}
		else if (e.getSource() == this.pageUp)
		{
			int newIndex = this.printTarget.getPreviousVerticalPage(this.currentPage);
			if (newIndex != -1)
			{
				this.currentPage = newIndex;
				this.showCurrentPage();
			}
		}
		else if (e.getSource() == this.pageDown)
		{
			int newIndex = this.printTarget.getNextVerticalPage(this.currentPage);
			if (newIndex != -1)
			{
				this.currentPage = newIndex;
				this.showCurrentPage();
			}
		}
		else if (e.getSource() == this.closeButton)
		{
			this.saveSettings();
			this.dispose();
		}
	}

	public void saveSettings()
	{
		Settings.getInstance().storeWindowSize(this);
		Settings.getInstance().storeWindowPosition(this);
	}

	@Override
	public void windowActivated(WindowEvent e)
	{
	}

	@Override
	public void windowClosed(WindowEvent e)
	{
	}

	@Override
	public void windowClosing(WindowEvent e)
	{
		this.saveSettings();
	}

	@Override
	public void windowDeactivated(WindowEvent e)
	{
	}

	@Override
	public void windowDeiconified(WindowEvent e)
	{
	}

	@Override
	public void windowIconified(WindowEvent e)
	{
	}

	@Override
	public void windowOpened(WindowEvent e)
	{
	}

	static class PreviewContainer
		extends JPanel
	{
		protected int H_GAP = 16;
		protected int V_GAP = 10;

		@Override
		public Dimension getPreferredSize()
		{
			int n = getComponentCount();
			if (n == 0)
			{
				return new Dimension(H_GAP, V_GAP);
			}
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;

			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
			int nRow = n / nCol;
			if (nRow * nCol < n)
			{
				nRow++;
			}

			int ww = nCol * (w + H_GAP) + H_GAP;
			int hh = nRow * (h + V_GAP) + V_GAP;
			Insets ins = getInsets();
			return new Dimension(ww + ins.left + ins.right, hh + ins.top + ins.bottom);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		public void doLayout()
		{
			Insets ins = getInsets();
			int x = ins.left + H_GAP;
			int y = ins.top + V_GAP;

			int n = getComponentCount();
			if (n == 0)
			{
				return;
			}
			Component comp = getComponent(0);
			Dimension dc = comp.getPreferredSize();
			int w = dc.width;
			int h = dc.height;

			Dimension dp = getParent().getSize();
			int nCol = Math.max((dp.width - H_GAP) / (w + H_GAP), 1);
			int nRow = n / nCol;
			if (nRow * nCol < n)
			{
				nRow++;
			}

			int index = 0;
			for (int k = 0; k < nRow; k++)
			{
				for (int m = 0; m < nCol; m++)
				{
					if (index >= n)
					{
						return;
					}
					comp = getComponent(index++);
					comp.setBounds(x, y, w, h);
					x += w + H_GAP;
				}
				y += h + V_GAP;
				x = ins.left + H_GAP;
			}
		}
	}

	static class PagePreview
		extends JPanel
	{
		protected int m_w;
		protected int m_h;
		protected Image m_source;
		protected Image m_img;

		PagePreview()
		{
			super();
		}

		PagePreview(int w, int h, Image source)
		{
			super();
			this.setImage(w, h, source);
		}

		public final void setImage(int w, int h, Image source)
		{
			m_w = w;
			m_h = h;
			m_source = source;
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
			m_img.flush();
			repaint();
		}

		@Override
		public Dimension getPreferredSize()
		{
			Insets ins = getInsets();
			return new Dimension(m_w + ins.left + ins.right, m_h + ins.top + ins.bottom);
		}

		@Override
		public Dimension getMaximumSize()
		{
			return getPreferredSize();
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		public void paint(Graphics g)
		{
			if (this.m_img != null)
			{
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
				g.drawImage(m_img, 0, 0, this);
				paintBorder(g);
			}
		}
	}
}
