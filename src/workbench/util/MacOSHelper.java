/*
 * MacOSHelper.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2012, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author
 *
 * To contact the author please send an email to: support@sql-workbench.net
 *
 */
package workbench.util;

import java.awt.EventQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import workbench.WbManager;
import workbench.gui.actions.OptionsDialogAction;
import workbench.log.LogMgr;
import workbench.resource.Settings;

/**
 * This class - if running on Mac OS - will install an ApplicationListener
 * that responds to the Apple-Q keystroke (handleQuit).
 *
 * Information taken from
 *
 * http://developer.apple.com/documentation/Java/Reference/1.4.2/appledoc/api/com/apple/eawt/Application.html
 * http://developer.apple.com/samplecode/OSXAdapter/index.html
 *
 * @author Thomas Kellerer
 */
public class MacOSHelper
	implements InvocationHandler
{
	private Object proxy;

	private static boolean isMacOS = System.getProperty("os.name").startsWith("Mac OS");

	public static boolean isMacOS()
	{
		return isMacOS;
	}

	public void installApplicationHandler()
	{
		if (!isMacOS()) return;
		try
		{
			LogMgr.logDebug("MacOSHelper.installApplicationHandler()", "Trying to install Mac OS ApplicationListener");
			Class appClass = Class.forName("com.apple.eawt.Application");
			Object application = appClass.newInstance();
			if (application != null)
			{
				LogMgr.logDebug("MacOSHelper.installApplicationHandler()", "Obtained Application object");

				// Create a dynamic Proxy that can be registered as the ApplicationListener
				Class listener = Class.forName("com.apple.eawt.ApplicationListener");
				this.proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class[] { listener },this);
				Method add = appClass.getMethod("addApplicationListener", new Class[] { listener });
				if (add != null)
				{
					// Register the proxy as the ApplicationListener. Calling events on the Listener
					// will result in calling the invoke method from this class.
					add.invoke(application, this.proxy);
					LogMgr.logInfo("MacOSHelper.installApplicationHandler()", "Mac OS ApplicationListener installed");
				}

				// Now register for the Preferences... menu
				Method enablePrefs = appClass.getMethod("setEnabledPreferencesMenu", boolean.class);
				enablePrefs.invoke(application, Boolean.TRUE);
				LogMgr.logDebug("MacOSHelper.installApplicationHandler()", "Registered for Preferences event");
			}
			else
			{
				LogMgr.logError("MacOSHelper.installApplicationHandler()", "Could not create com.apple.eawt.Application",null);
			}
		}
		catch (Exception e)
		{
			LogMgr.logError("MacOSHelper.installApplicationHandler()", "Could not install ApplicationListener", e);
		}

	}

	@Override
	public Object invoke(Object prx, Method method, Object[] args)
		throws Throwable
	{
		if (prx != proxy)
		{
			LogMgr.logWarning("MacOSHelper.invoke()", "Different Proxy object passed to invoke!");
		}
		try
		{
			String methodName = method.getName();
			LogMgr.logDebug("MacOSHelper.invoke()", "ApplicationEvent [" + methodName + "] received. Arguments: " + getArguments(args));
			if ("handleQuit".equals(methodName))
			{
				WbManager.getInstance().removeShutdownHook();

				boolean handled = Settings.getInstance().getBoolProperty("workbench.osx.quit.sethandled", true);
				if (handled)
				{
					// Apparently MacOS will call System.exit() once this event is triggered (and the "handled" flat was set to true)
					// The following line will prevent WbManager from calling system.exit() as well.
					System.setProperty("workbench.system.doexit", "false");
				}

				setHandled(args[0], handled);

				boolean immediate = Settings.getInstance().getBoolProperty("workbench.osx.quit.immediate", true);
				if (immediate)
				{
					LogMgr.logDebug("MacOSHelper.invoke()", "Calling exitWorkbench()");
					WbManager.getInstance().exitWorkbench(false);
				}
				else
				{
					EventQueue.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							LogMgr.logDebug("MacOSHelper.invoke()", "Calling exitWorkbench()");
							WbManager.getInstance().exitWorkbench(false);
						}
					});
				}
			}
			else if ("handleAbout".equals(methodName))
			{
				WbManager.getInstance().showDialog("workbench.gui.dialogs.WbAboutDialog");
				setHandled(args[0], true);
			}
			else if ("handlePreferences".equals(methodName))
			{
				OptionsDialogAction.showOptionsDialog();
				setHandled(args[0], true);
			}
			else
			{
				LogMgr.logInfo("MacOSHelper.invoke()", "Ignoring unknown event: " + method.getName());
			}
		}
		catch (Throwable e)
		{
			LogMgr.logError("MacOSHelper.invoke()", "Error during callback", e);
			LogMgr.logDebug("MacOSHelper.invoke()", "Arguments: " + getArguments(args));
		}
		return null;
	}

	private String getArguments(Object[] args)
	{
		if (args == null) return "<null>";

		StringBuilder arguments = new StringBuilder();

		for (int i=0; i < args.length; i++)
		{
			if (i > 0) arguments.append(", ");
			arguments.append("args[");
			arguments.append(Integer.toString(i));
			arguments.append("]=");
			if (args[i] == null)
			{
				arguments.append("null");
			}
			else
			{
				arguments.append(args[i].getClass().getName());
				arguments.append(" [");
				arguments.append(args[i].toString());
				arguments.append("]");
			}
		}
		return arguments.toString();
	}

	private void setHandled(Object event, boolean flag)
	{
		if (event == null)
		{
			LogMgr.logError("MacOSHelper.setHandled()", "No event object passed!", null);
			return;
		}
		try
		{
			Method setHandled = event.getClass().getMethod("setHandled", boolean.class);
			setHandled.invoke(event, Boolean.valueOf(flag));
		}
		catch (Exception e)
		{
			LogMgr.logWarning("MacOSHelper.setHandled()", "Could not call setHandled() on class " + event.getClass().getName(), e);
		}
	}

}
