
package javaff.data.strips;

import java.util.Collection;

import javaff.data.Parameter;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class NotEquals extends Equals {
	
	public NotEquals() {
		super.parameters = new SetWrapper<Parameter>();
	}
	
	public NotEquals(Collection<Parameter> params)
	{
		this();
		
		for (Parameter p : params)
			this.addParameter(p);
	}
	
	
	@Override
	protected boolean areEqual() {

		return !super.areEqual();
	}

	@Override
	public boolean isTrue(State s) {
		return this.areEqual();
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer("(!= ");
		for (Parameter p : this.parameters)
		{
			buf.append(p.toString()+ " ");
		}
		buf.append(")");
		
		return buf.toString();
	}
	
	@Override
	public String toStringTyped()
	{
		StringBuffer buf = new StringBuffer("(!= ");
		for (Parameter p : this.parameters)
		{
			buf.append(p.toStringTyped()+ " ");
		}
		buf.append(")");
		
		return buf.toString();
	}

}
