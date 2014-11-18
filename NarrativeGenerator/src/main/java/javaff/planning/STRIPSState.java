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

package javaff.planning;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Set;

import javaff.data.Action;
import javaff.data.GroundFact;
import javaff.data.Plan;
import javaff.data.TotalOrderPlan;
import javaff.data.strips.Proposition;
import uk.ac.liverpool.narrative.SetWrapper;

public class STRIPSState extends State implements Cloneable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7650218292026400142L;
	public static boolean doStatics = false;
	public Set<Proposition> facts;
	transient public Set<Action> actions;
	transient public TotalOrderPlan RelaxedPlan = null;
	transient public Set<Action> helpfulActions = null;

	transient protected RelaxedPlanningGraph RPG;
	transient protected boolean RPCalculated = false;
	transient protected BigDecimal HValue = null;

	protected STRIPSState()
	{

	}

	public TotalOrderPlan getPlan()
	{
		return (TotalOrderPlan) plan;
	}

	public STRIPSState(Set<Action> a, Set<Proposition> f, GroundFact g)
	{
		facts = f;
		goal = g;
		actions = a;

		this.helpfulActions = new SetWrapper<Action>();
		this.HValue = new BigDecimal(-1);
		this.RelaxedPlan = null;
		// filter = NullFilter.getInstance();
	}

	protected STRIPSState(Set a, Set f, GroundFact g, TotalOrderPlan p)
	{
		this(a, f, g);
		plan = p;
	}

	public Object clone()
	{
		Set<Proposition> nf = new SetWrapper<Proposition>(facts);//.clone();
		TotalOrderPlan p = null;
		if (plan!=null)
			p = (TotalOrderPlan) plan.clone();
		GroundFact g = null;
		if (goal!=null)
			g= (GroundFact) goal.clone();
		if (actions == null)
			actions = new SetWrapper<Action>();
		STRIPSState SS = new STRIPSState(new SetWrapper<Action>(actions), nf, g, p);
		//		SS.setRPG(new RelaxedPlanningGraph(this.RPG.gproblem));

		if (this.RPG != null)
		{
			SS.RPG = this.RPG;
			//TODO: Check if RPG needs to be cloned! 
//			RelaxedPlanningGraph rpg = (RelaxedPlanningGraph) this.RPG.clone();
//			SS.RPG = rpg;

			SS.RPCalculated = this.RPCalculated;
		}

		if (this.HValue != null)
		{
			SS.HValue = new BigDecimal(this.HValue.toString());
		}
		if (helpfulActions!=null) {
			SS.helpfulActions = new SetWrapper<Action>();
			for (Action ha : this.helpfulActions)
			{
				SS.helpfulActions.add((Action) ha.clone());
			}

			if (SS.plan != null)
			{
				SS.plan = (TotalOrderPlan) this.plan.clone();
			}
		}
		
		SS.sometimes = sometimes;
		SS.always = always;
		SS.currentGoal = currentGoal;
		// SS.setFilter(filter);
		return SS;
	}

	public void setRPG(RelaxedPlanningGraph rpg)
	{
		RPG = rpg;
		RPCalculated = false;
	}

	public RelaxedPlanningGraph getRPG()
	{
		return RPG;
	}

	// public Set getNextStates() // get all the next possible states reachable
	// from this tmstate
	// {
	// return getNextStates(filter.getActions(this));
	// }

	public State apply(Action a)
	{
		//		STRIPSState s = (STRIPSState) super.apply(a);
		STRIPSState s = (STRIPSState) this.clone();
		s.RPCalculated = false; //if we don't set false here, FF heuristic won't work.

		a.apply(s);
		if (plan!=null)
			s.plan.addAction(a);
		return s;
	}

	public void addProposition(Proposition p)
	{
		facts.add(p);
	}

	public void removeProposition(Proposition p)
	{
		facts.remove(p);
	}

	public boolean isTrue(Proposition p)
	{
		//		int ph = p.hashCode();
		//		for (Proposition q : this.facts)
		//		{
		//			/* 23/8/11 -- something batshit crazy is going on here. On certain domains and specific problems
		//						  the hashCode() being returned from the comparison variable is identical to the
		//						  non-equal value of the Proposition being checked. I cannot explain it, but
		//						  an example is "fuel-level plane2 fl2" and "fuel-level plane3 fl3" being classed
		//						  as equal through their respective hashCodes. The end result of this is
		//						  actions becoming applicable when they are not, and thus invalid plans.
		//						  So, the commented out code has been replaced with a simple call to equals().
		//						  I have no doubt this is slower, but I would rather it just worked.
		//			*/
		//			int qh = q.hashCode();
		//			
		//			if (ph == qh)
		//				return true;
		//			
		//			if (p
		//		}
		//23/8/11 - all illegal statics should have been removed by now, so if a fact has somehow
		//			made it to here (probably embedded in a Quantified literal), then it must be legal and
		//			therefore true.
//		if (p.isStatic()) {
//			
////			if (!this.facts.contains(p))
////				throw new IllegalArgumentException("Static but untrue! see Strispstate.isTrue");
//			return true;  
//		}
		if (doStatics) {
			if (p.isStatic()) 
				return true;  
		}
		return this.facts.contains(p);
	}

	public Set<Action> getActions()
	{
		return actions;
	}

	public void calculateRP()
	{
		if (!RPCalculated)
		{
			RelaxedPlan = RPG.getPlan(this);
			helpfulActions = new SetWrapper();
			if (!(RelaxedPlan == null))
			{
				HValue = new BigDecimal(RelaxedPlan.getPlanLength());

				Iterator it = RelaxedPlan.iterator();
				while (it.hasNext())
				{
					Action a = (Action) it.next();
					if (RPG.getLayer(a) == 0)
						helpfulActions.add(a);
				}
			} else
				HValue = javaff.JavaFF_mod.MAX_DURATION;
			RPCalculated = true;
		}
	}

	public BigDecimal getHValue()
	{
		calculateRP();
		return HValue;
	}

	public BigDecimal getGValue()
	{
		return new BigDecimal(plan.getPlanLength());
	}

	public Plan getSolution()
	{
		return plan;
	}

	public boolean equals(Object obj)
	{
		if (obj instanceof STRIPSState)
		{
			STRIPSState s = (STRIPSState) obj;
			return s.facts.equals(facts);
		} else
			return false;
	}

	public int hashCode()
	{
		int hash = 7;
		hash = 31 * hash ^ facts.hashCode();
		return hash;
	}

	@Override
	public String toString()
	{
		StringBuffer strBuf = new StringBuffer();
		for (Proposition o : this.facts)
		{
			strBuf.append(o+ ", " );
		}

		return strBuf.toString();
	}

}
