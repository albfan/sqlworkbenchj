/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016 Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0 (the "License")
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.ssh;

import java.io.File;
import java.util.Properties;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import workbench.log.LogMgr;

/**
 *
 * @author Thomas Kellerer
 */
public class PortForwarder
{
  public static final int DEFAULT_SSH_PORT = 22;

  private String sshHost;
  private String sshUser;
  private String password;
  private String privateKeyFile;

  private Session session;
  private int localPort;

  public PortForwarder(SshConfig config)
  {
    this.sshHost = config.getHostname();
    this.sshUser = config.getUsername();
    this.password = config.getPassword();
    setPrivateKeyFile(config.getPrivateKeyFile());
  }

  private void setPrivateKeyFile(String keyFile)
  {
    this.privateKeyFile = null;
    if (keyFile != null)
    {
      File f = new File(keyFile);
      if (f.exists())
      {
        privateKeyFile = f.getAbsolutePath();
      }
    }
  }
  /**
   * Forwards a local port to a remote port.
   *
   * @param remoteHost  the remote host (as seen from the SSH host, typically the DB server)
   * @param remotePort  the port of the remote host
   *
   * @return the local port used for forwarding
   */
  public int startFowarding(String remoteDbServer, int remoteDbPort)
    throws JSchException
  {
    return startForwarding(remoteDbServer, remoteDbPort, 0, DEFAULT_SSH_PORT);
  }

  /**
   * Forwards a local port to a remote port.
   *
   * @param remoteHost      the remote host (as seen from the SSH host, typically the DB server)
   * @param remotePort      the port of the remote host
   * @param localPortToUse  the local port to use. If 0 choose a free port
   *
   * @return the local port  used for forwarding
   */
  public int startForwarding(String remoteDbServer, int remoteDbPort, int localPortToUse, int sshPort)
    throws JSchException
  {
    Properties props = new Properties();
    props.put("StrictHostKeyChecking", "no");
    JSch jsch = new JSch();

    long start = System.currentTimeMillis();
    LogMgr.logInfo("PortForwarder.startForwarding()", "Connecting to host: " + sshHost + " using username: " + sshUser);
    if (privateKeyFile != null)
    {
      jsch.addIdentity(privateKeyFile, password);
    }
    session = jsch.getSession(sshUser, sshHost, sshPort);
    if (privateKeyFile == null)
    {
      session.setPassword(password);
    }
    session.setConfig(props);
    session.connect();
    long duration = System.currentTimeMillis() - start;
    LogMgr.logDebug("PortForwarder.startForwarding()", "Connected to host: " + sshHost + " using username: " + sshUser + " (" + duration + "ms)");

    if (localPortToUse < 0) localPortToUse = 0;

    localPort = session.setPortForwardingL(localPortToUse, remoteDbServer, remoteDbPort);
    LogMgr.logInfo("PortForwarder.startForwarding()",
      "Port forwarding established: localhost:"  + localPort + " -> " + remoteDbServer + ":" + remoteDbPort + " through host " + sshHost);

    return localPort;
  }


  public boolean isConnected()
  {
    return session != null && session.isConnected();
  }

  public int getLocalPort()
  {
    return localPort;
  }

  public void close()
  {
    if (session != null && session.isConnected())
    {
      LogMgr.logDebug("PortForwarder.close()", "Disconnecting ssh session to host: " + session.getHost());
      session.disconnect();
    }
    session = null;
    localPort = -1;
  }
}
