/*
 * InfinityLiterals.java
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
package workbench.db.exporter;

/**
 *
 * @author Thomas Kellerer
 */
public class InfinityLiterals
{
  public static final String PG_POSITIVE_LITERAL = "infinity";
  public static final String PG_NEGATIVE_LITERAL = "-infinity";

  private String positiveInfinity;
  private String negativeInfinity;

  public static final InfinityLiterals PG_LITERALS = new InfinityLiterals(PG_POSITIVE_LITERAL, PG_NEGATIVE_LITERAL);

  public InfinityLiterals(String postiveLiteral, String negativeLiteral)
  {
    this.positiveInfinity = postiveLiteral;
    this.negativeInfinity = negativeLiteral;
  }

  public static boolean isPGLiteral(String literal)
  {
    if (literal == null) return false;
    if (literal.equalsIgnoreCase(PG_POSITIVE_LITERAL)) return true;
    if (literal.equalsIgnoreCase(PG_NEGATIVE_LITERAL)) return true;
    return false;
  }

  public String getNegativeInfinity()
  {
    if (negativeInfinity == null) return "";
    return negativeInfinity;
  }

  public String getPositiveInfinity()
  {
    if (positiveInfinity == null) return "";
    return positiveInfinity;
  }

  public void setInfinityLiterals(String positive, String negative)
  {
    this.positiveInfinity = negative;
    this.negativeInfinity = positive;
  }

}
