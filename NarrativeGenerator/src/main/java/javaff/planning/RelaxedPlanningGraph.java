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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.GroundProblem;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

public class RelaxedPlanningGraph extends PlanningGraph
{
	//needed for clone()
	protected RelaxedPlanningGraph()
	{
		
	}
	
	
	public RelaxedPlanningGraph(GroundProblem gp)
	{
		super(gp);
	}
	
	/**
	 * Construct an RPG with a goal which overrides that contained in the first parameter's GroundProblem.goal
	 * field.
	 * @param gp
	 * @param goal
	 */
	public RelaxedPlanningGraph(GroundProblem gp, GroundFact goal)
	{
		super(gp);
		this.setGoal(goal);
	}
	
	
	public RelaxedPlanningGraph(Set groundActions, GroundFact goal) {
		super(groundActions, goal);
		// TODO Auto-generated constructor stub
	}


	@Override
	public Object clone()
	{
		
		RelaxedPlanningGraph clone = new RelaxedPlanningGraph();
		clone.actionMap = new MapWrapper<Action, PlanningGraph.PGAction>(this.actionMap);
		clone.actionMutexes = new SetWrapper(this.actionMutexes);
		clone.actions = new SetWrapper(this.actions);
		clone.factLayers = new ArrayList<Set<Fact>>(this.factLayers);
		clone.goal = new SetWrapper<PGFact>(this.goal);
		clone.initial = new SetWrapper<PGFact>(this.initial);
		clone.level_off = this.level_off;
		clone.num_layers = this.num_layers;
		clone.numeric_level_off = this.numeric_level_off;
		clone.propMutexes = new SetWrapper(this.propMutexes);
		clone.propositionMap = new MapWrapper<Fact, PlanningGraph.PGFact>(this.propositionMap);
		clone.propositions = new SetWrapper(this.propositions);
		clone.readyActions = new SetWrapper<PlanningGraph.PGAction>(this.readyActions);
		
		return clone;
	}
	
	@Override
	public List extractPlan()
	{
		return this.searchRelaxedPlan(this.goal, super.num_layers);
	}
	

	public List searchRelaxedPlan(Set<PGFact> goalSet, int l)
	{
		if (l == 0)
			return new ArrayList();
		Set chosenActions = new SetWrapper();
		// loop through actions to achieve the goal set
		for (PGFact g : goalSet)
		{
			PGAction a = null;
			for (PGAction na : g.achievedBy)
			{
				if (na.layer < l && na.layer >= 0)
				{
					if (na instanceof PGNoOp)
					{
						a = na;
						break; //always choose NO-Ops if they exist
					} 
					else if (chosenActions.contains(na))
					{
						a = na;
						break;
					} 
					else
					{
						if (a == null)
							a = na;
						else if (a.difficulty > na.difficulty) //this is the "min" in h_add
							a = na;
					}
				}
			}

			if (a != null)
			{
				chosenActions.add(a);
			}
		}


		Set newGoalSet = new SetWrapper();
		// loop through chosen actions adding in propositions and comparators
		Iterator cait = chosenActions.iterator();
		while (cait.hasNext())
		{
			PGAction ca = (PGAction) cait.next();
			newGoalSet.addAll(ca.conditions);
		}

		List rplan = this.searchRelaxedPlan(newGoalSet, l - 1);
		rplan.addAll(chosenActions);
		return rplan;
	}


	public boolean checkPropMutex(MutexPair m, int l)
	{
		return false;
	}

	public boolean checkPropMutex(PGFact p1, PGFact p2, int l)
	{
		return false;
	}

	public boolean checkActionMutex(MutexPair m, int l)
	{
		return false;
	}

	public boolean checkActionMutex(PGAction a1, PGAction a2, int l)
	{
		return false;
	}

	protected boolean noMutexes(Set s, int l)
	{
		return true;
	}

	protected boolean noMutexesTest(Node n, Set s, int l) // Tests to see if
															// there is a mutex
															// between n and all
															// nodes in s
	{
		return true;
	}
	
	@Override
	public void setGoal(Fact g)
	{
		super.setGoal(g);
	}
//	
//	/**
//	 * Creates the RPG, returns null.
//	 */
//	@Override
//	public TotalOrderPlan getPlan(State s)
//	{
//		setInitial(s);
//		resetAll(s);
//
//		// set up the intital set of facts
//		Set scheduledFacts = new SetWrapper(initial);
//		List scheduledActs = null;
//
//		scheduledActs = createFactLayer(scheduledFacts, 0);
//		List plan = null;
//
//		this.factLayers.add(new ArrayList(initial)); //add current layer
//
//		// create the graph==========================================
//		while (true)
//		{
//			scheduledFacts = createActionLayer(scheduledActs, num_layers);
//			++num_layers;
//			scheduledActs = createFactLayer(scheduledFacts, num_layers);
//
//			if (scheduledFacts != null)
//			{
//				List factList = new ArrayList();
//				//plan = extractPlan();
//				for (Object pgp : scheduledFacts)
//					factList.add(((PGProposition)pgp).proposition);
//				
//				this.factLayers.add(factList); //add current layer
//				
//			}
//
//			
//			if (scheduledActs.size() == 0 && scheduledFacts.size() == 0)
//				plan = extractPlan();
//			
//			if (plan != null)
//				break;
//			if (!level_off)
//				numeric_level_off = 0;
//			if (level_off || numeric_level_off >= NUMERIC_LIMIT)
//			{
//				// printGraph();
//				break;
//			}
//		}
//
//		if (plan != null)
//		{
//			Iterator pit = plan.iterator();
//			TotalOrderPlan p = new TotalOrderPlan();
//			while (pit.hasNext())
//			{
//				PGAction a = (PGAction) pit.next();
//				if (!(a instanceof PGNoOp))
//					p.addAction(a.action);
//			}
//			// p.print(javaff.JavaFF.infoOutput);
//			return p;
//		} else
//			return null;
//
//	}

}