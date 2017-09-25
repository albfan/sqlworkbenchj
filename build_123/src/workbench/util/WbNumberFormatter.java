/*
 * WbNumberFormatter.java
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
package workbench.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author Thomas Kellerer
 */
public class WbNumberFormatter
{
  private final DecimalFormat formatter;
  private final char decimalSymbol;
  private final int maxDigits;
  private final boolean fixedDigits;
  private final boolean alwaysUseFormatter;

  public WbNumberFormatter(char sep)
  {
    this(0, sep);
  }

  public WbNumberFormatter(int maxDigits, char sep)
  {
    this(maxDigits, sep, false);
  }

  public WbNumberFormatter(int maxDigits, char decimal, boolean fixedDigits)
  {
    char filler = fixedDigits ? '0' : '#';
    decimalSymbol = decimal;
    String pattern = StringUtil.padRight("0.", maxDigits > 0 ? maxDigits + 2 : 22, filler);
    DecimalFormatSymbols symb = new DecimalFormatSymbols();
    symb.setDecimalSeparator(decimal);
    formatter = new DecimalFormat(pattern, symb);
    this.maxDigits = maxDigits;
    this.fixedDigits = fixedDigits;
    alwaysUseFormatter = false;
  }

  public WbNumberFormatter(String pattern, char decimal, char groupSymbol)
  {
    decimalSymbol = decimal;
    DecimalFormatSymbols symb = new DecimalFormatSymbols();
    symb.setDecimalSeparator(decimalSymbol);
    symb.setGroupingSeparator(groupSymbol);
    formatter = new DecimalFormat(pattern, symb);
    maxDigits = 0;
    fixedDigits = false;
    alwaysUseFormatter = true;
  }

  public char getDecimalSymbol()
  {
    return decimalSymbol;
  }

  /**
   * Returns a pattern suitable to be applied for a DecimalFormat instance.
   *
   * @return  the pattern;
   */
  public String toFormatterPattern()
  {
    return formatter.toPattern();
  }

  private boolean isInteger(Number n)
  {
    return (n instanceof Integer
      || n instanceof Long
      || n instanceof Short
      || n instanceof BigInteger
      || n instanceof AtomicInteger
      || n instanceof AtomicLong);
  }

  public String format(Number value)
  {
    if (value == null) return "";

    String formatted = null;

    if (alwaysUseFormatter)
    {
      synchronized (this.formatter)
      {
        formatted =  formatter.format(value);
      }
    }
    else if (value instanceof BigDecimal)
    {
      formatted = format((BigDecimal)value);
    }
    else if (isInteger(value))
    {
      formatted = value.toString();
    }
    else
    {
      synchronized (this.formatter)
      {
        formatted = formatter.format(value);
      }
    }

    return formatted;
  }

  /**
   * Format a BigDecimal value according to the settings.
   *
   * DecimalFormat does not preserve the real number of digits for a BigDecimal.
   * If no maximum digits are defineed, this method will return a string with
   * all digits that are available (essentially the result of BigDecimal.toPlainString())
   *
   * @param value  the value to format
   * @return the formatted value
   */
  private String format(BigDecimal value)
  {
    String display = value.toPlainString();
    if (maxDigits <= 0 && decimalSymbol == '.') return display; // no maximum given

    int scale = value.scale();
    if (scale <= 0 && !fixedDigits) return display; // no decimal digits, nothing to do

    if (scale > maxDigits && maxDigits > 0)
    {
      BigDecimal rounded = value.setScale(this.maxDigits, RoundingMode.HALF_UP);
      display = rounded.toPlainString();
    }

    if (fixedDigits && maxDigits > 0)
    {
      int pos = display.lastIndexOf('.');
      int digits;
      if (pos == -1)
      {
        digits = 0;
      }
      else
      {
        digits = display.length() - pos - 1;
      }

      if (digits < maxDigits)
      {
        int num = (maxDigits - digits);
        StringBuilder sb = new StringBuilder(display.length() + num);
        sb.append(display);
        if (digits == 0)
        {
          sb.append(decimalSymbol);
        }

        for (int i=0; i < num; i++)
        {
          sb.append('0');
        }
        if (decimalSymbol != '.' && digits > 0)
        {
          sb.setCharAt(pos, decimalSymbol);
        }
        return sb.toString();
      }
    }

    if (decimalSymbol != '.' )
    {
      // this is a little bit faster than using a StringBuilder and setCharAt()
      // and as numbers will not contain any two byte characters theres nothing
      // to worry about encoding.
      int pos = display.lastIndexOf('.');
      char[] ca = display.toCharArray();
      ca[pos] = decimalSymbol;
      display = new String(ca);
    }

    return display;
  }

}
