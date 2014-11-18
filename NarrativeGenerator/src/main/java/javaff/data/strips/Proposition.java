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
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class Proposition extends javaff.data.Literal implements GroundFact, SingleLiteral, STRIPSFact, Serializable
{
	
	public Proposition() {
		// TODO Auto-generated constructor stub
	}
	
	public Proposition(PredicateSymbol p)
	{
		name = p;
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
		Proposition p = new Proposition(this.name);
		p.parameters = new ArrayList(this.parameters);
		return p;
	}

	public boolean isTrue(State s) // returns whether this conditions is true
									// is State S
	{
		STRIPSState ss = (STRIPSState) s;
		boolean t = ss.isTrue(this);
		return t;
	}

	public void apply(State s) // carry out the effects of this on State s
	{
		STRIPSState ss = (STRIPSState) s;
		ss.addProposition(this);
	}

	public void applyAdds(State s)
	{
		apply(s);
	}

	public void applyDels(State s)
	{
	}

	public boolean isStatic()
	{
		
		//return false;//
		return name.isStatic();
	}

//	public Set getDeletePropositions()
//	{
//		return super.EmptySet;
//	}
//
//	public Set getAddPropositions()
//	{
//		Set rSet = new SetWrapper();
//		rSet.add(this);
//		return rSet;
//	}

	public GroundFact staticify(Map fValues)
	{
		System.out.println("STATICIFY prop!");
		if (isStatic())
			return TrueCondition.getInstance();
		else
			return this;
	}

	public GroundFact staticifyEffect(Map fValues)
	{
		return this;
	}

//	public Set getConditionalPropositions()
//	{
//		Set rSet = new SetWrapper();
//		rSet.add(this);
//		return rSet;
//	}

	public Set getOperators()
	{
		return super.EmptySet;
	}

	public Set getComparators()
	{
		return super.EmptySet;
	}

	public int hashCode()
	{
		int hash = 7;
		hash = 31 * hash ^ name.hashCode();
		hash = 31 * hash ^ parameters.hashCode();
		return hash;
	}
	
	@Override
	public boolean equals(Object obj)
	{
//		return this.toString().equals(obj.toString());
		if (obj instanceof Proposition)
		{
			Proposition p = (Proposition) obj;
			if (!name.equals(p.name))
				return false;
			return (parameters.equals(p.parameters));
		} 
		else
			return false;
	}
}
