/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 * parts Copyright 2012 Fabio Corubolo
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

import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;
import uk.ac.liverpool.narrative.Solution;

public class BranchingHillClimbingSearch extends BranchingSearch {


	private int branchDepth = 5;

	public BranchingHillClimbingSearch() {
		super();
		maxDepth = 1000;
	}


	public List<State> search(State start) {
		initialStart = start;
		Set<State> closed;
		closed = new SetWrapper<State>();
		totalBranches = 0;
		if (start.sometimes.size()>0) {
			start.sometimes.add(start.goal);
			// and setting the next goal to be the first in the list
			start.currentGoal = 0;
			start.goal = start.sometimes.get(start.currentGoal);
			((STRIPSState)start).setRPG(new RelaxedPlanningGraph(start.getActions(),start.goal));

		}
		search(start, closed,0);
		return solutions;

	}

	@SuppressWarnings("unchecked")
	private void search(State start, Set<State> closed, int bn) {
		LinkedList<State> open = new LinkedList<State>();
		if (stop)
			return ;
		totalBranches++;
		if (start.goalReached()) { // wishful thinkink
			start.currentGoal++;
			//we reached a goal : if sometimes are 0, we go to 1 here... otherwise 
			// if sometimes 1, we added 1; so 1 < 2
			if (start.currentGoal<start.sometimes.size()) {
				// we set the current goal to the next in list, and continue to plan
				start.goal = start.sometimes.get(start.currentGoal);
				((STRIPSState)start).setRPG(new RelaxedPlanningGraph(start.getActions(),start.goal));
				closed = new SetWrapper<State>();
				search(start, closed, bn+1);
				return;
			} else {
				if (isNewSolution(start)){
					solutions.add(start);
					fireNewSolution(new Solution((STRIPSState)initialStart, start));
					if (!silent) 
						System.err.println("newSol!! ?? !");
					totalSolutions++;
				}
				return;
			}
		}

		int depth = 0;

		open.add(start);
		closed.add(start);


		while (!open.isEmpty()) // whilst still states to consider
		{
			if (depth >= this.maxDepth)
				return ;
			if (stop)
				return;
			double bestHValue = Double.MAX_VALUE;
			// best so far

			State s = open.removeFirst(); // get the next one
			Set<State> successors = s.getNextStates(filter.getActions(s));
			ArrayList<State> bestSuccessors = new ArrayList<State>();

			Iterator<State> succItr = successors.iterator();

			while (succItr.hasNext()) {
				State succ = succItr.next(); // next successor
				if (stop)
					return ;
				if (!closed.contains(succ)) {
					closed.add(succ);
					if (succ.goalReached()) { // if we've found a goal tmstate -
						succ.currentGoal++;
						//we reached a goal : if sometimes are 0, we go to 1 here... otherwise 
						// if sometimes 1, we added 1; so 1 < 2
						if (succ.currentGoal<succ.sometimes.size()) {
							// we set the current goal to the next in list, and continue to plan
							succ.goal = succ.sometimes.get(succ.currentGoal);
							((STRIPSState)succ).setRPG(new RelaxedPlanningGraph(succ.getActions(),succ.goal));
							closed = new SetWrapper<State>();
							search(succ, closed, bn+1);
							return;
						} else {
							if (isNewSolution(succ)){
								if (!silent)
									System.out.print("NEW ");
								solutions.add(succ);
								fireNewSolution(new Solution((STRIPSState)initialStart, succ));
							}
							if (!silent) 
							System.out.println("SOL: tsol: "+ totalSolutions + " tbrn " + totalBranches + " actionsN "
									+ s.plan);

							totalSolutions++;
							return ;
						}
					}
					int res = 0;
					double hValue = succ.getHValue().doubleValue();

					if (comp!=null && comp instanceof HCostEvaluator) {
						HCostEvaluator comp2 =  (HCostEvaluator)comp;
						hValue = comp2.computeHCost(succ) ;
					} 

					res = Double.compare(hValue,bestHValue);

					// int res = new HValueComparatorTotalCost().
					if (res < 0) {
						// if we've found a tmstate with a better heuristic
						// value
						// than the best seen so far

						bestHValue = hValue; // note the new best
						// value
						bestSuccessors.clear(); // clear the open list
						bestSuccessors.add(succ); // put this on it
					} else if (res == 0) {
						bestSuccessors.add(succ); // otherwise, add to the open
						// list
					}
				}
			}
			if (bestSuccessors.isEmpty())
				return ;
			else {
				open.clear();
				int n = javaff.JavaFF_mod.generator
						.nextInt(bestSuccessors.size());
				s = bestSuccessors.get(n);
				open.add(s);
				// // ACTUAL BRANCHING HAPPENS HERE
				// TODO: support state filters (now ignored)
				if (bestSuccessors.size() > 1) {
					int n2 = javaff.JavaFF_mod.generator
							.nextInt(bestSuccessors.size());

					State s2 = bestSuccessors.get(n2);
					String na = ""+ s.plan.getActions().get(s.plan.getPlanLength()-1);
					int pl2 = s2.plan.getPlanLength();
					String na2 = ""+ s2.plan.getActions().get(pl2-1);
					if (na!= na2 && 

							( pl2 % step == 0) &&
							bn < branchDepth
							) {
						if (!silent)
						System.out.println("*** BRANCH "
								+ pl2 + "s - " + na +" - VS - " + na2);
						Set<State> cl = (Set<State>) new SetWrapper<State>(closed);
						closed.add(s2);
						bestSuccessors.remove(s2);
						//closed.add(open.pollFirst());
						search (s2, cl, bn + 1);
					}
				}

			}

			depth++;
		}
		return ;
	}


}
