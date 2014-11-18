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

package javaff.search;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javaff.planning.Filter;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.MapWrapper;

public class EnforcedHillClimbingSearch extends Search
{
	protected BigDecimal bestHValue;

	protected Map closed;
	protected LinkedList open;

	public EnforcedHillClimbingSearch(State s)
	{
		this(s, new HValueComparator());
	}

	public EnforcedHillClimbingSearch(State s, Comparator c)
	{
		super(s);
		setComparator(c);

		closed = new MapWrapper();
		open = new LinkedList();
	}

	public void setFilter(Filter f)
	{
		filter = f;
	}

	public State removeNext()
	{

		return (State) (open).removeFirst();
	}

	public boolean needToVisit(State s)
	{
		
	int hash = 7;
		
		hash = 31 * hash ^ ((STRIPSState)s).facts.hashCode();
		Integer Shash = new Integer(hash);
		State D = (State) closed.get(Shash);

		
		if (closed.containsKey(Shash) &&  ((STRIPSState)s).facts.equals(((STRIPSState)D).facts))
			return false;

		closed.put(Shash, s);
				return true; // and return true
	}

	public State search()
	{

		if (start.goalReached())
		{ // wishful thinking
			return start;
		}

		needToVisit(start); // dummy call (adds start to the list of 'closed'
							// states so we don't visit it again

		open.add(start); // add it to the open list
		bestHValue = start.getHValue(); // and take its heuristic value as the
										// best so far

		javaff.JavaFF_mod.logger.fine("Best H "+bestHValue);

		while (!open.isEmpty()) // whilst still states to consider
		{
			State s = removeNext(); // get the next one

			Set successors = s.getNextStates(filter.getActions(s)); // and find
																	// its
																	// neighbourhood

			if (stop)
				return null;
			Iterator succItr = successors.iterator();

			while (succItr.hasNext())
			{
				State succ = (State) succItr.next(); // next successor
				if (stop)
					return null;
				if (needToVisit(succ))
				{
					if (succ.goalReached())
					{ // if we've found a goal tmstate - return it as the
						// solution
						return succ;
					} 
					else if (succ.getHValue().compareTo(bestHValue) < 0)
					{
						// if we've found a tmstate with a better heuristic value
						// than the best seen so far

						bestHValue = succ.getHValue(); // note the new best
														// avlue
						javaff.JavaFF_mod.logger.fine("Best H "+bestHValue);
						open = new LinkedList(); // clear the open list
						open.add(succ); // put this on it
						break; // and skip looking at the other successors
					}
					else
					{
						open.add(succ); // otherwise, add to the open list
					}
				}
			}

		}
		return null;
	}



}
