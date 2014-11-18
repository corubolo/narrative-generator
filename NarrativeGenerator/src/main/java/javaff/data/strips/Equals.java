
package javaff.data.strips;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.Parameter;
import javaff.data.UngroundFact;
import javaff.data.metric.NamedFunction;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class Equals implements GroundFact, UngroundFact//, ADLFact
{
	protected static final Set EmptySet = new SetWrapper();
	
	protected Set<Parameter> parameters;
	
	public Equals()
	{
		this.parameters = new SetWrapper<Parameter>();
	}
	
	public Equals(Parameter a, Parameter b)
	{
		this();
		
		this.addParameter(a);
		this.addParameter(b);
	}
	
	public Equals(Collection<Parameter> params)
	{
		this();
		
		for (Parameter p : params)
			this.addParameter(p);
	}
	
	@Override
	public Object clone()
	{
		Equals e = new Equals();
		for (Parameter p : this.parameters)
		{
			e.addParameter((Parameter) p.clone());
		}
		
		return e;
	}
	
	public void addParameter(Parameter p)
	{
		this.parameters.add(p);
	}

	protected boolean areEqual()
	{
//		return this.a.equals(this.b);
		Iterator<Parameter> it = this.parameters.iterator();
		Parameter f = it.next();
		while (it.hasNext())
		{
			Parameter next = it.next();
			if (f.equals(next) == false)
				return false;
		}
		
		return true;
	}
	
	public int size()
	{
		return this.parameters.size();
	}

	@Override
	public void apply(State s)
	{
		
	}

	@Override
	public void applyAdds(State s)
	{
		
	}

	@Override
	public void applyDels(State s)
	{
		
	}

	@Override
	public Set<NamedFunction> getComparators()
	{
		return EmptySet;
	}

	@Override
	public Set<Fact> getFacts()
	{
		Set<Fact> s = new SetWrapper<Fact>();
		s.add(this);
		return s;
	}

	@Override
	public Set getOperators()
	{
		return EmptySet;
	}

	@Override
	public boolean isTrue(State s)
	{
		return this.areEqual();
	}

	@Override
	public GroundFact staticify(Map fValues)
	{
		return this;
	}

	@Override
	public boolean isStatic()
	{
		return false;
	}
	
	/**
	 * Does nothing
	 */
	public void setStatic(boolean value)
	{
		
	}

	@Override
	public void PDDLPrint(PrintStream p, int indent)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer("(= ");
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
		StringBuffer buf = new StringBuffer("(= ");
		for (Parameter p : this.parameters)
		{
			buf.append(p.toStringTyped()+ " ");
		}
		buf.append(")");
		
		return buf.toString();
	}

	@Override
	public boolean effects(PredicateSymbol ps)
	{
		return false;
	}

	@Override
	public UngroundFact effectsAdd(UngroundFact cond)
	{
		return this;
	}

	@Override
	public Set<Fact> getStaticPredicates()
	{
		return EmptySet;
	}

	@Override
	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		Equals equals = new Equals();
		for (Parameter p : this.parameters)
		{
			PDDLObject o = varMap.get(p);
			equals.addParameter(o);
		}
		
		return equals;
	}

	@Override
	public UngroundFact minus(UngroundFact effect)
	{
		return this;
	}

}
