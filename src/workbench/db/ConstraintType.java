/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public enum ConstraintType
{
	Check,
	Unique,
	ForeignKey,
	PrimaryKey,
	/** Postgres only */
	Exclusion;
}
