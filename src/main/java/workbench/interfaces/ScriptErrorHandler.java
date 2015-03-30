/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.interfaces;

import workbench.sql.ErrorDescriptor;
import workbench.sql.parser.ScriptParser;

/**
 *
 * @author Thomas Kellerer
 */
public interface ScriptErrorHandler
{
  /**
   * Display a prompt to the user and ask on how to proceed with a statement that caused an error.
   *
   * Return values:
   * <ul>
   * <li>WbSwingUtilities.IGNORE_ONE - ignore the current statement and continue</li>
   * <li>WbSwingUtilities.IGNORE_ALL - ignore all subsequent errors without asking any more</li>
   * <li>JOptionPane.CANCEL_OPTION - cancel script execution</li>
   * </ul>
   * @param errorStatementIndex the index of the statement in a script
   * @param errorDetails        the error description
   * @param parser              the parser used to run the script
   * @param selectionOffset     the offset of the actual statement
   * <p>
   * @return
   */
  int scriptErrorPrompt(int errorStatementIndex, ErrorDescriptor errorDetails, ScriptParser parser, int selectionOffset);

}
