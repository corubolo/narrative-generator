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

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javaff.data.strips.PredicateSymbol;
import uk.ac.liverpool.narrative.SetWrapper;

public abstract class Literal implements Fact, Serializable
{
	/**
	 * Return this instead of constantly creating new empty sets for returns.
	 */
	transient protected static final Set EmptySet = new SetWrapper();
	protected PredicateSymbol name;
	protected List<Parameter> parameters = new ArrayList<Parameter>(); // list of Parameters

	
	public void setPredicateSymbol(PredicateSymbol n)
	{
		name = n;
	}

	public PredicateSymbol getPredicateSymbol()
	{
		return name;
	}

	public List<Parameter> getParameters()
	{
		return parameters;
	}
	
	public void setParameters(List<Parameter> params)
	{
		this.parameters = params;
	}

	public void addParameter(Parameter p)
	{
		parameters.add(p);
	}

	public void addParameters(List l)
	{
		parameters.addAll(l);
	}

	public String toString()
	{
		String stringrep = name.toString();
		Iterator i = parameters.iterator();
		while (i.hasNext())
		{
			stringrep = stringrep + " " + i.next();
		}
		return stringrep;
	}

	public String toStringTyped()
	{
		String stringrep = name.toString();
		Iterator i = parameters.iterator();
		while (i.hasNext())
		{
			Parameter o = (Parameter) i.next();
			stringrep += " " + o + " - " + o.type.toString();
		}
		return stringrep;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof Literal)
		{
			Literal p = (Literal) obj;
			return (name.equals(p.name) && parameters.equals(p.parameters) && this
					.getClass() == p.getClass());
		} else
			return false;
	}

	public boolean isStatic()
	{
		return name.isStatic();
	}

	public void setStatic(boolean value)
	{
		this.name.setStatic(value);
	}
	
	public void PDDLPrint(PrintStream p, int indent)
	{
		PDDLPrinter.printToString(this, p, false, true, indent);
	}
	
	public abstract Object clone();
}
