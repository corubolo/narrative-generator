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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class HillClimbingSearchMod extends BranchingSearch{
	
public HillClimbingSearchMod() {
	super();
	setMaxDepth(1000);
	
}
	
	public List<State> search(State start) {
		Set<State> closed;
		LinkedList<State> open;
		closed = new SetWrapper<State>();
		open = new LinkedList<State>();
		return search(start, 0, open, closed, solutions);
		
	}

	private List<State> search(State start, int i, LinkedList<State> open,
			Set<State> closed, List<State> solutions) {
		
		if (start.goalReached()) { // wishful thinking
			solutions.add(start);
			return solutions;
		}

		int depth = 0;

		open.add(start);
		closed.add(start);

		while (!open.isEmpty()) // whilst still states to consider
		{

			if (depth >= this.maxDepth)
				return solutions;

			BigDecimal bestHValue = new BigDecimal(Integer.MAX_VALUE);
			// best so far

			State s = open.removeFirst(); // get the next one
			Set<State> successors = s.getNextStates(filter.getActions(s));
			ArrayList<State> bestSuccessors = new ArrayList<State>();

			Iterator<State> succItr = successors.iterator();

			while (succItr.hasNext()) {
				State succ = succItr.next(); // next successor

				if (!closed.contains(succ)) {
					closed.add(succ);
					int res = succ.getHValue().compareTo(bestHValue);
					if (succ.goalReached()) { // if we've found a goal tmstate -
						// return it as the
						// solution
						solutions.add(succ);
						return solutions;
					}
					if (res < 0) {
						// if we've found a tmstate with a better heuristic
						// value
						// than the best seen so far

						bestHValue = succ.getHValue(); // note the new best
						// avlue
						javaff.JavaFF_mod.logger.fine("Best H "+bestHValue);
						bestSuccessors.clear(); // clear the open list
						bestSuccessors.add(succ); // put this on it
					} else if (res == 0) {
						bestSuccessors.add(succ); // otherwise, add to the open
						// list
					}
				}
			}
			if (bestSuccessors.isEmpty())
				return solutions;
			else {
				open.clear();
				open.add(bestSuccessors.get(javaff.JavaFF_mod.generator
						.nextInt(bestSuccessors.size())));
			}

			depth++;
		}
		return solutions;
	}
}
