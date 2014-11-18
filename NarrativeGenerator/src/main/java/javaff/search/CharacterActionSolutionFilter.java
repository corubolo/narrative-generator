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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import javaff.data.Action;
import uk.ac.liverpool.narrative.Solution;

public class CharacterActionSolutionFilter implements SolutionFilter{

	Map<String, float[]> actionCosts;
	public Map<String, float[]> getActionCosts() {
		return actionCosts;
	}

	public void setActionCosts(Map<String, float[]> actionCosts) {
		this.actionCosts = actionCosts;
	}

	public Map<String, float[]> getCharacterCosts() {
		return characterCosts;
	}

	public void setCharacterCosts(Map<String, float[]> characterCosts) {
		this.characterCosts = characterCosts;
	}

	public float[] getAuthorDirection() {
		return authorDirection;
	}

	public void setAuthorDirection(float[] authorDirection) {
		this.authorDirection = authorDirection;
	}

	Map<String, float[]> characterCosts;
	float[] authorDirection = null;
	
	double threshold;
	
	public double getThreshold() {
		
		return threshold;
	}

	public void setThreshold(double threshold) {
		
		Preconditions.checkArgument((threshold <=1 && threshold> 0),"Threshold must be in the [0:1] range ");
		this.threshold = threshold;
	}

	public CharacterActionSolutionFilter(Map<String, float[]> actionCosts, double threshold) {
		super();
		this.actionCosts = actionCosts;
		
		this.threshold = threshold;
	}

	public CharacterActionSolutionFilter() {
		// TODO Auto-generated constructor stub
	}
	
	private class Ssol implements Comparable<Ssol>{
		
		public Ssol(Solution s, double d) {
			super();
			this.s = s;
			this.d = d;
		}

		Solution s;
		double d;
		
		@Override
		public int compareTo(Ssol o) {
			// TODO Auto-generated method stub
			return Double.compare(d, o.d);
		}
		
	}

	@Override
	public List<Solution> filterSolutions(List<Solution> so) {
		LinkedList<Ssol > s2 = new LinkedList<Ssol>();
		for ( Solution sol: so ) {
			double ccost = computeCost(sol.actions);
			s2.add(new Ssol(sol, ccost));
		}
		Collections.sort(s2);
		int n = (int)(so.size() * threshold);
		List<Solution> s = new ArrayList<Solution>(n);
		for (int i=0; i<n ; i++)
			s.add(s2.get(s2.size()-1-i).s);
		return s;
	}

	private float computeCost(List<Action> l1) {
		if (l1.size() == 0)
			return 0;
		float totalFutureCost = 0.0f;
		for (int i = 0; i < l1.size(); i++) {
			Action a = l1.get(i);
			double c = distanceCharAction(a.name.toString(), a.parameters.get(0).toString());
//			if (c!=0)
//				System.out.println(a + " " + c);
			totalFutureCost += c;
		}
		return totalFutureCost/ l1.size();
	}

	private double distanceCharAction(String action,
			String character) {
		float[] cc ;
		float[] ac = actionCosts.get(action);
		if (characterCosts != null)
			cc = characterCosts.get(character);
		else 
			cc = authorDirection;
		try {
		double r = distanceNorm(cc,
				ac);
		
		return r;
		} catch (NullPointerException x) {
			String s  = "Missing " + (ac==null?"action: "+ action:"character: " +character);
			x.printStackTrace();
			throw (new IllegalArgumentException (s, x));
		}
	}
	
	/**
	 * Will return a number between -1; 1 to express agreement/disagreement between vectors
	 * @param x
	 * @param y
	 * @return
	 */

	public double distanceNorm(float[] x, float[] y) {

		double sumXY2 = 0;
		int c = 0;
		for(int i = 0, n = x.length; i < n; i++) {
			// Neutral  0
			if (!((Float.isNaN(x[i])|| Float.isNaN(y[i])))) {
				double sign = (Math.abs( x[i] - y[i])- 0.5)*2;
				sumXY2 +=  sign;
				c++;
			}		
			// disagree			 1 - 0 = 0 - 1 = 1 ->              					 -1
			// agree    		1 - 1 = 0 - 0 = x - x = 0       					 1
			// Neutral 1 - 0.5 = 0.5 - 1 = 0 - 0.5 = 0.5 - 1           				 0
		}
		if (c == 0)
			return 0;
		return  -1.0 * sumXY2/ c;
	}

}
