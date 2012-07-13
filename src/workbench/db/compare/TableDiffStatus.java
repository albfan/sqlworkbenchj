/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db.compare;

/**
 *
 * @author Thomas Kellerer
 */
public enum TableDiffStatus
{
	OK,
	ColumnMismatch,
	ReferenceNotFound,
	TargetNotFound,
	NoPK
}
