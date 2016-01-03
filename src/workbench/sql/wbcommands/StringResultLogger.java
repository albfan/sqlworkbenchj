/*
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2016, Thomas Kellerer.
 *
 * Licensed under a modified Apache License, Version 2.0
 * that restricts the use for certain governments.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://sql-workbench.net/manual/license.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */
package workbench.sql.wbcommands;

import workbench.interfaces.ResultLogger;

import workbench.util.MessageBuffer;

/**
 *
 * @author Thomas Kellerer
 */
public class StringResultLogger
  implements ResultLogger
{
  private MessageBuffer messages = new MessageBuffer();

  @Override
  public void clearLog()
  {
    messages.clear();
  }

  @Override
  public void appendToLog(String msg)
  {
    messages.append(msg);
  }

  @Override
  public void showLogMessage(String msg)
  {
    messages.clear();
    messages.append(msg);
  }

  public MessageBuffer getMessages()
  {
    return messages;
  }

}
