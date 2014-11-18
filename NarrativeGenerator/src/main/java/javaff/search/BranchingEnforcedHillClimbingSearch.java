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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;
import uk.ac.liverpool.narrative.Solution;

public class BranchingEnforcedHillClimbingSearch extends BranchingSearch  {

	public BranchingEnforcedHillClimbingSearch() {
		super();
		maxDepth = 1000;
		branchDepth = 4;
	}


	public List<State> search(State start) {
		initialStart = start;
		Set<State> closed;
		closed = new SetWrapper<State>();
		totalBranches = 0;
		// we start by adding the final goal to the list
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

	private void search(State start, Set<State> closed, int bn) {
		if (stop)
			return ;
		totalBranches++;
		LinkedList<State> open = new LinkedList<State>();
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
					totalSolutions++;
				}
				return ;
			}
		}

		int depth = 0;

		open.add(start);
		closed.add(start);
		double bestHValue = start.getHValue().doubleValue();
		if (comp!=null && comp instanceof HCostEvaluator) {
			HCostEvaluator comp2 =  (HCostEvaluator)comp;
			bestHValue= comp2.computeHCost(start) ;
		} 

		while (!open.isEmpty()) // whilst still states to consider
		{
			if (depth >= this.maxDepth)
				return ;
			if (stop)
				return ;
			State s = open.removeFirst(); // get the next one

			if (!(sf instanceof EmptyStateFilter)){
				// collect two good steps that involve the chosen character! take them from the open list
				// could do it only if s is already character c then search for s2 character c with diff heuristic 
				// up to a level
				State s2 = null;

				if (sf.isStateOK(s) && s.plan.getPlanLength()%step == 0) {
					Iterator<State> i = open.iterator();
					while (i.hasNext() && !sf.isStateOK(s2)) {
						s2 = i.next();
					}
					if (s2!=null) {
						if (!silent) {
							System.out.println("*** BRANCH with SF");
							System.out.println(s.plan);
							System.out.println(s2.plan);
						}
						Set<State> cl = (Set<State>) new SetWrapper<State>(closed);
						closed.add(s2);
						open.remove(s2);
						search (s2, cl, bn + 1);
					}					
				}
			} else {
				// // ACTUAL BRANCHING HAPPENS HERE

				if (open.size() > 0) {
					State s2 = open.getFirst();
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
						open.remove(s2);
						search (s2, cl, bn + 1);
					}
				}
			}


			Set<State> successors = s.getNextStates(filter.getActions(s));
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


					// int res = new HValueComparatorTotalCost().
					double hValue = succ.getHValue().doubleValue();

					if (comp!=null && comp instanceof HCostEvaluator) {
						HCostEvaluator comp2 =  (HCostEvaluator)comp;
						hValue = comp2.computeHCost(succ) ;
					} 


					if (Double.compare(hValue, bestHValue) < 0) {
						// if we've found a tmstate with a better heuristic
						// value
						// than the best seen so far

						bestHValue = hValue; // note the new best
						// value
						open.clear();
						open.add(succ);
					} else {
						open.add(succ); // otherwise, add to the open
						// list
					}
				}
			}
			depth++;
		}
		return ;
	}


}
