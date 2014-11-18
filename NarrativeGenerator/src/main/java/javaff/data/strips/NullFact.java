/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 *
 * (Questions/bug reports now to be sent to Andrew Coles)
 *
 * This file is part of JavaFF.
 * 
 * JavaFF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JavaFF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 * 
 ************************************************************************/

package javaff.data.strips;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.PDDLPrinter;
import javaff.data.UngroundFact;
import javaff.data.metric.NamedFunction;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class NullFact extends Proposition implements UngroundFact, GroundFact, STRIPSFact
{
	protected static final Set EmptySet = new SetWrapper();
	private static NullFact n;

	public NullFact()
	{
		super(new PredicateSymbol());
	}
	
	public NullFact(PredicateSymbol s)
	{
		super(s);
	}
	
	@Override
	public Set<Fact> getFacts()
	{
		Set<Fact> s = new SetWrapper<Fact>(1);
		s.add(this);
		return s;
	}
	
//	@Override
//	public int hashCode()
//	{
//		return this.s.hashCode();
//	}

	public static NullFact getInstance()
	{
		if (n == null)
			n = new NullFact();
		return n;
	}
	
	public Object clone()
	{
		return n;
	}

	public boolean effects(PredicateSymbol ps)
	{
		return false;
	}

	public UngroundFact effectsAdd(UngroundFact cond)
	{
		return cond;
	}

	public GroundFact groundEffect(Map<Variable, PDDLObject> varMap)
	{
		return this;
	}

	public void apply(State s)
	{

	}

	public void applyAdds(State s)
	{

	}

	public void applyDels(State s)
	{

	}

	public GroundFact staticifyEffect(Map fValues)
	{
		return this;
	}

//	public Set getAddPropositions()
//	{
//		return NullEffect.EmptySet;
//	}
//
//	public Set getDeletePropositions()
//	{
//		return NullEffect.EmptySet;
//	}

	public Set getOperators()
	{
		return NullFact.EmptySet;
	}

	public String toString()
	{
		return "()";
	}

	public String toStringTyped()
	{
		return toString();
	}

	public void PDDLPrint(PrintStream p, int indent)
	{
		PDDLPrinter.printToString(this, p, false, false, indent);
	}

	@Override
	public boolean isStatic()
	{
		return true;
	}

	@Override
	public Set<Fact> getStaticPredicates()
	{
		return null;
	}

	@Override
	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		return null;
	}

	@Override
	public UngroundFact minus(UngroundFact effect)
	{
		return null;
	}

	@Override
	public Set<NamedFunction> getComparators()
	{
		return null;
	}

//	@Override
//	public Set<Fact> getConditionalPropositions()
//	{
//		return null;
//	}

	@Override
	public boolean isTrue(State s)
	{
		return false;
	}

	@Override
	public GroundFact staticify(Map fValues)
	{
		return null;
	}
}
