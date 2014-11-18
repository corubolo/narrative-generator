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

package javaff.planning;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javaff.data.Action;
import javaff.data.GroundFact;
import javaff.data.Plan;
import javaff.data.TotalOrderPlan;
import uk.ac.liverpool.narrative.SetWrapper;

public abstract class State implements Cloneable, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 7483127562866427384L;
	public GroundFact goal;
	transient public TotalOrderPlan plan;
	transient public List<GroundFact> sometimes;
	transient public List<GroundFact> always;
	transient public int currentGoal=0;

	public State()
	{
		plan = new TotalOrderPlan();
	}

	public Plan getPlan()
	{
		return plan;
	}

	// public Filter filter = null;

	// public void setFilter(Filter f)
	// {
	// filter = f;
	// }

	// public Filter getFilter()
	// {
	// return filter;
	// }

	// public abstract Set getNextStates(); // get all the next possible states
	// reachable from this tmstate

	public Set<State> getNextStates(Set actions) // get all the states after applying
	// this set of actions
	{
		Set<State> rSet = new SetWrapper<State>();
		Iterator<Action> ait = actions.iterator();
		while (ait.hasNext())
		{
			Action a = ait.next();
			State S = apply(a);
			// we check here for the always!
			boolean al= true;
			if (S.always != null) {
				for (GroundFact f:always) {
					if (!f.isTrue(S)) {
						al = false;
						break;
					}
				}

			}
			if (al)
				rSet.add(S);
		}
		return rSet;
	}

	public abstract State apply(Action a);
	//	{
	//		State s = null;
	//			
	//		s = (State) this.clone();
	//		
	//		
	//		a.apply(s);
	//		return s;
	//	}

	public abstract BigDecimal getHValue();

	public abstract BigDecimal getGValue();

	public boolean goalReached()
	{
		return goal.isTrue(this);
	}

	public abstract Plan getSolution();

	public abstract Set<Action> getActions();

	public boolean checkAvailability(Action a) // put in for invariant checking
	{
		return true;
	}

	public abstract Object clone();
}
