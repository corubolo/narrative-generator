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

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javaff.data.Action;
import javaff.data.Plan;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;
import uk.ac.liverpool.narrative.Solution;

public class BranchingBestFirstSearch extends BranchingSearch {

	// Parameters that influence branch generation
	private boolean singleClosedList = false;
	// also try true and step 3
	private boolean clearOpenList = true;
	public int deltaHV =2; // 1

	public int deltaPL =1; // 1

	public BranchingBestFirstSearch() {
		super();
		setComp(new HValueComparator());
		maxDepth = 80;
		branchDepth=4;

	}



	public List<State> search(State start) {
		initialStart = (State) start.clone();
		Set<State> closed;
		TreeSet<State> open;
		closed = new SetWrapper<State>();
		open = new TreeSet<State>(comp);

		totalBranches = 0;

		// we start by adding the final goal to the list
		if (start.sometimes.size()>0) {
			start.sometimes.add(start.goal);
			// and setting the next goal to be the first in the list
			start.currentGoal = 0;
			start.goal = start.sometimes.get(start.currentGoal);
			((STRIPSState)start).setRPG(new RelaxedPlanningGraph(start.getActions(),start.goal));

		}
		search(start, 0,0, open, closed);
		return solutions;
	}

	@SuppressWarnings("unchecked")
	public void search(State start, int nodeCount, int bn,  TreeSet<State> open,
			Set<State> closed) {		
		if (stop)
			return;
		int steps = 0;
		totalBranches++;
		open.add(start);
		while (!open.isEmpty()) {
			if (stop)
				return;
			State s = open.pollFirst();

			if (!closed.contains(s)) {
				closed.add(s);
				++nodeCount;
				if (steps >= maxDepth){ 
					System.err.println("Max steps reaced: "+ steps);
					return;
				}
				steps++;
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
						search(s, nodeCount,bn+1, open , closed);
						return;
					} else {
						if (isNewSolution(s)){ 
							if (!silent)
								System.out.print("NEW ");
							solutions.add(s);
							fireNewSolution(new Solution((STRIPSState)initialStart, s));
						}
						if (!silent)
							System.out.println("SOL: tsol: "+ totalSolutions + " tbrn " + totalBranches + " actionsN " + nodeCount
									+ s.plan);
						totalSolutions++;
						return;
					}
				}

				// Branching part
				// if we are not just starting and if we have options
				int pl1 = s.plan.getPlanLength();

				if (open.size() > 0 
						//&& pl1 >0
						) {
					// here we compare the currnet state s with the 
					// second best state : hvalue will be >= that of s
					State s2 = open.first();
					// last actions in the plan for the states
					String na = ""+ s.plan.getActions().get(pl1-1);
					int pl2 = s2.plan.getPlanLength();
					String na2 = ""+ s2.plan.getActions().get(pl2-1);
					long hv1 = s.getHValue().longValue();
					long hv2 = s2.getHValue().longValue();

					// finally we apply the state filter (if defined)
					if (! (sf instanceof EmptyStateFilter)){
						// collect two good steps that involve the chosen character! take them from the open list
						// could do it only if s is already character c then search for s2 character c with diff heuristic 
						// up to a level

						if (sf.isStateOK(s)) {
							int ns =0;
							Iterator<State> it = open.iterator();
							while (it.hasNext() && ns <5){
								Set<State> cl = (Set<State>) new SetWrapper<State>(closed);
								// if we branch 
								if (na!= na2 &&  
										sf.isStateOK(s2) &&
										hv2  - hv1 < 3 && 
										bn< branchDepth && 
										pl2 - pl1 < 2 ){
									TreeSet<State> op = (TreeSet<State>) open.clone();
									if (!silent) {
										System.out.println("*** Branch  + FILTERING");
										System.out.println(s.plan.getPlanLength() +" " +  getLastAction (s.plan));
										System.out.println(s2.plan.getPlanLength() +" " +  getLastAction (s2.plan));
									}
									op.clear();
									op.add(s2);

									// we start searching for a solution on the branch
									search(s2, nodeCount,bn+1, op, cl);
									break;

								}
								// otherwise 
								cl.add(s2);
								s2 = it.next();
								hv2 = s2.getHValue().longValue();
								pl2 = s2.plan.getPlanLength();
								na2 = ""+ s2.plan.getActions().get(pl2-1);
								ns++;
							}

						}

						// No state filter here
					} else  // if we are not applying a special filter //
						// if: the actions are not the same 
						if (na!= na2 && 
						// lenght of the plan 2  is multiple of step (2 by default)
						(pl2%step == 0) && 
						// we have not just branched 
						//!justStarted && 
						// we don't have more then 3 branches in depth
						bn< branchDepth  && 
						// the s2 hvalue is not worse then s (this means in fact equal)
						hv2  - hv1 < deltaHV &&
						// and if the plan for s2 is not longer then the plan for s (so we avoid useless loops )
						pl2 - pl1 < deltaPL

								) 
						{

							// WE BRANCH! 
							if (!silent)
								System.out.println("*** BRANCH " + (hv2-hv1) + "dh - "
										+(pl2 -pl1) + "dl - " + na +" - VS - " + na2);

							TreeSet<State> op = (TreeSet<State>) open.clone();

							if (clearOpenList ){
								op.clear();
								op.add(s2);
							}						
							Set<State> cl = closed;
							if (!singleClosedList) {
								// we remove the state s2 from the current search's open ones and add it to the close 
								// ( so we avoid going there in a future step) 
								cl = (Set<State>) new SetWrapper<State>(closed);
								closed.add(open.pollFirst());
							}

							// we start searching for a solution on the branch
							search(s2, nodeCount,bn+1, op, cl);
						}
				}

				// we add all the successor to state s and continue;
				open.addAll(s.getNextStates(filter.getActions(s)));


			}
		}
		return;
	}

	private Action getLastAction(Plan plan) {
		List<Action> actions = plan.getActions();
		Action lastAction = actions.get(actions.size()-1);

		return lastAction;
	}

	public int getDeltaHV() {
		return deltaHV;
	}

	public void setDeltaHV(int deltaHV) {
		this.deltaHV = deltaHV;
	}

	public int getDeltaPL() {
		return deltaPL;
	}

	public void setDeltaPL(int deltaPL) {
		this.deltaPL = deltaPL;
	}

	public boolean isSingleClosedList() {
		return singleClosedList;
	}

	public void setSingleClosedList(boolean singleClosedList) {
		this.singleClosedList = singleClosedList;
	}


	public boolean isClearOpenList() {
		return clearOpenList;
	}

	public void setClearOpenList(boolean clearOpenList) {
		this.clearOpenList = clearOpenList;
	}

}
