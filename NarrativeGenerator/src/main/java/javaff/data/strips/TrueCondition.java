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

import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.PDDLPrinter;
import javaff.data.Parameter;
import javaff.data.UngroundFact;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class TrueCondition implements GroundFact, UngroundFact, STRIPSFact, SingleLiteral
{
	protected static final Set EmptySet = new SetWrapper();
	private static TrueCondition t;

	private TrueCondition()
	{
	}
	
	@Override
	public Set<Fact> getFacts()
	{
		Set<Fact> s = new SetWrapper<Fact>(1);
		s.add(t);
		return s;
	}
	
	public Object clone()
	{
		return t;
	}

	public GroundFact staticify(Map fValues)
	{
		return this;
	}

	public static TrueCondition getInstance()
	{
		if (t == null)
			t = new TrueCondition();
		return t;
	}

	public UngroundFact minus(UngroundFact effect)
	{
		return this;
	}

	public boolean isTrue(State s)
	{
		return true;
	}

	public boolean isStatic()
	{
		return true;
	}

	/**
	 * Stub, does nothing. TrueConditions are always true, so always static.
	 */
	public void setStatic(boolean value)
	{
		
	}

	public Set getStaticPredicates()
	{
		return TrueCondition.EmptySet;
	}

//	public Set getConditionalPropositions()
//	{
//		return TrueCondition.EmptySet;
//	}

	public Set getComparators()
	{
		return TrueCondition.EmptySet;
	}

	public String toString()
	{
		return "()";
	}

	public String toStringTyped()
	{
		return toString();
	}

	public void PDDLPrint(java.io.PrintStream p, int indent)
	{
		PDDLPrinter.printToString(this, p, false, false, indent);
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

//	@Override
//	public Set<SingleLiteral> getAddPropositions()
//	{
//		return null;
//	}
//
//	@Override
//	public Set<SingleLiteral> getDeletePropositions()
//	{
//		return null;
//	}

	@Override
	public Set getOperators()
	{
		return null;
	}

//	@Override
//	public GroundFact staticifyEffect(Map fValues)
//	{
//		return null;
//	}

	@Override
	public boolean effects(PredicateSymbol ps)
	{
		return false;
	}

	@Override
	public UngroundFact effectsAdd(UngroundFact cond)
	{
		return null;
	}

	@Override
	public PredicateSymbol getPredicateSymbol()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPredicateSymbol(PredicateSymbol n)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Parameter> getParameters()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setParameters(List<Parameter> params)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		return this;
	}

}
