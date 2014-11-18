//
//  LocalSearch.java
//  JavaFF
//
//  Created by Andrew Coles on Thu Jan 31 2008.
//

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
import uk.ac.liverpool.narrative.SetWrapper;

public class LocalSearch extends Search
{
	protected BigDecimal bestHValue;

	protected Map closed;
	protected LinkedList open;
	protected SuccessorSelector selector = null;

	protected int depthBound = 10000;
	protected int restartBound = 10000;

	public LocalSearch(State s)
	{
		this(s, new HValueComparator());
	}

	public LocalSearch(State s, Comparator c)
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

	public void setSuccessorSelector(SuccessorSelector s)
	{
		selector = s;
	}

	public void setDepthBound(int i)
	{
		depthBound = i;
	}

	public void setRestartBound(int r)
	{
		restartBound = r;
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
		return true;
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

		int currentDepth = 0;
		int currentRestarts = 0;

		State bestState = start;
		BigDecimal bestHValue = start.getHValue();
		Map bestClosed = (Map) new MapWrapper(closed);//.clone();

		while (!open.isEmpty()) // whilst still states to consider
		{
			State s = removeNext(); // get the next one

			Set successors = s.getNextStates(filter.getActions(s)); // and find
																	// its
																	// neighbourhood

			Set toChooseFrom = new SetWrapper();

			Iterator succItr = successors.iterator();

			while (succItr.hasNext())
			{
				State succ = (State) succItr.next();

				if (needToVisit(succ))
				{
					if (succ.goalReached())
					{ // if we've found a goal tmstate - return it as the solution
						return succ;
					}
					else
					{
						toChooseFrom.add(succ); // otherwise, add to the set of
												// successors to choose from
					}
				}
			}

			if (!toChooseFrom.isEmpty())
			{ // if the tmstate actually has any successors
				State chosenSuccessor = selector.choose(toChooseFrom); // choose
																		// one

				if (chosenSuccessor.getHValue().compareTo(bestHValue) < 0)
				{ // if this is a new 'best tmstate'
					currentDepth = 0; // reset the depth bound
					open.add(chosenSuccessor); // add this to the open list
					bestState = chosenSuccessor; // and note it's the best
					bestHValue = chosenSuccessor.getHValue();
					bestClosed =  (Map) new MapWrapper(closed);//.clone();
				}
				else
				{
					++currentDepth;
					if (currentDepth < depthBound)
					{
						open.add(chosenSuccessor);
					}
					else
					{
						++currentRestarts;
						if (currentRestarts == restartBound)
						{
							return null;
						}
						currentDepth = 0;
						closed =  (Map) new MapWrapper(bestClosed);//.clone();
						open.add(bestState);
					}
				}
			}

		}
		return null;
	}
}
