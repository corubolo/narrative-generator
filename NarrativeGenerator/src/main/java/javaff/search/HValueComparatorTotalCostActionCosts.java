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
import java.util.List;
import java.util.Map;

import javaff.data.Action;
import javaff.planning.STRIPSState;
import javaff.planning.State;

public class HValueComparatorTotalCostActionCosts implements Comparator<State>, HCostEvaluator {

	boolean usePast = false;
	boolean useFuture = true;

	Map<String, float[]> actionCosts;
	Map<String, float[]> characterCosts;
	float[] authorDirection = null;

	public float[] getAuthorDirection() {
		return authorDirection;
	}

	public void setAuthorDirection(float[] authorDirection) {
		this.authorDirection = authorDirection;
	}

	public HValueComparatorTotalCostActionCosts() {
		super();
	}

	public void setActionCostMap(Map<String, float[]> costs) {
		this.actionCosts = costs;
	}

	public void setCharacterCostMap(Map<String, float[]> costs) {
		this.characterCosts = costs;
	}

	public int compare(State obj1, State obj2) {
		int r = 0;
		if (obj1 == obj2)
			return 0;
		if ((obj1 instanceof STRIPSState) && (obj2 instanceof STRIPSState)) {
			STRIPSState s1 = (STRIPSState) obj1;
			STRIPSState s2 = (STRIPSState) obj2;

			// Get the Hvalues
			int d1 = s1.getHValue().intValue();
			int d2 = s2.getHValue().intValue();
			List<Action> h1 = null;
			// find the relaxed plan of S1
			if (s1.RelaxedPlan != null)
				h1 = s1.RelaxedPlan.getActions();
			List<Action> h2 = null;
			// find the relaxed plan of S2
			if (s2.RelaxedPlan != null)
				h2 = s2.RelaxedPlan.getActions();

			List<Action> g1 = s1.plan.getActions();
			List<Action> g2 = s2.plan.getActions();

			int size1 = g1.size();
			int size2 = g2.size();
			if (size1<1 || size2<1)
				return (int)Math.signum(d1- d2) ;

			Action a1 = g1.get(size1 - 1);
			Action a2 = g2.get(size2 - 1);

			// Calculate the euclidean distances between the chacarter and the
			// action annotations of the present action
			double c1 = distanceCharAction(a1.name.toString(), a1.parameters.get(0).toString());
			double c2 = distanceCharAction(a2.name.toString(), a2.parameters.get(0).toString());

			float pc1 = 0, pc2 = 0, fc1 = 0, fc2 = 0;

			// If we use the past, calculate the cost of the past actions
			if (!usePast) {
				pc1=pc2=0;
			} else {
				pc1 = computeCost(g1, 1);
				pc2 = computeCost(g2, 1);
			}
			// If we use the past, calculate the cost of the past actions
			if (!useFuture) {
				fc1=fc2=0;
			} else {
				fc1 = computeCost(h1);
				fc2 = computeCost(h2);
			}

			double cost1 = pc1 + c1 + fc1;
			double cost2 = pc2 + c2 + fc2;

			// Compare the h values of S1 and S2
			int r1 = (int)d1-d2;

			// Compare the cost values of S1 and S2
			double r2 = cost1 - cost2;

			// Agree
			if (r1 > 0 && r2 > 0)
				return 1;

			// Agree
			if (r1 < 0 && r2 < 0)
				return -1;

			// H value is same
			if (r1 == 0) {
				//System.out.println("delta " + (r1 - r2) );
				return (int)Math.signum(r2);

			}
			// Disagree
			if (((r1<0 && r2 > 0 )|| (r1>0 && r2<0)) && (Math.abs(r2) - Math.abs(r1))>2) {
				System.out.println("Force "+ r1 + " "+ (r2));
				r = (int)Math.signum(r2);
			} else 
			{
				r = r1;
			}
			if (r == 0) {
				if (s1.hashCode() > s2.hashCode())
					r = 1;
				else if (s1.hashCode() == s2.hashCode() && s1.equals(s2))
					r = 0;
				else
					r = -1;
			}

		}

		return r;
	}

	private float computeCost(List<Action> l1) {
		return computeCost(l1,0);
	}

	private float computeCost(List<Action> l1, int skip) {

		if (l1.size() == 0)
			return 1;
		float totalFutureCost = 0.0f;
		for (int i = 0; i < l1.size() - skip; i++) {
			Action a = l1.get(i);
			totalFutureCost += distanceCharAction(a.name.toString(), a.parameters.get(0).toString());
		}
		return totalFutureCost ;
	}

	private double distanceCharAction(String action,
			String character) {
		float[] cc ;
		float[] ac = actionCosts.get(action);
		if (characterCosts != null)
			cc = characterCosts.get(character);
		else 
			cc = authorDirection;
		double r = euclideanDistanceNorm(cc,
				ac);
//		if (r!= 1)
//			System.out.println(action + " "  + r);
		return r;
	}

	public double euclideanDistanceNorm(float[] x, float[] y) {

		double sumXY2 = 0;
		for(int i = 0, n = x.length; i < n; i++) {
			// inifluent 															1
			if ((Float.isNaN(x[i])|| Float.isNaN(y[i])))
				sumXY2++;
			else {
				
				double sign = 1 + (Math.abs( x[i] - y[i])- 0.5)*2* x.length ;//* 8;
				sumXY2 +=  sign  ;
			}		
			// disagree			 1 - 0 = 0 - 1 = 1 ->              					 2
			// agree    		1 - 1 = 0 - 0 = x - x = 0       					1
			// average disagreement 1 - 0.5 = 0.5 - 1 = 0 - 0.5 = 0.5 - 1            0
		}
		return sumXY2 / x.length;
	}

	/* (non-Javadoc)
	 * @see javaff.search.HCostEvaluator#computeHCost(javaff.planning.State)
	 */
	@Override
	public double computeHCost(State s) {

		if (s instanceof STRIPSState) {
			STRIPSState ss = (STRIPSState) s;
			List<Action> h1 = null;
			int h = ss.getHValue().intValue();
			if (ss.RelaxedPlan != null)
				h1 = ss.RelaxedPlan.getActions();
			double r = computeCost(h1);
			return r;
		}
		return s.getHValue().intValue();
	}

}
