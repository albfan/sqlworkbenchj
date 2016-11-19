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
package workbench.util;

/**
 *
 * @author Thomas Kellerer
 */
public enum DurationFormat
{
  /**
   * Return the number of milliseconds as is, with "ms" appended.
   */
  millis,
  
  /**
   * Convert the milliseconds to seconds, including fractional seconds.
   * e.g. 1500ms will be formatted as 1.5s
   */
  seconds,

  /**
   * Use a dynamic format that adjusts to the value of the duration.
   * Small values will be shown in seconds, larger seconds in hours, minutes, seconds.
   * Only valid information is included.
   */
  dynamic
}
