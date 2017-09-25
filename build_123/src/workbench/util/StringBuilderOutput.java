/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.util;

import workbench.interfaces.TextOutput;

/**
 *
 * @author Thomas Kellerer
 */
public class StringBuilderOutput
  implements TextOutput
{
  private final StringBuilder content;

  public StringBuilderOutput()
  {
    content = new StringBuilder();
  }

  public StringBuilderOutput(int size)
  {
    content = new StringBuilder(size);
  }

  @Override
  public void append(CharSequence text)
  {
    if (text == null) return;
    content.append(text);
  }

  @Override
  public String toString()
  {
    return content.toString();
  }

}
