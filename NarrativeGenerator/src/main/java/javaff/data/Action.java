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

package javaff.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.metric.NamedFunction;
import javaff.data.strips.Not;
import javaff.data.strips.OperatorName;
import javaff.planning.State;

public abstract class Action implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -9130259519281066656L;
	public OperatorName name;
	public List<Parameter> parameters = new ArrayList<Parameter>(); // List of PDDLObjects
	transient public BigDecimal cost = new BigDecimal(0);
	
	public Action()
	{
		this.name = new OperatorName("");
		this.parameters = new ArrayList<Parameter>(); // List of PDDLObjects
	}
public Action(String name)
	{
		this.name = new OperatorName(name);
		this.parameters = new ArrayList<Parameter>(); // List of PDDLObjects
	}
	public String toString()
	{
		String stringrep = name.toString();
		Iterator<Parameter> i = parameters.iterator();
		while (i.hasNext())
		{
			stringrep = stringrep + " " + i.next();
		}
		return stringrep;
	}

	public abstract boolean isApplicable(State s);

	public abstract void apply(State s);

	public abstract Set<Fact> getPreconditions();
	
	public abstract Set<Fact> getAddPropositions();

	public abstract Set<Not> getDeletePropositions();

	public abstract Set<NamedFunction> getComparators();

	public abstract Set getOperators();

	public abstract void staticify(Map fValues);
	
	public boolean deletes(Fact f)
	{
		for (Not n : this.getDeletePropositions())
		{
			if (n.literal.equals(f))
			{
				return true;
			}	
		}
		
		return false;
	}
	
	public boolean adds(Fact f)
	{
		return this.getAddPropositions().contains(f);
	}

	public boolean requires(Fact f)
	{
		return this.getPreconditions().contains(f);
//		for (Fact pc : this.getPreconditions())
//		{
//			if (pc instanceof Not)
//			{
//				
//			}
//		}
	}
	
	public boolean equals(Object obj)
	{
		
		if (obj instanceof Action)
		{
			Action a = (Action) obj;
			return (name.equals(a.name) && parameters.equals(a.parameters));
		} 
		else
			return false;
	}

	public int hashCode()
	{
		return name.hashCode() ^ parameters.hashCode();
	}
	
	public abstract Object clone();

	public List<Parameter> getParameters()
	{
		return parameters;
	}

	public void setParameters(List<Parameter> parameters)
	{
		this.parameters = parameters;
	}

	public OperatorName getName()
	{
		return name;
	}

	public void setName(OperatorName name)
	{
		this.name = name;
	}
}
