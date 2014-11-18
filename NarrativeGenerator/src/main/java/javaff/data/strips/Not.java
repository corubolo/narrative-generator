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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.PDDLPrinter;
import javaff.data.UngroundFact;
import javaff.data.metric.NamedFunction;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

/**
 * NOT wrapper for a single fact, derived from Fact. NOT itself is derived from this too.
 * @author David Pattison
 *
 */
public class Not implements GroundFact, UngroundFact, STRIPSFact, Serializable
{
	protected static final Set EmptySet = new SetWrapper();
	
	public Fact literal;

	public Not(Fact l)
	{
		if (l instanceof Not)
			this.literal = new Not(((Not)l).literal);
		else
			this.literal = l;
	}
	
	@Override
	public Set<Fact> getFacts()
	{
		Set<Fact> s = new SetWrapper<Fact>(1);
		s.add(this);
		return s;
	}
	
	public Object clone()
	{
		return new Not((Fact) this.literal.clone());
	}

	public void apply(State s)
	{
		STRIPSState ss = (STRIPSState) s;
		ss.removeProposition((Proposition) literal);
	}

	public void applyAdds(State s)
	{
	}

	public void applyDels(State s)
	{
		apply(s);
	}

	public boolean effects(PredicateSymbol ps)
	{
		UngroundFact ue = (UngroundFact) literal;
		return ue.effects(ps);
	}

	public UngroundFact effectsAdd(UngroundFact cond)
	{
		return cond;
	}
//	public Set getAddPropositions()
//	{
//		return NOT.EmptySet;
//	}

//	public Set getDeletePropositions()
//	{
//		Set rSet = new SetWrapper();
//		rSet.add(literal);
//		return rSet;
//	}

	public Set getOperators()
	{
		return Not.EmptySet;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof Not)
		{
			Not n = (Not) obj;
			return (literal.equals(n.literal));
		} else
			return false;
	}

	public GroundFact staticifyEffect(Map fValues)
	{
		return this;
	}

	public int hashCode()
	{
		return literal.hashCode() ^ 2;
	}

	public String toString()
	{
		return "not (" + literal.toString() + ")";
	}

	public String toStringTyped()
	{
		return "not (" + literal.toStringTyped() + ")";
	}

	public void PDDLPrint(java.io.PrintStream p, int indent)
	{
		p.print("(not ");
		PDDLPrinter.printToString(literal, p, false, true, indent);
		p.print(")");
	}

	@Override
	public Set<NamedFunction> getComparators()
	{
		return ((GroundFact)this.literal).getComparators();
	}

//	@Override
//	public Set<Fact> getConditionalPropositions()
//	{
//		return ((GroundFact)this.literal).getConditionalPropositions();
//	}

	@Override
	public boolean isTrue(State s)
	{
		boolean istrue = ((GroundFact)this.literal).isTrue(s);
//		if (this.literal.isStatic() == true && istrue == false)
//			return false;
			
		return !istrue;
	}

	@Override
	public GroundFact staticify(Map fValues)
	{
//		return ((GroundFact)this.literal).staticify(fValues);
		return new Not(((GroundFact)this.literal).staticify(fValues));
//		return this;
	}

	@Override
	public boolean isStatic()
	{
		return ((GroundFact)this.literal).isStatic();
	}

	public void setStatic(boolean value)
	{
		this.literal.setStatic(value);
	}

	@Override
	public Set getStaticPredicates()
	{
		return ((UngroundFact)this.literal).getStaticPredicates();
	}

	@Override
	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		return new Not(((UngroundFact)this.literal).ground(varMap));
	}

	@Override
	public UngroundFact minus(UngroundFact effect)
	{
		return ((UngroundFact)this.literal).minus(effect);
	}
}
