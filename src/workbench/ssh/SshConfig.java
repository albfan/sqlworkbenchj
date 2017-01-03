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

import workbench.util.StringUtil;

/**
 *
 * @author Thomas Kellerer
 */
public class SshConfig
{
  private boolean changed;

  private int sshPort = PortForwarder.DEFAULT_SSH_PORT;
  private String sshHost;
  private String password;
  private String temporaryPassword;
  private String username;
  private int localPort;

  private boolean rewriteURL;

  private String privateKeyFile;

  /**
   * Returns the local port that should be used for port forwarding.
   *
   * @return the local port or 0 if a free port should be chosen automatically.
   */
  public int getLocalPort()
  {
    return localPort;
  }

  public void setLocalPort(int port)
  {
    changed = changed || port != localPort;
    this.localPort = port < 0 ? 0 : port;
  }

  public String getHostname()
  {
    return sshHost;
  }

  public void setHostname(String host)
  {
    if (StringUtil.equalStringOrEmpty(host, sshHost) == false)
    {
      this.changed = true;
      this.sshHost = StringUtil.trimToNull(host);
    }
  }

  public int getSshPort()
  {
    return sshPort;
  }

  public void setSshPort(int port)
  {
    if (port > 0 && port != sshPort)
    {
      changed = true;
      sshPort = port;
    }
  }

  public void setTemporaryPassword(String pwd)
  {
    this.temporaryPassword = pwd;
  }

  public String getPassword()
  {
    if (temporaryPassword != null) return temporaryPassword;
    return password;
  }

  /**
   * Return the password for the SSH host login.
   *
   * @param pwd
   */
  public void setPassword(String pwd)
  {
    if (StringUtil.equalStringOrEmpty(pwd, password) == false)
    {
      this.changed = true;
      this.password = pwd;
    }
  }

  /**
   * Return the username for the SSH host login.
   */
  public String getUsername()
  {
    return username;
  }

  public void setUsername(String user)
  {
    if ("Thomas".equals(user))
    {
      Thread.dumpStack();
    }
    if (StringUtil.equalStringOrEmpty(username, user) == false)
    {
      this.changed = true;
      this.username = StringUtil.trimToNull(user);
    }
  }

  public String getPrivateKeyFile()
  {
    return privateKeyFile;
  }

  public void setPrivateKeyFile(String keyFile)
  {
    if (StringUtil.equalStringOrEmpty(privateKeyFile, keyFile) == false)
    {
      this.changed = true;
      this.privateKeyFile = StringUtil.trimToNull(keyFile);
    }
  }

  /**
   * Controls if the JDBC URL from the profile should automatically be re-written.
   *
   * If true, the database servername from the profile's JDBC URL will be rewritten
   * to point to "localhost" and the port used for the SSH tunnel.
   */
  public boolean getRewriteURL()
  {
    return rewriteURL;
  }

  public void setRewriteURL(boolean doRewrite)
  {
    this.changed = this.changed || this.rewriteURL != doRewrite;
    this.rewriteURL = doRewrite;
  }

  public boolean isValid()
  {
    return StringUtil.isNonBlank(sshHost) && StringUtil.isNonBlank(username) && StringUtil.isNonBlank(password);
  }

  public void resetChanged()
  {
    changed = false;
  }

  public boolean isChanged()
  {
    return changed;
  }

  public void copyFrom(SshConfig config)
  {
    if (config == this) return;
    setHostname(config.getHostname());
    setUsername(config.getUsername());
    setPassword(config.getPassword());
    setRewriteURL(config.getRewriteURL());
    setLocalPort(config.getLocalPort());
    setSshPort(config.getSshPort());
    setPrivateKeyFile(config.getPrivateKeyFile());
  }

  public SshConfig createCopy()
  {
    SshConfig copy = new SshConfig();
    copy.localPort = this.localPort;
    copy.password = this.password;
    copy.privateKeyFile = this.privateKeyFile;
    copy.rewriteURL = this.rewriteURL;
    copy.sshHost = this.sshHost;
    copy.sshPort = this.sshPort;
    copy.username = this.username;
    copy.changed = this.changed;
    return copy;
  }
}
