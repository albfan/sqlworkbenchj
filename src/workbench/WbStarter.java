/*
 * WbStarter.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2017, Thomas Kellerer
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
package workbench;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * This is a wrapper to kick-off the actual WbManager class.
 *
 * It should run with any JDK >= 1.3 as it does not reference any other classes.
 * <br/>
 * This class is compiled separately in build.xml to allow for a different
 * class file version between this class and the rest of the application.
 * Thus a check for the correct JDK version can be done inside the Java code.
 *
 * @author Thomas Kellerer
 */
public class WbStarter
{

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args)
  {
    String version = System.getProperty("java.version", null);
    if (version == null)
    {
      version = System.getProperty("java.runtime.version");
    }

    int versionNr = -1;

    try
    {
      int pos = version.indexOf('.');
      int part1 = -1;
      int part2 = -1;

      if (pos < 0)
      {
        part1 = Integer.parseInt(version);
      }
      else
      {
        part1 = Integer.parseInt(version.substring(0,pos));
        part2 = Integer.parseInt(version.substring(pos + 1, pos + 2)); // we only consider one digit at the second position
      }

      // Before Java 9 the Java version was reported as 1.8 or 1.7
      if (version.startsWith("1"))
      {
        versionNr = part2;
      }
      else
      {
        versionNr = part1;
      }

//      System.out.println("version: " + version + ", versionNr: " + versionNr + ", part1: " + part1 + ", part2: "+ part2);
    }
    catch (Exception e)
    {
      versionNr = -1;
    }

    if (versionNr < 8)
    {
      String error =
        "SQL Workbench/J requires Java 8, but only " + version + " was found\n\n" +
        "If you do have Java 8 installed, please point JAVA_HOME to the location of your Java 8 installation.\n" +
        "or refer to the manual for details on how to specify the Java runtime to be used.";

      System.err.println("*** Cannot run this application ***");
      System.err.println(error);
      try
      {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        // The dummy Frame is needed for pre Java 5 because otherwise
        // the dialog will not appear in the Windows task bar
        Frame dummy = new Frame("SQL Workbench/J - Wrong Java version");
        dummy.setBounds(-2000, -2000, 0, 0);
        dummy.setVisible(true);

        try
        {
          URL iconUrl = WbStarter.class.getClassLoader().getResource("workbench/resource/images/workbench16.png");
          ImageIcon icon = new ImageIcon(iconUrl);
          dummy.setIconImage(icon.getImage());
        }
        catch (Exception e)
        {
          e.printStackTrace();
        }
        final JDialog d = new JDialog(dummy, "SQL Workbench/J - Wrong Java version", true);
        d.getContentPane().setLayout(new BorderLayout(5, 5));
        JButton b = new JButton("Close");
        b.addActionListener(new ActionListener()
        {
          public void actionPerformed(ActionEvent e)
          {
            d.setVisible(false);
            d.dispose();
          }
        });
        JOptionPane pane = new JOptionPane(error, JOptionPane.WARNING_MESSAGE, JOptionPane.DEFAULT_OPTION, (Icon)null, new Object[] { b } );
        d.getContentPane().add(pane, BorderLayout.CENTER);
        d.pack();
        d.setLocationRelativeTo(null);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        d.setVisible(true);
      }
      catch (Throwable e)
      {
        e.printStackTrace();
        // Ignore
      }
      System.exit(1);
    }

    try
    {
      // Do not reference WbManager directly, otherwise a compile
      // of this class will trigger a compile of the other classes, but they
      // should be compiled with a different class file version (see build.xml)
      Class mgr = Class.forName("workbench.WbManager");
      Method main = mgr.getDeclaredMethod("main", new Class[] { String[].class });
      main.invoke(null, new Object[] { args });
    }
    catch (Throwable e)
    {
      e.printStackTrace();
    }
  }

}
