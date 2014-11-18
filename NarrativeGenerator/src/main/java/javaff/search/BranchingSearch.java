/***********************************************************************
 *
 * This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com - and Meriem Bendis
 * The University of Liverpool
 *
 *
 * BranchingStoryGenerator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * BranchingStoryGenerator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 *
 ************************************************************************/

package javaff.search;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import javaff.data.Action;
import javaff.data.strips.Proposition;
import javaff.planning.Filter;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.Solution;
import uk.ac.liverpool.narrative.Template;

public abstract class BranchingSearch {

	protected Comparator<State> comp;
	protected Filter filter = null;
	public  List<State> solutions;
	protected StateFilter sf;
	protected State initialStart;
	// Parameters that influence branch generation
	// branch each N steps
	protected int step = 2;
	// max depth
	protected int maxDepth;
	// compare plans only using the end state (equals)
	public static int totalBranches = 0;
	public static int totalSolutions = 0;
	List<SolutionListener> sollis = new LinkedList<SolutionListener>();
	
	public boolean silent = false;
	
	static Template template;

	public static Template getTemplate() {
		return template;
	}

	public static void setTemplate(Template te) {
		template = te;
	}
	public int branchDepth = 3;
	volatile protected boolean stop = false;

	public int getBranchDepth() {
		return branchDepth;
	}

	public void setBranchDepth(int branchDepth) {
		this.branchDepth = branchDepth;
	}

	public void stop() {
		stop = true;
	}

	public void addSolutionListener(SolutionListener s) {
		sollis.add(s);
	}
	public void addSolutionListener(Set<SolutionListener> s) {
		sollis.addAll(s);
	}
	protected void fireNewSolution(Solution s) {
		for (SolutionListener sl:sollis)
			sl.processSolution(s);
	}
	
	public enum CompareMethod {
		/**
		 * Compares if the two end states are equals at the end: that is if the facts are equals at the end (same propositions that is same name and parameters).
		 */
		equalState, 
		/**
		 * If the sequence of actions is the exact same (same order and all equal)
		 */
		//equalActions,
		/**
		 * If the sequence of actions is different of at least Delta; given the LCS algorithm.
		 */
		LCSDelta, 
		/**
		 * If the sequence of actions is different of at least Delta; that at least delta actions are different, in any order.
		 */
		ActionDelta, 
		/**
		 * if an action list is a permutation of another
		 */
		permutation, 
		
		/**
		 * Jacard similarity based
		 */
		Jacard, 
		/**
		 * Dice similarity based
		 */
		Dice, 
		
		/** state difference at the end: at least delta number of propositions must be different */
		StateDelta, 
		/**
		 * no action taken (always different)
		 */
	//	noAction
	};

	// minimum difference (in number of actions) in order to consider two plans different (order not counting).
	protected int deltaTS= 2;
	// here we just want to remove the completely useless solutions; we will do a full cleaning at the end. So we take the action delta method with delta =1 that we get rid of permutations of the same actions
	protected CompareMethod useSolutionMethod = CompareMethod.permutation;

	
	public BranchingSearch() {
		sf = new EmptyStateFilter();
		solutions = new LinkedList<State>();
	}

	public Comparator<State> getComp() {
		return comp;
	}

	public void setComp(Comparator<State> comp) {
		this.comp = comp;
	}

	public abstract List<State> search(State start);

	public boolean isNewSolution (State s) {

		return checkIfNewSol (s,useSolutionMethod, deltaTS, solutions);
	}

	public Filter getFilter() {
		return filter;
	}

	public void setFilter(Filter filter) {
		this.filter = filter;
	}

	public StateFilter getSf() {
		return sf;
	}

	public void setSf(StateFilter sf) {
		this.sf = sf;
	}


	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}
	public int getMaxDepth() {
		return maxDepth;
	}

	public void setMaxDepth(int d) {
		this.maxDepth = d;
	}

	/**
	 * This method is analogous to the {@link #checkIfNewSol(State, CompareMethod, int, List)} method but will remove from the list the duplicate solution, according to the usm to the criteria. 
	 * @param sol List of current solutions (as list of actions!)
	 * @param finalState The state containing the plan (where all goals are accomplished)
	 * @param usm Comparison method 
	 * @param d minimum difference (in number of different steps) for two plans to be considered different for the delta methods
	 * @return True if the solution is new (not already present in the list) according to the criteria
	 */

	public static List<Solution> cleanupSolutions (CompareMethod usm, double d, List<Solution> sol) {
		List<Solution> ret = new LinkedList<Solution>(sol);
		for (int i=0;i<ret.size();i++) {
			List<Action> a = ret.get(i).actions;
			State e1 = ret.get(i).end;
			for (int k=i+1;k<ret.size();k++) {
				List<Action> b = ret.get(k).actions;
				State e2 = ret.get(k).end;
				if (!isDifferent(a, b, usm, d, e1, e2)) {
					ret.remove(k);
					k--;
				}
			}
		}

		return ret;
	}

	/**
	 * This method is analogous to the {@link #checkIfNewSol(State, CompareMethod, int, List)} method but will remove from the list the duplicate solution, according to the usm to the criteria. 
	 * @param sol List of current solutions
	 * @param finalState The state containing the plan (where all goals are accomplished)
	 * @param usm Comparison method 
	 * @param d minimum difference (in number of different steps) for two plans to be considered different for the delta methods
	 * @return True if the solution is new (not already present in the list) according to the criteria
	 */

	public static List<State> cleanupSolutions (List<State> sol, CompareMethod usm, int d) {
		List <State> ret = new LinkedList<State>(sol);
		for (int i=0;i<ret.size();i++) {
			State a = ret.get(i);
			for (int k=i+1;k<ret.size();k++) {
				State b = ret.get(k);
				if (!isDifferent(a, b, usm, d)) {
					ret.remove(k);
					k--;
				}
			}
		}

		return ret;
	}

	/** 
	 * This method is used to verify if a solution is new or it is repeated. 
	 * The simplest way is by setting useEqualState; this is done by comparing the end states to be different; but this will cut out all solutions that have same end state but different path. 
	 * 
	 * 
	 * @param sol List of current solutions
	 * @param finalState The state containing the plan (where all goals are accomplished)
	 * @param usm Comparison method 
	 * @param d minimum difference (in number of different steps) for two plans to be considered different for the delta methods
	 * @return true if the solution is new (not already present in the list) according to the criteria
	 */
	public static boolean checkIfNewSol(State finalState, CompareMethod usm, int d, List<State> sol) {
		// if there is an equal one, it's not new
		for (State c :sol) {
			if (isDifferent(finalState, c, usm, d) == false)
				return false;
		}
		//otherwise it is new
		return true;

	}
	/** 
	 * This method is used to verify if a solution is new or it is repeated. 
	 * The simplest way is by setting useEqualState; this is done by comparing the end states to be different; but this will cut out all solutions that have same end state but different path. 
	 * 
	 * 
	 * @param sol List of current solutions
	 * @param finalState The state containing the plan (where all goals are accomplished)
	 * @param usm Comparison method 
	 * @param d minimum difference (in number of different steps) for two plans to be considered different for the delta methods
	 * @return true if the solution is new (not already present in the list) according to the criteria
	 */
	public static boolean checkIfNewSol(Solution s, CompareMethod usm, double d, List<Solution> sol) {
		// if there is an equal one, it's not new
		for (Solution c :sol) {
			if (isDifferent(c.actions, s.actions, usm, d, c.end, s.end) == false)
				return false;
		}
		//otherwise it is new
		return true;

	}

	private static boolean isDifferent(State x,State y, CompareMethod usm,
			int d) {
		List<Action> a = x.plan.getActions();
		List<Action> b = y.plan.getActions();
		return isDifferent(a, b, usm, d, x, y);
	}
	/**
	 * @param finalState
	 * @param usm
	 * @param d
	 * @param a
	 * @param c
	 * @param b
	 */
	private static boolean isDifferent(List a,List b, CompareMethod usm,
			double d, State x, State y) {
		int delta = 0;
		List aa = new LinkedList();
		List bb = new LinkedList();
		if (template!=null) {
			for (Object ac: a) {
				aa.add(template.apply_template((Action)ac));
			}
			for (Object ac: b) {
				bb.add(template.apply_template((Action)ac));
			}
			a = aa;
			b = bb;
		}
		switch (usm) {
		case permutation:
			if (a.size()!=b.size())
				return true;
			for (Object l:a) {
				if (!b.contains(l))
					return true;
			}
			return false;
		case ActionDelta:
			//delta is abs (difference in length)
			delta = Math.abs(a.size() - b.size());
			int limit = Math.min( a.size() , b.size());

			// for all the elements (up to min (a,b lengths))
			for (int i=0; i<limit ; i++) {
				// if an action is already in the plan; it does not differentiate ( so different plans with the same actions will be considered equal)
				if (!b.contains(a.get(i))) {
					delta++;
				}
			}
			if (delta > d)
				return true;
			else return false;
		case equalState:
			if (x== null || y == null)
				throw new IllegalArgumentException ("Null state when comparing by states");
			if (((STRIPSState)y).facts.equals(((STRIPSState)x).facts))
				return false;
			else return true;
		case LCSDelta:
			List aaa = BranchingUtils.longestCommonSubsequence(a.toArray(), b.toArray());
			delta =  Math.min(a.size(), b.size()) -aaa.size();
			if (delta > d)
				return true;
			else return false;
//		case equalActions:
//			if (a.equals(b))
//				return false;
//			else return true;
//		case LCSStringDelta:
//			String s = BranchingUtils.longestCommonSubstring(a.toString(), b.toString());
//			delta =  Math.max(a.toString().length(), b.toString().length()) -s.length();
//			if (delta > d)
//				return true;
//			else return false;
		case Jacard:
			double t = BranchingUtils.jacardSimilarity(a,b);

			if (t > 1 - (d/10))
				return false;
			else return true;
		case Dice:
			double t1 = BranchingUtils.diceSimilarity(a,b);
			if (t1 > 1 - (d/10))
				return false;
			else return true;
			
		case StateDelta:
			Set<Proposition> fx = ((STRIPSState)x).facts;
			Set<Proposition> fy = ((STRIPSState)y).facts;
			delta = Sets.difference(fx, fy).size();
			//System.out.println(delta);
			if (delta > d)
				return true;
			else return false;
		default:
			break;
		}
		return true;
	}

}
