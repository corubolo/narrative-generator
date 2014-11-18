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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.Parameter;
import javaff.data.UngroundProblem;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

public abstract class Operator implements javaff.data.PDDLPrintable
{
	public OperatorName name;
	public List<Variable> params = new ArrayList<Variable>(); // list of Variables

	public String toString()
	{
		String stringrep = name.toString();
		Iterator<Variable> i = params.iterator();
		while (i.hasNext())
		{
			//Variable v = (Variable) i.next();
			stringrep += " " + i.next().toString();
		}
		return stringrep;
	}
	
	public abstract Object clone();

	public String toStringTyped()
	{
		String stringrep = name.toString();
		Iterator<Variable> i = params.iterator();
		while (i.hasNext())
		{
			Variable v = (Variable) i.next();
			stringrep += " " + v.toStringTyped();
		}
		return stringrep;
	}

	public abstract boolean effects(PredicateSymbol ps);

	protected abstract Action ground(Map<Variable, PDDLObject> varMap);

	public abstract Set<Fact> getStaticConditionPredicates();

	public Action ground(List<PDDLObject> values)
	{
		Map<Variable, PDDLObject> varMap = new MapWrapper<Variable, PDDLObject>();
		Iterator<PDDLObject> vit = values.iterator();
		Iterator<Variable> pit = params.iterator();
		while (pit.hasNext())
		{
			Variable v = (Variable) pit.next();
			PDDLObject o = (PDDLObject) vit.next();
			if (o == null)
				System.out.println("Error: null parameter grounding aciton "+ this);
			varMap.put(v, o);
		}
		
		Action a = this.ground(varMap);
		return a;

	}

	public Set<Action> ground(UngroundProblem up)
	{
		Set<ArrayList<PDDLObject>> s = getParameterCombinations(up);
		Set<Action> rSet = new SetWrapper<Action>();
		out : for (ArrayList<PDDLObject> l : s)
		{	
			//This is a hack to stop actions which have no associated parameters being created -- as this causes 
			//a NullPointerException to be thrown during grounding. The alternative to this is to construct a pfile
			//which has one of every object type.
			//TODO Do this in getParameterCombinations()
			for (PDDLObject p : l)
			{
				//if p is null then the pfile has no parameter with which to ground out an action, so ignore it. This can massively reduce the action set for planning!
				if (p == null) {
				System.out.println("NULL");
					continue out;
					}
			}
			
			
			Action groundedAction = ground(l); //ground an individual action using the specified grounded params
		
			//23/8/11 -- Another hack to eliminate any grounded actions whose preconditions contain
			//			 a static fact which is not true in the initial state, rendering it impossible
			for (Fact pc : groundedAction.getPreconditions())
			{
				if (pc.isStatic() && up.initial.contains(pc) == false)
					continue out;
			}
			
			rSet.add(groundedAction); 
		}
		return rSet;
	}

	public Set<ArrayList<PDDLObject>> getParameterCombinations(UngroundProblem up)
	{
		int arraysize = params.size();

		Set<Fact> staticConditions = getStaticConditionPredicates();

		boolean[] set = new boolean[arraysize]; // which of the parameters has
												// been fully set
		Arrays.fill(set, false);

		ArrayList<PDDLObject> combination = new ArrayList<PDDLObject>(arraysize);
		for (int i = 0; i < arraysize; ++i)
		{
			combination.add(null);
		}

		// Set for holding the combinations
		Set<ArrayList<PDDLObject>> combinations = new SetWrapper<ArrayList<PDDLObject>>();
		combinations.add(combination);

		// Loop through ones that must be static
		for (Fact fp : staticConditions)
		{
			Predicate p = (Predicate) fp;
			Set<ArrayList<PDDLObject>> newcombs = new SetWrapper<ArrayList<PDDLObject>>();

			Set<Proposition> sp = up.staticPropositionMap.get(p
					.getPredicateSymbol());

			// Loop through those in the initial tmstate
			for (Proposition prop : sp)
			{
				for (ArrayList<PDDLObject> c : combinations)
				{
					// check its ok to put in
					boolean ok = true;
					Iterator<Parameter> propargit = prop.getParameters().iterator();
					int counter = 0;
					while (propargit.hasNext() && ok)
					{
						PDDLObject arg = (PDDLObject) propargit.next();
						Parameter k = (Parameter) p.getParameters().get(counter);
						int i = params.indexOf(k);
						if (i >= 0 && set[i])
						{
							if (!c.get(i).equals(arg))
								ok = false;
						}
						counter++;
					}
					// if so, duplicate it and put it in and put it in newcombs
					if (ok)
					{
						ArrayList<PDDLObject> sdup = (ArrayList<PDDLObject>) c.clone();
						counter = 0;
						propargit = prop.getParameters().iterator();
						while (propargit.hasNext())
						{
							PDDLObject arg = (PDDLObject) propargit.next();
							Parameter k = (Parameter) p.getParameters().get(
									counter);
							int i = params.indexOf(k);
							if (i >= 0)
							{
								sdup.set(i, arg);
								counter++;
							}
						}
						newcombs.add(sdup);
					}
				}
			}

			combinations = newcombs;

			for (Parameter s : p.getParameters())
			{
				int i = params.indexOf(s);

				if (i >= 0)
					set[i] = true;
			}
		}

		int counter = 0;
		//foreach parameter
		for (Variable p : params)
		{
			//if unset so far (not static?)
			if (!set[counter])
			{
				Set<ArrayList<PDDLObject>> newcombs = new SetWrapper<ArrayList<PDDLObject>>();
				for (ArrayList<PDDLObject> s : combinations)
				{
					Set<PDDLObject> objs = (Set<PDDLObject>) up.typeSets.get(p.getType());
					for (PDDLObject ob : objs)
					{
						ArrayList<PDDLObject> sdup = (ArrayList<PDDLObject>) s.clone();
						sdup.set(counter, ob);
						newcombs.add(sdup);
					}
				}
				combinations = newcombs;

			}
			++counter;
		}
		return combinations;

	}
}
