/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package workbench.db;

/**
 *
 * @author Thomas Kellerer
 */
public class ConstraintDefinition
{
	private String constraintName;
	private Boolean deferrable;
	private Boolean initiallyDeferred;
	private String comment;
	private boolean isSystemName;
	private ConstraintType type;

	public ConstraintDefinition(String name)
	{
		this.constraintName = name;
	}

	public static ConstraintDefinition createUniqueConstraint(String name)
	{
		ConstraintDefinition cons = new ConstraintDefinition(name);
		cons.setConstraintType(ConstraintType.Unique);
		return cons;
	}

	public ConstraintType getConstraintType()
	{
		return type;
	}

	public void setConstraintType(ConstraintType type)
	{
		this.type = type;
	}

	public String getConstraintName()
	{
		return constraintName;
	}

	public void setConstraintName(String constraintName)
	{
		this.constraintName = constraintName;
	}

	public boolean isDeferred()
	{
		if (deferrable == null) return false;
		return deferrable.booleanValue();
	}
	
	public Boolean isDeferrable()
	{
		return deferrable;
	}

	public void setDeferrable(boolean deferrable)
	{
		this.deferrable = Boolean.valueOf(deferrable);
	}

	public Boolean isInitiallyDeferred()
	{
		return initiallyDeferred;
	}

	public void setInitiallyDeferred(boolean initiallyDeferred)
	{
		this.initiallyDeferred = Boolean.valueOf(initiallyDeferred);
	}

	public String getComment()
	{
		return comment;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	public void setIsSystemName(boolean flag)
	{
		this.isSystemName = flag;
	}

	public boolean isSystemName()
	{
		return this.isSystemName;
	}

	@Override
	public String toString()
	{
		return (type != null ? type.toString() + " constraint: " : "") + getConstraintName();
	}

	@Override
	public int hashCode()
	{
		int hash = 3;
		hash = 79 * hash + (this.constraintName != null ? this.constraintName.hashCode() : 0);
		hash = 79 * hash + (this.type != null ? this.type.hashCode() : 0);
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		final ConstraintDefinition other = (ConstraintDefinition) obj;
		if ((this.constraintName == null) ? (other.constraintName != null) : !this.constraintName.equals(other.constraintName))
		{
			return false;
		}
		if (this.type != other.type)
		{
			return false;
		}
		return true;
	}


}
