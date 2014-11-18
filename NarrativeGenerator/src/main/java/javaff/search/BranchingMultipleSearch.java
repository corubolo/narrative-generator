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

import java.util.List;

import javaff.JavaFF_mod;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;
import uk.ac.liverpool.narrative.Solution;

public class BranchingMultipleSearch extends BranchingSearch  {


	public BranchingMultipleSearch() {
		super();
		maxDepth = 1000;
	}

	private int iterations = 20;
	private Class<? extends BranchingSearch> algorithm = EnforcedHillClimbingSearchMod.class;



	public int getIterations() {
		return iterations;
	}



	public void setIterations(int numSearch) {
		this.iterations = numSearch;
	}



	public Class<? extends BranchingSearch> getAlgorithm() {
		return algorithm;
	}



	public void setAlgorithm(Class<? extends BranchingSearch> algorithm) {
		this.algorithm = algorithm;
	}



	public List<State> search(State start) {
		initialStart = start;
		SetWrapper.setRandomizeEach(true);
		MapWrapper.setRandomizeEach(true);
		try {
			for (int i=0; i< iterations;i++) {
				if (stop)
					return solutions;
				JavaFF_mod.logger.info("Iteration: " + i);
				BranchingSearch b = algorithm.newInstance();
				b.setFilter(filter);
				b.setComp(comp);
				List<State> sol = b.search(start);
				if (sol.size()>0)
					if (isNewSolution(sol.get(0))){ 
						System.out.print("NEW ");
						solutions.add(sol.get(0));
						fireNewSolution(new Solution((STRIPSState)initialStart, sol.get(0)));
					}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {	
			e.printStackTrace();
		}

		return solutions;

	}


}
