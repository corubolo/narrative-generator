/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 * Copyright 2011, David Pattison
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.GroundProblem;
import javaff.data.TotalOrderPlan;
import javaff.data.strips.Not;
import javaff.data.strips.Proposition;
import javaff.data.strips.SingleLiteral;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

public class PlanningGraph
{
	// ******************************************************
	// Data Structures
	// ******************************************************
	public Map<Fact, PGFact> propositionMap = new MapWrapper<Fact, PGFact>(); // (Fact => PGProposition)
	public Map<Action, PGAction> actionMap = new MapWrapper<Action, PGAction>(); // (Action => PGAction)

	public Set propositions = new SetWrapper();//cant cast to Fact because, insanely, it is used to store both Propositions and PGPropositions
	public Set actions = new SetWrapper();

	public Set<PGFact> initial, goal;
	public Set propMutexes, actionMutexes;
	//	public List memorised; //5/5/12 -- removed this because I dont think its actually doing anything. The real FF does use memorisation, but JavaFF doesnt seem to have this implemented properly

	public List<Set<Fact>> factLayers = new ArrayList<Set<Fact>>();


	protected Set<PGAction> readyActions = null; // PGActions that have all their
	// propositions met, but not their
	// PGBinaryComparators or preconditions
	// are mutex
	//	public GroundProblem gproblem; //this is a stupid place to keep a copy of the groundproblem 
	//but the existing infrastructure demands I do it in order to solve
	//the problem of cloning plan graphs without resorting to mountains of code. -- dpattiso

	boolean level_off = false;
	static int NUMERIC_LIMIT = 4;
	int numeric_level_off = 0;
	int num_layers;
	private boolean doMutexes = false;

	// ******************************************************
	// Main methods
	// ******************************************************
	protected PlanningGraph()
	{

	}

	public PlanningGraph(GroundProblem gp)
	{
		this(gp.actions, gp.goal);
	}

	public PlanningGraph(Set groundActions, GroundFact goal)
	{
		this.actionMap = new MapWrapper<Action, PlanningGraph.PGAction>();
		this.actionMutexes = new SetWrapper();
		this.actions = new SetWrapper();
		this.factLayers = new ArrayList<Set<Fact>>();
		this.goal = new SetWrapper<PlanningGraph.PGFact>();
		this.initial = new SetWrapper<PlanningGraph.PGFact>();
		this.level_off = false;
		this.num_layers = 0;
		this.numeric_level_off = 4;
		this.propMutexes = new SetWrapper();
		this.propositionMap = new MapWrapper<Fact, PlanningGraph.PGFact>();
		this.propositions = new SetWrapper();
		this.readyActions = new SetWrapper<PlanningGraph.PGAction>();


		setActionMap(groundActions);
		setLinks();
		createNoOps();
		setGoal(goal);
	}

	public Object clone()
	{
		PlanningGraph clone = new PlanningGraph();
		clone.actionMap = new MapWrapper<Action, PlanningGraph.PGAction>(this.actionMap);



		clone.actionMutexes = new SetWrapper(this.actionMutexes);
		clone.actions = new SetWrapper(this.actions);
		clone.factLayers = new ArrayList<Set<Fact>>(this.factLayers);
		clone.goal = new SetWrapper<PGFact>(this.goal);
		//		for (PGFact g : this.goal)
		//		{
		//			clone.goal.add((PGFact) g.clone());
		//		}

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


	/**
	 * Returns a list of all propositions which can appear on each layer- mutexes are not included, making this
	 * similar to an RPG
	 * @param s
	 * @return
	 */
	public TotalOrderPlan getPlan(State s)
	{
		setInitial(s);
		resetAll(s);
		//		AND oldGoal = new AND(this.goal);
		setGoal(s.goal);

		// set up the intital set of facts
		List<PGFact> scheduledFacts = new ArrayList<PGFact>(initial);
		List<PGAction> scheduledActs = null;

		scheduledActs = createFactLayer(scheduledFacts, 0);
		List plan = null;

		//
		Set<Fact> realInitial = new SetWrapper<Fact>();
		for (PGFact i : this.initial)
		{
			realInitial.add((Fact) i.fact);
		}

		this.factLayers.add(realInitial); //add current layer
		//		this.pgFactLayers.add(scheduledFacts); //add current layer

		// create the graph==========================================
		while (true)
		{
			scheduledFacts = createActionLayer(scheduledActs, num_layers);
			++num_layers;
			scheduledActs = createFactLayer(scheduledFacts, num_layers);

			if (scheduledFacts != null)
			{
				Set factList = new SetWrapper();
				//plan = extractPlan();
				for (Object pgp : scheduledFacts)
					factList.add(((PGFact)pgp).fact);

				factList.addAll(this.factLayers.get(num_layers-1));

				this.factLayers.add(factList); //add current layer

			}


			if (goalMet() && !goalMutex())
			{
				plan = extractPlan();
			}
			if (plan != null)
				break;
			if (!level_off)
				numeric_level_off = 0;
			if (level_off || numeric_level_off >= NUMERIC_LIMIT)
			{
				// printGraph();
				break;
			}
		}

		TotalOrderPlan p = null;
		if (plan != null)
		{
			p = new TotalOrderPlan();
			Iterator pit = plan.iterator();
			while (pit.hasNext())
			{
				PGAction a = (PGAction) pit.next();
				if (!(a instanceof PGNoOp))
					p.addAction(a.action);
			}
			// p.print(javaff.JavaFF.infoOutput);
			return p;
		}
		//		this.setGoal(oldGoal);

		return p;

	}

	//GET LAYER CONTAINING needs changed to reflect single literals, conjunctions, ors, etc
	public TotalOrderPlan getPlanFromExistingGraph(Fact g)
	{
		readyActions = new SetWrapper();

		this.setGoal(g);

		List plan = null;
		if (goalMet() && !goalMutex())
		{
			plan = extractPlan();
		}

		TotalOrderPlan p = null;
		if (plan != null)
		{
			p = new TotalOrderPlan();
			Iterator pit = plan.iterator();
			while (pit.hasNext())
			{
				PGAction a = (PGAction) pit.next();
				if (!(a instanceof PGNoOp))
					p.addAction(a.action);
			}
			// p.print(javaff.JavaFF.infoOutput);
			return p;
		}
		//		this.setGoal(oldGoal);

		return p;

		//		int lastLayer = 0;
		//		int i = 0;
		//		for (Fact g : goal.getFacts())
		//		{
		//			int d = this.getLayerContaining(g);
		//			if (d < 0)
		//				return null;
		//			if (d > lastLayer)
		//				lastLayer = d;
		//			
		//			++i;
		//		}
		////		this.printGraph();
		//		List plan = null;
		//		this.setGoal(goal);
		//		if (goalMet() && !goalMutex())
		//		{
		//			plan = this.searchPlan(this.goal, lastLayer); //lastLayer should be enough...
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
		//		} 
		//		else
		//			return null;

	}

	/**
	 * Build the PG until it is fully stable with the specified state (including goal), but do not construct 
	 * any kind of plan.
	 * @param s
	 * @return
	 */
	public void constructStableGraph(State init)
	{
		resetAll(init);
		setInitial(init);
		Set<PGFact> oldGoal = new SetWrapper<PGFact>(this.goal);
		setGoal(init.goal);

		// set up the intital set of facts
		List<PGFact> scheduledFacts = new ArrayList<PGFact>(this.initial);
		List<PGAction> scheduledActs = null;

		scheduledActs = createFactLayer(scheduledFacts, 0);
		List plan = null;

		//
		Set<Fact> realInitial = new SetWrapper<Fact>();
		for (PGFact i : this.initial)
		{
			realInitial.add((Fact) i.fact);
		}

		this.factLayers.add(realInitial); //add current layer
		//		this.pgFactLayers.add(scheduledFacts); //add current layer

		// create the graph==========================================
		while (true)
		{
			scheduledFacts = createActionLayer(scheduledActs, num_layers);
			++num_layers;
			scheduledActs = createFactLayer(scheduledFacts, num_layers);

			if (scheduledFacts != null)
			{
				Set factList = new SetWrapper();
				//plan = extractPlan();
				for (Object pgp : scheduledFacts)
					factList.add(((PGFact)pgp).fact);

				factList.addAll(this.factLayers.get(num_layers-1));

				this.factLayers.add(factList); //add current layer

			}


			if (!level_off)
				numeric_level_off = 0;
			if (level_off || numeric_level_off >= NUMERIC_LIMIT) //is this creating > stable layers?
			{
				// printGraph();
				break;
			}
		}
	}

	public Set getFactsAtLayer(int i)
	{
		return this.factLayers.get(i);
	}

	/**
	 * Returns the distance/layer which contains the first instance of the specified proposition.
	 * @param p
	 * @return The distance to the proposition, or -1 if it is not found in any layer.
	 */
	public int getLayerContaining(Fact p)
	{
		for (int i = 0; i < this.factLayers.size(); i++)
		{
			if (this.factLayers.get(i).contains(p))
				return i;
		}

		return -1;
	}

	public int getFactLayerSize()
	{
		return this.factLayers.size();
	}

	//	public List<List> getFactLayers(State s)
	//	{
	//		setInitial(s);
	//		resetAll(s);
	//
	//		// set up the intital set of facts
	//		Set scheduledFacts = new SetWrapper(initial);
	//		List scheduledActs = null;
	//
	//		scheduledActs = createFactLayer(scheduledFacts, 0);
	//
	//		// create the graph==========================================
	//		while (true)
	//		{
	//			scheduledFacts = createActionLayer(scheduledActs, num_layers);
	//			++num_layers;
	//			scheduledActs = createFactLayer(scheduledFacts, num_layers);
	//
	//			if (goalMet() && !goalMutex())
	//			{
	//				return factLayers; //met goal so return lists
	//			}
	//			if (!level_off)
	//				numeric_level_off = 0;
	//			if (level_off || numeric_level_off >= NUMERIC_LIMIT)
	//			{
	//				// printGraph();
	//				return null;
	//			}
	//		}
	//	}

	// ******************************************************
	// Setting it all up
	// ******************************************************
	protected void setActionMap(Set<Action> gactions)
	{
		Queue<Action> queue = new LinkedList<Action>(gactions);
		while (queue.isEmpty() == false)
		{
			Action a = queue.remove();

			PGAction pga = new PGAction(a);
			actionMap.put(a, pga);
			actions.add(pga);
		}
	}

	protected PGFact getPGFact(Fact p)
	{
		Object o = propositionMap.get(p);
		PGFact pgp;
		if (o == null)
		{
			pgp = new PGFact(p);
			propositionMap.put(p, pgp);
			propositions.add(pgp);
		} else
			pgp = (PGFact) o;
		return pgp;
	}


	protected void setLinks()
	{
		for (Object a : this.actions)
		{
			PGAction pga = (PGAction) a;

			Set<Fact> pcs = pga.action.getPreconditions();
			for (Fact p : pcs)
			{
				PGFact pgp = this.getPGFact(p);
				pga.conditions.add(pgp);
				pgp.enables.add(pga);
			}

			Set<Fact> adds = pga.action.getAddPropositions();
			for (Fact p : adds)
			{
				PGFact pgp = this.getPGFact(p);
				pga.achieves.add(pgp);
				pgp.achievedBy.add(pga);
			}

			Set<Not> dels = pga.action.getDeletePropositions();
			for (Fact p : dels)
			{
				PGFact pgp = this.getPGFact(p);
				pga.deletes.add(pgp);
				pgp.deletedBy.add(pga);
			}
		}
	}

	protected void resetAll(State s)
	{
		factLayers = new ArrayList<Set<Fact>>();

		propMutexes = new SetWrapper();
		actionMutexes = new SetWrapper();

		//		memorised = new ArrayList();

		readyActions = new SetWrapper();

		num_layers = 0;

		Iterator ait = actions.iterator();
		while (ait.hasNext())
		{
			PGAction a = (PGAction) ait.next();
			a.reset();
		}

		Iterator pit = propositions.iterator();
		while (pit.hasNext())
		{
			PGFact p = (PGFact) pit.next();
			p.reset	();
		}
	}


	protected void setGoal(Fact g)
	{
		goal = new SetWrapper();
		Iterator csit = g.getFacts().iterator();
		while (csit.hasNext())
		{
			Fact p = (Fact) csit.next();
			PGFact pgp = getPGFact(p);
			goal.add(pgp);
		}
	}

	public void setInitial(State S)
	{
		Set i = ((STRIPSState) S).facts;
		initial = new SetWrapper();
		Iterator csit = i.iterator();
		while (csit.hasNext())
		{
			Fact p = (Fact) csit.next();
			PGFact pgp = getPGFact(p);
			initial.add(pgp);
		}
	}

	protected void createNoOps()
	{
		Iterator pit = propositions.iterator();
		while (pit.hasNext())
		{
			PGFact p = (PGFact) pit.next();
			PGNoOp n = new PGNoOp(p);
			n.conditions.add(p);
			n.achieves.add(p);
			p.enables.add(n);
			p.achievedBy.add(n);
			actions.add(n);
		}
	}

	// ******************************************************
	// Graph Construction
	// ******************************************************

	protected ArrayList<PGAction> createFactLayer(List<PGFact> pFacts, int pLayer)
	{
		//		memorised.add(new SetWrapper());
		ArrayList<PGAction> scheduledActs = new ArrayList<PGAction>();
		Set<MutexPair> newMutexes = null;
		if (doMutexes) {
			newMutexes = new SetWrapper<MutexPair>();
		}
		for (PGFact f : pFacts)
		{
			if (f.layer < 0)
			{
				f.layer = pLayer;

				out: for (PGAction a : f.enables)
				{
					//					//need to check for NOTs now that ADL is supported
					//					for (PGFact apc : a.conditions)
					//					{
					//						if (apc.fact instanceof Not)
					//						{
					//							if (pFacts.contains(((Not)apc.fact).literal) == true)
					//								continue out;
					//						}
					//
					//					}

					scheduledActs.add(a);
				}
				//				scheduledActs.addAll(f.enables);
				level_off = false;
				if (doMutexes)
					// calculate mutexes
					if (pLayer != 0)
					{
						Iterator pit = propositions.iterator();
						while (pit.hasNext())
						{
							PGFact p = (PGFact) pit.next();
							if (p.layer >= 0 && this.checkPropMutex(f, p, pLayer))
							{
								this.makeMutex(f, p, pLayer, newMutexes);
								System.out.println("NEW MUTEX");
							}
						}
					}

			}
		}
		if (doMutexes) {
			// check old mutexes
			Iterator pmit = propMutexes.iterator();
			while (pmit.hasNext())
			{
				MutexPair m = (MutexPair) pmit.next();
				if (checkPropMutex(m, pLayer))
				{
					this.makeMutex(m.node1, m.node2, pLayer, newMutexes);
				} else
				{
					level_off = false;
				}
			}

			// add new mutexes to old mutexes and remove those which have
			// disappeared
			propMutexes = newMutexes;
		}
		return scheduledActs;
	}

	public boolean checkPropMutex(MutexPair m, int l)
	{
		return checkPropMutex((PGFact) m.node1, (PGFact) m.node2,
				l);
	}

	public boolean checkPropMutex(Fact p1, Fact p2, int l)
	{
		if (p1 == p2)
			return false;

		PGFact pgp1 = this.getPGFact(p1);
		PGFact pgp2 = this.getPGFact(p2);

		if (pgp1 == null || pgp2 == null)
			return false;

		return this.checkPropMutex(pgp1, pgp2, l);
	}


	protected boolean checkPropMutex(PGFact p1, PGFact p2, int l)
	{
		if (p1 == p2)
			return false;

		// Componsate for statics
		if (p1.achievedBy.isEmpty() || p2.achievedBy.isEmpty())
			return false;

		Iterator a1it = p1.achievedBy.iterator();
		while (a1it.hasNext())
		{
			PGAction a1 = (PGAction) a1it.next();
			if (a1.layer >= 0)
			{
				Iterator a2it = p2.achievedBy.iterator();
				while (a2it.hasNext())
				{
					PGAction a2 = (PGAction) a2it.next();
					if (a2.layer >= 0 && !a1.mutexWith(a2, l - 1))
						return false;
				}
			}

		}
		return true;
	}

	protected void makeMutex(Node n1, Node n2, int l, Set<MutexPair> mutexPairs)
	{
		n1.setMutex(n2, l);
		n2.setMutex(n1, l);
		mutexPairs.add(new MutexPair(n1, n2));
		
		System.out.println("New mutex!");
	}

	protected ArrayList<PGFact> createActionLayer(List<PGAction> pActions, int pLayer)
	{
		level_off = true;
		Set<PGAction> actionSet = this.getAvailableActions(pActions, pLayer);
		actionSet.addAll(readyActions);
		
		ArrayList<PGFact> scheduledFacts;
		if (doMutexes) {
			readyActions = new SetWrapper<PGAction>();
			Set<PGAction> filteredSet = this.filterSet(actionSet, pLayer);
			scheduledFacts = this.calculateActionMutexesAndProps(filteredSet,
					pLayer);
		} else {
			scheduledFacts = this.calculateActionMutexesAndProps(actionSet,
					pLayer);
		}
		return scheduledFacts;
	}

	/*
	 * 24/5/2011 - David Pattison
	 * From what I can tell, this method returns the set of actions whose preconditions have been
	 * satisfied. But this is done by comparing the number of PCs against the number of actions
	 * in the List provided which satisfy at least one of the PCs. I guess the idea is that
	 * as each PC (PGFact) is mapped to X actions which require it, when the set of actions
	 * which is passed in is constructed, if the layer at p-1 contains N PCs, there will be at least
	 * N actions in the list (hence why it is a list- allows duplicates). If |A| < N, then the actions
	 * cannot be applicable. This is a both a genius and terrible way to do this.
	 */
	protected Set<PGAction> getAvailableActions(List<PGAction> pActions, int pLayer)
	{
		STRIPSState hackState = new STRIPSState();
		hackState.facts = new SetWrapper<Proposition>();
		for (Fact f : this.factLayers.get(pLayer))
		{
			if (f instanceof Proposition)
				hackState.facts.add((Proposition) f);
		}

		Set<PGAction> actionSet = new SetWrapper<PGAction>();
		for (PGAction a : pActions)
		{		
			if (a.layer < 0)
			{
				a.counter++;
				a.difficulty += pLayer;
				if (a instanceof PGNoOp || a.action.isApplicable(hackState))
				{
					actionSet.add(a);
					level_off = false;
				}
			}

			//			if (a.layer < 0)
			//			{
			//				a.counter++;
			//				a.difficulty += pLayer;
			//				if (a.counter >= a.conditions.size())
			//				{
			//					actionSet.add(a);
			//					level_off = false;
			//				}
			//			}
		}
		return actionSet;
	}

	protected Set<PGAction> filterSet(Set<PGAction> pActions, int pLayer)
	{
		Set<PGAction> filteredSet = new SetWrapper<PGAction>();
		for (PGAction a : pActions)
		{
			if (this.noMutexes(a.conditions, pLayer)) {// true 
				filteredSet.add(a);
				//System.out.println("NOMUTEX");
			} else {
				readyActions.add(a);
				System.out.println("MUTEX");
			}
		}
		return filteredSet;
	}

	public ArrayList<PGFact> calculateActionMutexesAndProps(Set<PGAction> filteredSet, int pLayer)
	{
		Set<MutexPair> newMutexes = new SetWrapper<MutexPair>();

		ArrayList<PGFact> scheduledFacts = new ArrayList<PGFact>();

		for (PGAction a : filteredSet)
		{
			scheduledFacts.addAll(a.achieves);
			a.layer = pLayer;
			level_off = false;
			if (doMutexes) {
				// caculate new mutexes
				Iterator a2it = actions.iterator();
				while (a2it.hasNext())
				{
					PGAction a2 = (PGAction) a2it.next();
					if (a2.layer >= 0 && checkActionMutex(a, a2, pLayer))
					{
						System.out.println("adding action mutex at layer "+pLayer+"- "+a2+" <-> "+a);
						this.makeMutex(a, a2, pLayer, newMutexes);
					}
				}
			}
		}
		if (doMutexes) {
			// check old mutexes
			Iterator amit = actionMutexes.iterator();
			while (amit.hasNext())
			{
				MutexPair m = (MutexPair) amit.next();
				if (checkActionMutex(m, pLayer))
				{
					this.makeMutex(m.node1, m.node2, pLayer, newMutexes);
				} 
				else
				{
					level_off = false;
				}
			}

			// add new mutexes to old mutexes and remove those which have
			// disappeared
			actionMutexes = newMutexes;
		}
		return scheduledFacts;
	}

	public boolean checkActionMutex(MutexPair m, int l)
	{
		return checkActionMutex((PGAction) m.node1, (PGAction) m.node2, l);
	}

	public boolean checkActionMutex(PGAction a1, PGAction a2, int l)
	{
		if (a1 == a2)
			return false;

		Iterator p1it = a1.deletes.iterator();
		while (p1it.hasNext())
		{
			PGFact p1 = (PGFact) p1it.next();
			;
			if (a2.achieves.contains(p1))
				return true;
			if (a2.conditions.contains(p1))
				return true;
		}

		Iterator p2it = a2.deletes.iterator();
		while (p2it.hasNext())
		{
			PGFact p2 = (PGFact) p2it.next();
			if (a1.achieves.contains(p2))
				return true;
			if (a1.conditions.contains(p2))
				return true;
		}

		Iterator pc1it = a1.conditions.iterator();
		while (pc1it.hasNext())
		{
			PGFact p1 = (PGFact) pc1it.next();
			Iterator pc2it = a2.conditions.iterator();
			while (pc2it.hasNext())
			{
				PGFact p2 = (PGFact) pc2it.next();
				if (p1.mutexWith(p2, l))
					return true;
			}
		}

		return false;
	}

	protected boolean goalMet()
	{
		for (PGFact p : this.goal)
		{
			if (p.layer < 0)
			{
				return false;
			}
		}
		return true;
	}

	// false
	protected boolean goalMutex()
	{
		return !noMutexes(this.goal, num_layers);
	}

	// true
	protected boolean noMutexes(Set s, int l)
	{
		Iterator sit = s.iterator();
		if (sit.hasNext())
		{
			Node n = (Node) sit.next();
			Set s2 = new SetWrapper(s);
			s2.remove(n);
			Iterator s2it = s2.iterator();
			while (s2it.hasNext())
			{
				Node n2 = (Node) s2it.next();
				if (n.mutexWith(n2, l))
					return false;
			}
			return noMutexes(s2, l);
		} else
			return true;
	}

	// true
	protected boolean noMutexesTest(Node n, Set s, int l) 
	// Tests to see if
	// there is a mutex
	// between n and all
	// nodes in s
	{
		if (!doMutexes)
			return true;
		Iterator sit = s.iterator();
		while (sit.hasNext())
		{
			Node n2 = (Node) sit.next();
			if (n.mutexWith(n2, l))
				return false;
		}
		return true;
	}

	// ******************************************************
	// Plan Extraction
	// ******************************************************

	public List extractPlan()
	{
		return searchPlan(this.goal, num_layers);
	}

	public List searchPlan(Set goalSet, int l)
	{

		if (l == 0)
		{
			if (initial.containsAll(goalSet))
				return new ArrayList();
			else
				return null;
		}
		// do memorisation stuff
		//		Set badGoalSet = (Set) memorised.get(l);
		//		if (badGoalSet.contains(goalSet))
		//			return null;

		List ass = searchLevel(goalSet, (l - 1)); // returns a set of sets of
		// possible action
		// combinations
		Iterator assit = ass.iterator();

		while (assit.hasNext()) //go round each NON-mutex set
		{
			Set as = (Set) assit.next();
			Set newgoal = new SetWrapper();

			Iterator ait = as.iterator();
			while (ait.hasNext())
			{
				PGAction a = (PGAction) ait.next(); //construct a new goal set from the non-mutex action set's effects
				newgoal.addAll(a.conditions);
			}

			List al = searchPlan(newgoal, (l - 1)); //try to find a plan to this new goal
			if (al != null)
			{
				List plan = new ArrayList(al);
				plan.addAll(as);
				return plan; //if a plan was found, return it, else loop to the next set.
			}

		}

		// do more memorisation stuff
		//		badGoalSet.add(goalSet);
		return null;

	}


	public List searchLevel(Set goalSet, int layer)
	{
		if (goalSet.isEmpty())
		{
			Set s = new SetWrapper();
			List li = new ArrayList();
			li.add(s);
			return li;
		}

		List actionSetList = new ArrayList();
		Set newGoalSet = new SetWrapper(goalSet);

		Iterator git = goalSet.iterator();
		PGFact g = (PGFact) git.next();
		newGoalSet.remove(g);

		//always prefer No-ops
		for (PGAction a : g.achievedBy)
		{
			//			System.out.println("Checking "+a+" for no op");
			if ((a instanceof PGNoOp) && a.layer <= layer && a.layer >= 0)
			{
				Set newnewGoalSet = new SetWrapper(newGoalSet);
				newnewGoalSet.removeAll(a.achieves);

				List l = this.searchLevel(newnewGoalSet, layer);

				Iterator lit = l.iterator();
				while (lit.hasNext())
				{
					Set s = (Set) lit.next();
					if (this.noMutexesTest(a, s, layer))
					{
						s.add(a);
						actionSetList.add(s);
					}
				}
			}
		}

		for (PGAction a : g.achievedBy)
		{
			//ignore no-ops
			if (!(a instanceof PGNoOp) && a.layer <= layer && a.layer >= 0)
			{
				Set newnewGoalSet = new SetWrapper(newGoalSet); //copy over current goal set
				newnewGoalSet.removeAll(a.achieves); //remove all facts achieved by A
				List l = this.searchLevel(newnewGoalSet, layer);
				Iterator lit = l.iterator();
				while (lit.hasNext())
				{
					Set s = (Set) lit.next();
					if (this.noMutexesTest(a, s, layer))
					{
						s.add(a);
						actionSetList.add(s);
					}
				}
			}
		}
		//		System.out.println("Found action list for "+g+": "+actionSetList);

		return actionSetList;
	}

	// ******************************************************
	// Useful Methods
	// ******************************************************

	public int getLayer(Action a)
	{
		PGAction pg = (PGAction) actionMap.get(a);
		return pg.layer;
	}

	// ******************************************************
	// protected Classes
	// ******************************************************
	public class Node
	{
		public int layer;
		//public Set mutexes;

		public Map mutexTable= new MapWrapper();;

		public Node()
		{
		}

		public void reset()
		{
			layer = -1;
			//mutexes = new SetWrapper();
			mutexTable.clear();
		}

		public void setMutex(Node n, int l)
		{
			System.out.println("NEW MUTEX!");
			n.mutexTable.put(this, new Integer(l));
			this.mutexTable.put(n, new Integer(l));
		}
		// false
		public boolean mutexWith(Node n, int l)
		{
			/*
			 * if (this == n) return false; Iterator mit = mutexes.iterator();
			 * while (mit.hasNext()) { Mutex m = (Mutex) mit.next(); if
			 * (m.contains(n)) { return m.layer >= l; } } return false;
			 */
			Object o = mutexTable.get(n);
			if (o == null)
				return false;
			Integer i = (Integer) o;
			return i.intValue() >= l;
		}
	}

	public class PGAction extends Node
	{
		public Action action;
		public int counter, difficulty;

		public Set<PGFact> conditions = new SetWrapper<PGFact>();
		public Set<PGFact> achieves = new SetWrapper<PGFact>();
		public Set<PGFact> deletes = new SetWrapper<PGFact>();

		public PGAction()
		{

		}

		public PGAction(Action a)
		{
			action = a;
		}

		public Set getComparators()
		{
			return action.getComparators();
		}

		public Set getOperators()
		{
			return action.getOperators();
		}

		public void reset()
		{
			super.reset();
			counter = 0;
			difficulty = 0;
		}

		public String toString()
		{
			return action.toString();
		}
	}

	public class PGNoOp extends PGAction
	{
		private final Set EmptySet = new SetWrapper();
		public PGFact proposition;

		public PGNoOp(PGFact p)
		{
			proposition = p;
		}

		public String toString()
		{
			return ("No-Op " + proposition);
		}

		public Set getComparators()
		{
			return this.EmptySet;
		}

		public Set getOperators()
		{
			return this.EmptySet;
		}
	}

	public class PGSingleLiteral extends PGFact
	{

		public PGSingleLiteral(SingleLiteral p)
		{
			super(p);
		}

	}

	public class PGFact extends Node
	{
		public Fact fact;

		public Set<PGAction> enables = new SetWrapper<PGAction>();
		public Set<PGAction> achievedBy = new SetWrapper<PGAction>();
		public Set<PGAction> deletedBy = new SetWrapper<PGAction>();

		public PGFact(Fact p)
		{
			fact = p;
		}

		public String toString()
		{
			return fact.toString();
		}

		//		public Object clone()
		//		{
		//			PGFact clone = new PGFact((Fact) fact.clone());
		//			clone.enables = new SetWrapper<PlanningGraph.PGAction>();
		//			clone.
		//		}
	}

	protected class MutexPair
	{
		public Node node1, node2;

		public MutexPair(Node n1, Node n2)
		{
			node1 = n1;
			node2 = n2;
		}
	}

	// ******************************************************
	// Debugging Classes
	// ******************************************************
	public void printGraph()
	{
		for (int i = 0; i <= num_layers; ++i)
		{
			System.out.println("-----Layer " + i
					+ "----------------------------------------");
			printLayer(i);
		}
		System.out
		.println("-----End -----------------------------------------------");
	}

	public void printLayer(int i)
	{
		System.out.println("Facts:");
		Iterator pit = propositions.iterator();
		while (pit.hasNext())
		{
			PGFact p = (PGFact) pit.next();
			if (p.layer <= i && p.layer >= 0)
			{
				System.out.println("\t" + p);
				System.out.println("\t\tmutex with");
				Iterator mit = p.mutexTable.keySet().iterator();
				while (mit.hasNext())
				{
					PGFact pm = (PGFact) mit.next();
					Integer il = (Integer) p.mutexTable.get(pm);
					if (il.intValue() >= i)
					{
						System.out.println("\t\t\t" + pm);
					}
				}
			}
		}
		if (i == num_layers)
			return;
		System.out.println("Actions:");
		Iterator ait = actions.iterator();
		while (ait.hasNext())
		{
			PGAction a = (PGAction) ait.next();
			if (a.layer <= i && a.layer >= 0)
			{
				System.out.println("\t" + a);
				System.out.println("\t\tmutex with");
				Iterator mit = a.mutexTable.keySet().iterator();
				while (mit.hasNext())
				{
					PGAction am = (PGAction) mit.next();
					Integer il = (Integer) a.mutexTable.get(am);
					if (il.intValue() >= i)
					{
						System.out.println("\t\t\t" + am);
					}
				}
			}
		}
	}

}