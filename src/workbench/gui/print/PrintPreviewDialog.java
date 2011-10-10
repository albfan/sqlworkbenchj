/*
 * PrintPreview.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.print;

import workbench.print.*;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import workbench.WbManager;

import workbench.gui.WbSwingUtilities;
import workbench.gui.components.WbFontChooser;
import workbench.gui.components.WbToolbar;
import workbench.gui.components.WbToolbarButton;
import workbench.log.LogMgr;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;
import workbench.util.WbThread;

/**
 * @author Thomas Kellerer
 */
public class PrintPreviewDialog
	extends JDialog
	implements ActionListener, WindowListener
{
	protected int pageWidth;
	protected int pageHeight;
	private int scale = 100;
	protected WbTablePrinter printTarget;
	protected JComboBox cbZoom;
	private JButton pageSetupButton;
	private JButton printButton;
	private JButton chooseFontButton;
	private JButton closeButton;
	private JButton pageRight;
	private JButton pageLeft;
	private JButton pageDown;
	private JButton pageUp;
	private boolean hasHorizontalPages;
	private JScrollPane scroll;
	private int currentPage = 0;
	private PrintPreviewPanel preview;

	public PrintPreviewDialog(JFrame owner, WbTablePrinter target)
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

		tb.addSeparator();

		this.pageDown = new WbToolbarButton(ResourceMgr.getImage("Down"));
		this.pageDown.addActionListener(this);
		this.pageDown.setEnabled(false);
		tb.add(this.pageDown);

		this.pageUp = new WbToolbarButton(ResourceMgr.getImage("Up"));
		this.pageUp.addActionListener(this);
		this.pageUp.setEnabled(false);
		tb.add(this.pageUp);

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

		showCurrentPage();

		this.preview = new PrintPreviewPanel();
		this.scroll = new JScrollPane(this.preview);
		adjustScrollbar();

		getContentPane().add(scroll, BorderLayout.CENTER);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
	}

	private void selectPrintFont()
	{
		final Font f = WbFontChooser.chooseFont(this.getRootPane(), this.printTarget.getFont(), false, false);
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
//				g.setColor(Color.white);
//				g.fillRect(0, 0, pageWidth, pageHeight);
//				g.setColor(Color.LIGHT_GRAY);
//				Stroke s = g.getStroke();
//				g.setStroke(new BasicStroke(0.2f));
//				g.drawRect((int) pageFormat.getImageableX() - 1, (int) pageFormat.getImageableY() - 1, (int) pageFormat.getImageableWidth() + 1, (int) pageFormat.getImageableHeight() + 1);
//				g.setStroke(s);
				if (this.printTarget.print(g, pageFormat, this.currentPage) == Printable.PAGE_EXISTS)
				{
					this.preview.setPreviewImage(img);
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
			this.preview.setPreviewImage(null);
		}
		finally
		{
			WbSwingUtilities.repaintLater(this);
			WbSwingUtilities.showDefaultCursorOnWindow(this);
		}

		this.pageUp.setEnabled(currentPage > 0);
		this.pageDown.setEnabled(currentPage < printTarget.getNumberOfPages() - 1);

	}

	public void doPrint()
	{
		try
		{
			PrinterJob prnJob = PrinterJob.getPrinterJob();
			prnJob.setPrintable(this.printTarget.getPrintable(), this.printTarget.getPageFormat());
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
			preview.setScale((double)scale / 100d);
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
		if (e.getSource() == this.printButton)
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
		else if (e.getSource() == this.pageUp)
		{
			this.currentPage++;
			this.showCurrentPage();
		}
		else if (e.getSource() == this.pageDown)
		{
			this.currentPage --;
			this.showCurrentPage();
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


}
