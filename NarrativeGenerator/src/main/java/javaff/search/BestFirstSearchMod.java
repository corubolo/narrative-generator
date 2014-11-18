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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javaff.data.Plan;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class BestFirstSearchMod  extends BranchingSearch {


	public BestFirstSearchMod() {
		super();
		setComp(new HValueComparator());
	}


	public List<State> search(State start) {
		Set<State> closed;
		TreeSet<State> open;
		closed = new SetWrapper<State>();
		open = new TreeSet<State>(comp);
		// we start by adding the final goal to the list
		if (start.sometimes.size()>0) {
			start.sometimes.add(start.goal);
			// and setting the next goal to be the first in the list
			start.currentGoal = 0;
			start.goal = start.sometimes.get(start.currentGoal);
			((STRIPSState)start).setRPG(new RelaxedPlanningGraph(start.getActions(),start.goal));

		}
		search(start, 0, open, closed);
		return solutions;
	}

	public void search(State start, int nodeCount, TreeSet<State> open,
			Set<State> closed) {
		open.add(start);
		while (!open.isEmpty()) {
			State s = open.pollFirst();

			if (!closed.contains(s)) {
				closed.add(s);
				++nodeCount;
				if (s.goalReached()) {
					s.currentGoal++;
					//we reached a goal : if sometimes are 0, we go to 1 here... otherwise 
					// if sometimes 1, we added 1; so 1 < 2
					if (s.currentGoal<s.sometimes.size()) {
						// we set the current goal to the next in list, and continue to plan
						s.goal = s.sometimes.get(s.currentGoal);
						((STRIPSState)s).setRPG(new RelaxedPlanningGraph(s.getActions(),s.goal));
						closed = new SetWrapper<State>();
						open = new TreeSet<State>(comp);
						search(s, nodeCount, open , closed);
						return;
					} else {
						if (!solutions.contains(s))
							solutions.add(s);
						return ;
					}
				}
				if (open.size() > 0) {
					Plan p1 = s.plan;
					State s2 = open.first();
					Plan p2 = s2.plan;
					if (!silent) {
						System.out.println(p1.getActions());
						System.out.println(p2.getActions());
						System.out.println("1 " + s.getHValue() + " "
								+ s.getPlan().getActions().size() + " " + s.plan.getActions().get(s.plan.getPlanLength()-1));
						System.out.println("2 " + s2.getHValue() + " "
								+ s2.getPlan().getActions().size() + " " + s2.plan.getActions().get(s2.plan.getPlanLength()-1));
					}
				}



				open.addAll(s.getNextStates(filter.getActions(s)));
				if (!silent)
					System.out.println(nodeCount + " " + s.plan);

			}
		}
		return;
	}

}
