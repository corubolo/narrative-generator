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

import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class OldBranchingBestFirstSearch extends BranchingSearch {
	
	public OldBranchingBestFirstSearch() {
		super();
		setComp(new HValueComparator());
	}


	public List<State> search(State start) {
		Set<State> closed;
		TreeSet<State> open;
		closed = new SetWrapper<State>();
		open = new TreeSet<State>(comp);

		return search(start, 0, 0, open, closed);
	}

	public List<State> search(State start, int branchNumber, int nodeCount,
			TreeSet<State> open, Set<State> closed) {
		branchNumber++;

		int lastBranch = 0;
		open.add(start);
		while (!open.isEmpty()) {
			State s = open.pollFirst();
			if (!closed.contains(s)) {
				closed.add(s);
				++nodeCount;

				if (s.goalReached()) {
					if (!solutions.contains(s))
						System.out.print("NEW ");

					System.out.println("SOL: " + branchNumber + " " + nodeCount
							+ s.plan);

					solutions.add(s);
					return solutions;
				} 
				// branching happens here: every 4th action, we search on
				// the second best successor (if any is there)
				if (isPow(nodeCount, 6)
						&& lastBranch != s.plan.getPlanLength()) // isPow2(s.plan.getPlanLength(),4)
					// &&
				{
					try {
						lastBranch = s.plan.getPlanLength();

						TreeSet<State> op = (TreeSet<State>) open.clone();
						Set<State> cl = (Set<State>) new SetWrapper(closed);
						State b2 = null;
						int n = 0;
						do {
							n++;
							b2 = op.iterator().next();
							op.remove(b2);
							cl.add(b2);
						} while (n < op.size() && n < 3);
						b2 = op.iterator().next();
						System.out.println("Branch to "
								+ b2.plan.getActions().get(
										b2.plan.getActions().size() - 1));
						search(b2, branchNumber, nodeCount, op, cl);

						// TreeSet<State> op = (TreeSet<State>)open.clone();
						// HashSet<State> cl =
						// (HashSet<State>)closed.clone();
						// State b2,b3 = null;
						// b2= open.iterator().next();
						// b3= open.iterator().next();
						// System.out.println("Branch to " +
						// b2.plan.getActions().get(b2.plan.getActions().size()-
						// 1));
						// search(b2,branchNumber, nodeCount, op, cl);
						// op = (TreeSet<State>)open.clone();
						// cl = (HashSet<State>)closed.clone();
						//
						// System.out.println("Branch to " +
						// b3.plan.getActions().get(b3.plan.getActions().size()-
						// 1));
						// search(b3,branchNumber, nodeCount, op, cl);
						// closed.add(b2);
						// closed.add(b3);
						// open.remove(b2);
						// open.remove(b3);
						//
						// open.removeAll(s.getNextStates(filter.getActions(s)));
						//
						//
						// final State b3 = (State)b2.clone();
						// final int bn = branchNumber, nc = nodeCount;
						// final TreeSet<State> opp = op;
						// final HashSet<State> cll = cl;
						// new Thread() {public void run() {search(b3,bn,
						// nc, opp, cll);};}.start();
					} catch (Exception x) {

					}
				}
				open.addAll(s.getNextStates(filter.getActions(s)));


				System.out.println(branchNumber + " " + s.plan);
				System.out.println(nodeCount);
				System.out.println(s.plan.getPlanLength());

			}

		}
		return solutions;
	}

	public static void main(String[] args) {
		for (int i = 0; i < 100; i++)
			if (isPow(i, 4))
				System.out.println(i);
	}

	private static boolean isPow(int nodeCount, int i) {
		nodeCount -= 1;
		if (nodeCount <= 0)
			return false;
		int ii = i;
		while (i <= nodeCount) {
			if (i == nodeCount)
				return true;
			if (ii > 2)
				--ii;
			i *= (ii);
		}
		return false;
	}

	private static boolean isPow2(int nodeCount, int i) {
		if (nodeCount == 0)
			return false;
		if (nodeCount % i == 0) {

			return true;
		}
		return false;
	}
}
