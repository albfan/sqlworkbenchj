package workbench.util;

/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of Oracle or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Map;

/**
 * Example of using the java.lang.management API to dump stack trace and to perform deadlock detection.
 *
 * @author Mandy Chung
 *
 * This class was adapted from "ThreadMonitor.java" which is part of the JDK's demo applications, found
 * in JDK_HOME/demo/management/FullThreadDump
 */
public class ThreadDumper
{
  private ThreadMXBean tmbean;
  private static String INDENT = "    ";

  /**
   * Constructs a ThreadDumper object to get thread information
   * in the local JVM.
   */
  public ThreadDumper()
  {
    this.tmbean = ManagementFactory.getThreadMXBean();
  }

  /**
   * Returns a String with a full thread dump.
   */
  public String getThreadDump()
  {
    String result = null;
    try
    {
      result = dumpThreadInfo();
    }
    catch (Throwable th)
    {
      result = getStandardDump();
    }
    return result;
  }

  private String getStandardDump()
  {
    Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
    StringBuilder dump = new StringBuilder(all.size() * 100);

    for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet())
    {
      Thread t = entry.getKey();
      dump.append("Thread: \"" + t.getName() + "\", id=" + t.getId() + ", prio=" + t.getPriority() + "\n");
      dump.append("  State: " + t.getState().toString() + "\n");
      for (StackTraceElement element : entry.getValue())
      {
        dump.append("      ");
        dump.append(element.toString() + "\n");
      }
      dump.append('\n');
    }
    return dump.toString();
  }

  private String dumpThreadInfo()
  {
    StringWriter sw = new StringWriter(2000);
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Full Java thread dump");
    long[] tids = tmbean.getAllThreadIds();
    ThreadInfo[] tinfos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
    for (ThreadInfo ti : tinfos)
    {
      printThreadInfo(pw, ti);
    }
    return sw.toString();
  }

  /**
   * Returns the thread dump information with locks.
   */
  private String dumpThreadInfoWithLocks()
  {
    StringWriter sw = new StringWriter(2000);
    PrintWriter pw = new PrintWriter(sw);
    pw.println("Full Java thread dump with locks info");

    ThreadInfo[] tinfos = tmbean.dumpAllThreads(true, true);
    for (ThreadInfo ti : tinfos)
    {
      printThreadInfo(pw, ti);
      LockInfo[] syncs = ti.getLockedSynchronizers();
      printLockInfo(pw, syncs);
    }
    return sw.toString();
  }


  private void printThreadInfo(PrintWriter pw, ThreadInfo ti)
  {
    // print thread information
    printThread(pw, ti);

    // print stack trace with locks
    StackTraceElement[] stacktrace = ti.getStackTrace();
    MonitorInfo[] monitors = ti.getLockedMonitors();
    for (int i = 0; i < stacktrace.length; i++)
    {
      StackTraceElement ste = stacktrace[i];
      pw.println(INDENT + "at " + ste.toString());
      for (MonitorInfo mi : monitors)
      {
        if (mi.getLockedStackDepth() == i)
        {
          pw.println(INDENT + "  - locked " + mi);
        }
      }
    }
    pw.println();
  }

  private void printThread(PrintWriter pw, ThreadInfo ti)
  {
    StringBuilder sb = new StringBuilder("\"" + ti.getThreadName() + "\"" + " Id=" + ti.getThreadId() + " in " + ti.getThreadState());
    if (ti.getLockName() != null)
    {
      sb.append(" on lock=" + ti.getLockName());
    }
    if (ti.isSuspended())
    {
      sb.append(" (suspended)");
    }
    if (ti.isInNative())
    {
      sb.append(" (running in native)");
    }
    pw.println(sb.toString());
    if (ti.getLockOwnerName() != null)
    {
      pw.println(INDENT + " owned by " + ti.getLockOwnerName() + " Id=" + ti.getLockOwnerId());
    }
  }

  private void printMonitorInfo(PrintWriter pw, ThreadInfo ti, MonitorInfo[] monitors)
  {
    pw.println(INDENT + "Locked monitors: count = " + monitors.length);
    for (MonitorInfo mi : monitors)
    {
      pw.println(INDENT + "  - " + mi + " locked at ");
      pw.println(INDENT + "      " + mi.getLockedStackDepth() + " " + mi.getLockedStackFrame());
    }
  }

  private void printLockInfo(PrintWriter pw, LockInfo[] locks)
  {
    pw.println(INDENT + "Locked synchronizers: count = " + locks.length);
    for (LockInfo li : locks)
    {
      pw.println(INDENT + "  - " + li);
    }
    pw.println();
  }

  /**
   * Checks if any threads are deadlocked.
   *
   * If any, thread dump information is returned.
   */
  public String getDeadlockDump()
  {
    StringWriter sw = new StringWriter(2000);
    PrintWriter pw = new PrintWriter(sw);
    long[] tids;
    if (tmbean.isSynchronizerUsageSupported())
    {
      tids = tmbean.findDeadlockedThreads();
      if (tids == null)
      {
        return null;
      }
      pw.println("Deadlock found :-");
      ThreadInfo[] infos = tmbean.getThreadInfo(tids, true, true);
      for (ThreadInfo ti : infos)
      {
        printThreadInfo(pw, ti);
        printLockInfo(pw, ti.getLockedSynchronizers());
        pw.println();
      }
    }
    else
    {
      tids = tmbean.findMonitorDeadlockedThreads();
      if (tids == null)
      {
        return null;
      }
      ThreadInfo[] infos = tmbean.getThreadInfo(tids, Integer.MAX_VALUE);
      for (ThreadInfo ti : infos)
      {
        // print thread information
        printThreadInfo(pw, ti);
      }
    }
    return sw.toString();
  }

}
