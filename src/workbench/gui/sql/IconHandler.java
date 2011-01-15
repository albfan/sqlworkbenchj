/*
 * IconHandler.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2011, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.gui.sql;

import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;
import workbench.gui.WbSwingUtilities;
import workbench.interfaces.TextChangeListener;
import workbench.log.LogMgr;
import workbench.resource.GuiSettings;
import workbench.resource.ResourceMgr;
import workbench.resource.Settings;

/**
 *
 * @author Thomas Kellerer
 */
public class IconHandler
	implements PropertyChangeListener, TextChangeListener
{
	private SqlPanel client;

	private ImageIcon fileIcon;
	private ImageIcon fileModifiedIcon;
	private ImageIcon cancelIcon;
	private ImageIcon loadingIcon;
	private boolean textModified;

	public IconHandler(SqlPanel panel)
	{
		client = panel;
		client.getEditor().addTextChangeListener(this);
		Settings.getInstance().addPropertyChangeListener(this, Settings.PROPERTY_ANIMATED_ICONS);
	}

	protected void dispose()
	{
		Settings.getInstance().removePropertyChangeListener(this);
	}

	protected void flush()
	{
		if (cancelIcon != null) cancelIcon.getImage().flush();
		if (loadingIcon != null) loadingIcon.getImage().flush();
		if (fileIcon != null) fileIcon.getImage().flush();
		if (fileModifiedIcon != null) fileModifiedIcon.getImage().flush();
	}

	public void removeIcon()
	{
		if (client.isBusy()) return;
		showIconForTab(null);
	}

	public void showFileIcon()
	{
		if (client.isBusy()) return;
		showIconForTab(getFileIcon());
	}

	public void showCancelIcon()
	{
		showIconForTab(this.getCancelIndicator());
		if (loadingIcon != null) loadingIcon.getImage().flush();
	}

	private ImageIcon getLoadingIndicator()
	{
		if (this.loadingIcon == null)
		{
			if (GuiSettings.getUseAnimatedIcon())
			{
				String name = Settings.getInstance().getProperty("workbench.gui.animatedicon.name", "loading");
				this.loadingIcon = ResourceMgr.getPicture(name);
				if (loadingIcon == null)
				{
					this.loadingIcon = ResourceMgr.getPicture("loading");
				}
			}
			else
			{
				this.loadingIcon = ResourceMgr.getPicture("loading-static");
			}
		}
		return this.loadingIcon;
	}


	protected ImageIcon getCancelIndicator()
	{
		if (this.cancelIcon == null)
		{
			if (GuiSettings.getUseAnimatedIcon())
			{
				this.cancelIcon = ResourceMgr.getPicture("cancelling");
			}
			else
			{
				this.cancelIcon = ResourceMgr.getPicture("cancelling-static");
			}
		}
		return this.cancelIcon;
	}

	protected ImageIcon getFileIcon()
	{
		ImageIcon icon = null;
		if (textModified)
		{
			if (this.fileModifiedIcon == null)
			{
				this.fileModifiedIcon = ResourceMgr.getPicture("file-modified-icon");
			}
			icon = this.fileModifiedIcon;
		}
		else
		{
			if (this.fileIcon == null)
			{
				this.fileIcon = ResourceMgr.getPicture("file-icon");
			}
			icon = this.fileIcon;
		}

		return icon;
	}

	protected void showIconForTab(final ImageIcon icon)
	{
		Container parent = client.getParent();
		if (parent instanceof JTabbedPane)
		{
			final JTabbedPane tab = (JTabbedPane)parent;
			final int index = tab.indexOfComponent(client);
			if (index < 0) return;

			final Icon oldIcon = tab.getIconAt(index);
			if (icon == null && oldIcon == null) return;
			if (icon != oldIcon)
			{
				WbSwingUtilities.invoke(new Runnable()
				{
					@Override
					public void run()
					{
						tab.setIconAt(index, icon);
					}
				});
			}
		}
	}

	private Runnable hideBusyRunnable = new Runnable()
	{
		public void run()
		{
			_showBusyIcon(false);
		}
	};

	private Runnable showBusyRunnable = new Runnable()
	{
		public void run()
		{
			_showBusyIcon(true);
		}
	};

	public void showBusyIcon(boolean show)
	{
		if (show)
		{
			WbSwingUtilities.invoke(showBusyRunnable);
		}
		else
		{
			WbSwingUtilities.invoke(hideBusyRunnable);
		}
	}

	private void _showBusyIcon(boolean show)
	{
		Container parent = client.getParent();
		if (parent instanceof JTabbedPane)
		{
			final JTabbedPane tab = (JTabbedPane)parent;
			int index = tab.indexOfComponent(client);
			if (index >= 0 && index < tab.getTabCount())
			{
				try
				{
					if (show)
					{
						tab.setIconAt(index, getLoadingIndicator());
					}
					else
					{
						if (client.hasFileLoaded())
						{
							tab.setIconAt(index, getFileIcon());
						}
						else
						{
							tab.setIconAt(index, null);
						}
						if (GuiSettings.getUseAnimatedIcon())
						{
							// flushing the animated icons also stops the thread that
							// is used for the animation. If this is not done it will still
							// "animate" in the background (at least on older JDKs) and thus
							// degrade performance
							// For a static icon this is not necessary, actually not flushing
							// the static icon improves performance when it's re-displayed
							if (this.loadingIcon != null) this.loadingIcon.getImage().flush();
							if (this.cancelIcon != null) this.cancelIcon.getImage().flush();
						}
					}
				}
				catch (Throwable th)
				{
					LogMgr.logWarning("SqlPanel.setBusy()", "Error when setting busy icon!", th);
				}
				tab.validate();
				tab.repaint();
			}
		}
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		String prop = evt.getPropertyName();
		if (prop == null) return;

		if (evt.getSource() == Settings.getInstance() && prop.equals(Settings.PROPERTY_ANIMATED_ICONS))
		{
			if (this.cancelIcon != null)
			{
				this.cancelIcon.getImage().flush();
				this.cancelIcon = null;
			}
			if (this.loadingIcon != null)
			{
				this.loadingIcon.getImage().flush();
				this.loadingIcon = null;
			}
		}
	}

	public void textStatusChanged(boolean modified)
	{
		this.textModified = modified;
		// Make sure the icon for the file is updated to reflect
		// the modidified status
		if (client.hasFileLoaded()) showFileIcon();
	}

}
