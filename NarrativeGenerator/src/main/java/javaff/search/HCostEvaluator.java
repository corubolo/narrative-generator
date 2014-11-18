
package javaff.search;

import javaff.planning.State;

public interface HCostEvaluator {

	public abstract double computeHCost(State s);

}