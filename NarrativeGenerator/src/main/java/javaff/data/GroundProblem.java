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

package javaff.data;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javaff.data.adl.ADLFact;
import javaff.data.adl.Exists;
import javaff.data.adl.ForAll;
import javaff.data.adl.Imply;
import javaff.data.adl.Or;
import javaff.data.metric.BinaryComparator;
import javaff.data.metric.Function;
import javaff.data.metric.NamedFunction;
import javaff.data.metric.ResourceOperator;
import javaff.data.strips.And;
import javaff.data.strips.Equals;
import javaff.data.strips.InstantAction;
import javaff.data.strips.Not;
import javaff.data.strips.NullFact;
import javaff.data.strips.Proposition;
import javaff.data.strips.STRIPSFact;
import javaff.data.strips.SingleLiteral;
import javaff.data.strips.TrueCondition;
import javaff.data.temporal.DurativeAction;
import javaff.planning.MetricState;
import javaff.planning.RelaxedMetricPlanningGraph;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.RelaxedTemporalMetricPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.TemporalMetricState;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

public class GroundProblem implements Cloneable
{
	public String name;
	public Set<Action> actions = new SetWrapper<Action>();
	public Map<NamedFunction, BigDecimal> functionValues = new MapWrapper<NamedFunction, BigDecimal>();
	public Metric metric;
	
	public boolean isMetric, isTemporal;

	public GroundFact goal;
	public Set<Proposition> initial;
	public List<GroundFact> sometimes = new LinkedList<GroundFact>();

	public List<GroundFact> sometimesb = new LinkedList<GroundFact>();
	public List<GroundFact> sometimesa = new LinkedList<GroundFact>();
	public List<GroundFact> always = new LinkedList<GroundFact>();
	/**
	 * A map parameters to all propositions which contain it.
	 */
	public Map<Parameter, Set<Proposition>> objectPropositionMap; 
	
	/**
	 * A set of all grounded propositions which can be in the domain. This will include
	 * all propositions which could exist, but do not appear in the init or goal conditions, and are not preconditions
	 * or effects of any actions.
	 */
	public Set<Proposition> groundedPropositions;
	public Set<Proposition> reachableFacts;
	public Set<Proposition> staticFacts;

	public TemporalMetricState tmstate = null;
	public MetricState mstate = null;
	public STRIPSState state = null;
	
	public Set<Parameter> objects;
	
	public Object clone()
	{
		GroundProblem clone = new GroundProblem(new SetWrapper<Action>(this.actions),
												new SetWrapper<Proposition>(this.initial),
												(GroundFact)this.goal.clone(),
												new MapWrapper<NamedFunction, BigDecimal>(this.functionValues),
												this.metric);
		if (this.state != null)
			clone.state = (STRIPSState) this.state.clone();
		if (this.mstate != null)
			clone.mstate = (MetricState) this.mstate.clone();
		if (this.tmstate != null)
			clone.tmstate = (TemporalMetricState) this.tmstate.clone();
		
		clone.groundedPropositions = new SetWrapper<Proposition>(this.groundedPropositions);
		clone.reachableFacts = new SetWrapper<Proposition>(this.reachableFacts);
		clone.name = this.name;
		clone.objects = new SetWrapper<Parameter>(this.objects);
		clone.objectPropositionMap = new MapWrapper<Parameter, Set<Proposition>>(this.objectPropositionMap);
		return clone;
												
	}

	public GroundProblem(Set<Action> a, Set<Proposition> i, GroundFact g, Map<NamedFunction, BigDecimal> f, Metric m)
	{
		actions = a;
		initial = i;
		goal = g;
		functionValues = f;
		metric = m;
		name = "unknown";
		this.reachableFacts = new SetWrapper<Proposition>();
		this.staticFacts = new SetWrapper<Proposition>();
//		this.state = this.getTemporalMetricInitialState();
		this.objects = new SetWrapper<Parameter>();
		this.objectPropositionMap = new MapWrapper<Parameter, Set<Proposition>>();
		
		extractPddlObjects();
		computeGroundedProps();
		makeAllLowerCase();
		createTypePropositionMap();
		
		this.reachableFacts = new SetWrapper<Proposition>(this.groundedPropositions);
	}
	
	protected void createTypePropositionMap()
	{
		this.objectPropositionMap.clear();
		for (Proposition p : this.groundedPropositions)
		{
			for (Parameter par : p.getParameters())
			{
				if (this.objectPropositionMap.containsKey(par) == false)
					this.objectPropositionMap.put(par, new SetWrapper<Proposition>());
				
				this.objectPropositionMap.get(par).add(p);
			}
		}
	}

	protected void extractPddlObjects()
	{
		this.objects.clear();
		for (Action a : this.actions)
		{
			this.objects.addAll(a.parameters);
		}
	}

	protected void makeAllLowerCase()
	{
		//	action, goal, initial, groundedprops
		Map<String, Fact> lookup = new MapWrapper<String, Fact>();
		for (Fact p : this.groundedPropositions)
		{
			lookup.put(p.toString().toLowerCase(), p);
		}
	}
	
	/**
	 * This method extracts all individual facts which can exist in the domain by decompiling out all variants of a Fact (ADL, Equals etc). Anything which 
	 * exists in the domain, such as an Equality fact, is ignored.
	 */
	protected void computeGroundedProps()
	{
		this.groundedPropositions = new SetWrapper<Proposition>();
		this.groundedPropositions.addAll(initial); //add statics?
		for (Proposition p : this.initial)
			if (p.isStatic())
				this.staticFacts.add(p);
		
		for (Action a : this.actions)
		{
			for (Fact pc : a.getPreconditions())
			{
				Collection<Fact> c = this.decompileFact(pc);
				for (Fact f : c)
				{
					if (f instanceof Not && ((Not)f).literal instanceof Proposition)
						this.groundedPropositions.add((Proposition) ((Not) f).literal);
					else if (f instanceof Proposition)
						this.groundedPropositions.add((Proposition) f);
					
					if (f instanceof Proposition && f.isStatic())
						this.staticFacts.add((Proposition) f);
				}
			}
			
			for (Fact add : a.getAddPropositions())
			{
				Collection<Fact> c = this.decompileFact(add);
				for (Fact f : c)
				{
					if (f instanceof Not && ((Not)f).literal instanceof Proposition)
						this.groundedPropositions.add((Proposition) ((Not) f).literal);
					else if (f instanceof Proposition)
						this.groundedPropositions.add((Proposition) f);

					if (f instanceof Proposition && f.isStatic())
						this.staticFacts.add((Proposition) f);
				}
			}
			
			for (Not del : a.getDeletePropositions())
			{
				Collection<Fact> c = this.decompileFact(del);
				for (Fact f : c)
				{
					if (f instanceof Not && ((Not)f).literal instanceof Proposition)
						this.groundedPropositions.add((Proposition) ((Not) f).literal);
					else if (f instanceof Proposition)
						this.groundedPropositions.add((Proposition) f);

					if (f instanceof Proposition && f.isStatic())
						this.staticFacts.add((Proposition) f);
				}
			}
		}
	}

	/**
	 * This helper method deconstructs any Fact into individual literals for use
	 * in the planning graph. If a new type is introduced into the hierarchy, this will
	 * probably need modified.
	 * 
	 * @param f
	 * @return The various facts which the parameter decompiles into.
	 */
	protected static Collection<Fact> decompileFact(Fact f)
		{
		Set<Fact> decompiled = new SetWrapper<Fact>(); 
	
		if (f instanceof NullFact || f instanceof Equals || f instanceof TrueCondition)
		{
			//don't want to decompile these fact types
			decompiled.add(f);
		}
		else if (f instanceof ADLFact)
		{
			Collection<? extends STRIPSFact> strips = ((ADLFact) f).toSTRIPS();
			for (STRIPSFact stripsFact : strips)
			{
				decompiled.addAll(GroundProblem.decompileFact(stripsFact));
			}
		}
		else if (f instanceof And || f instanceof Or)
		{
			for (Fact subFact : f.getFacts())
		{
				decompiled.addAll(GroundProblem.decompileFact(subFact));
		}
		}
		else if (f instanceof SingleLiteral || f instanceof Function || f instanceof BinaryComparator || f instanceof ResourceOperator)
		{
			decompiled.add(f);
		}
		else if (f instanceof Not)
		{
			//Nots are a special case, and a very annoying one at that. The facts which are held inside the original Not, must themselves
			//be decompiled, but then re-wrapped once the method returns.
			Collection<Fact> needsNotted = GroundProblem.decompileFact(((Not)f).literal);
			for (Fact nn : needsNotted)
		{
				Not wrapped = new Not(nn);
				decompiled.add(wrapped);
		}
		}
		else
			throw new IllegalArgumentException("Cannot decompile fact "+f+" - unknown type: "+f.getClass());
		
		return decompiled;
	}

	public STRIPSState getSTRIPSInitialState()
	{
		if (this.state == null)
		{
			STRIPSState s = new STRIPSState(actions, initial, goal);
			s.setRPG(new RelaxedPlanningGraph(this));
			this.state = s;
		}
		return state;
	}

	public MetricState getMetricInitialState()
	{
		if (this.mstate == null)
		{
			MetricState ms = new MetricState(actions, initial, goal,
					functionValues, metric);
			ms.setRPG(new RelaxedMetricPlanningGraph(this));
			this.mstate = ms;
		}
		return mstate;
	}
	
	public STRIPSState recomputeSTRIPSInitialState()
	{
		STRIPSState s = new STRIPSState(actions, initial, goal);
		s.setRPG(new RelaxedPlanningGraph(this));
		this.state = s;
		
		return this.state;
	}

	public MetricState recomputeMetricInitialState()
	{
		MetricState ms = new MetricState(actions, initial, goal,
				functionValues, metric);
		ms.setRPG(new RelaxedMetricPlanningGraph(this));
		this.mstate = ms;
		
		return mstate;
	}

	public TemporalMetricState getTemporalMetricInitialState()
	{
		if (tmstate == null)
		{
			Set na = new SetWrapper();
			Set ni = new SetWrapper();
			Iterator ait = actions.iterator();
			while (ait.hasNext())
			{
				Action act = (Action) ait.next();
				if (act instanceof InstantAction)
				{
					na.add(act);
					ni.add(act);
				} else if (act instanceof DurativeAction)
				{
					DurativeAction dact = (DurativeAction) act;
					na.add(dact.startAction);
					na.add(dact.endAction);
					ni.add(dact.startAction);
				}
			}
			TemporalMetricState ts = new TemporalMetricState(ni, initial, goal,
					functionValues, metric);
			GroundProblem gp = new GroundProblem(na, initial, goal,
					functionValues, metric);
			gp.name = this.name;
			gp.reachableFacts = new SetWrapper<Proposition>(this.reachableFacts);
			ts.setRPG(new RelaxedTemporalMetricPlanningGraph(gp));
			tmstate = ts;
		}
		return tmstate;
	}
	

	public TemporalMetricState recomputeTemporalMetricInitialState()
	{
		Set na = new SetWrapper();
		Set ni = new SetWrapper();
		Iterator ait = actions.iterator();
		while (ait.hasNext())
		{
			Action act = (Action) ait.next();
			if (act instanceof InstantAction)
			{
				na.add(act);
				ni.add(act);
			} else if (act instanceof DurativeAction)
			{
				DurativeAction dact = (DurativeAction) act;
				na.add(dact.startAction);
				na.add(dact.endAction);
				ni.add(dact.startAction);
			}
		}
		TemporalMetricState ts = new TemporalMetricState(ni, initial, goal,
				functionValues, metric);
		GroundProblem gp = new GroundProblem(na, initial, goal,
				functionValues, metric);
		ts.setRPG(new RelaxedTemporalMetricPlanningGraph(gp));
		tmstate = ts;	
		
		return tmstate;
	}

	@Override
	public String toString() 
	{
		return "GroundProblem: "+this.name;
	}

	/**
	 * By default, GroundProblems can contain ADL actions and predicates. Calling this will
	 * convert the problem into a STRIPS-only form, by removing ADL from actions and replacing them with
	 * STRIPS-equivalent actions. 
	 * @param ground
	 */
	public void decompileADL()
	{
		Set<Action> refinedActions = new SetWrapper<Action>();
		
		//FIXME this is pretty badly written, and incomplete. Should be recursive -- too many assumptions on format of data
		//keep a queue of potentially-ADL actions. Add partially compiled out actions to it. When
		//no ADL constructs exist in the PCs (actions unsupported for now), it can be added to the set of
		//legal actions.
		Queue<Action> queue = new LinkedList<Action>(this.actions);
		out: while (queue.isEmpty() == false)
		{
			Action a = queue.remove();
			
			for (Fact pc : a.getPreconditions())
			{
				if (this.decompileAction(pc, a, queue) == true) //if the action needed changed and requires to be reparsed
				{
					continue out;
				}
			}
			
			for (Fact add : a.getAddPropositions())
			{
				if (this.decompileAction(add, a, queue) == true) //if the action needed changed and requires to be reparsed
					{
					continue out;
				}
			}
			
			for (Not del : a.getDeletePropositions())
			{
				if (this.decompileAction(del, a, queue) == true) //if the action needed changed and requires to be reparsed
				{
					continue out;
				}
			}
			
//			if (refinedActions.contains(a))
//				System.out.println("already here");
			
			refinedActions.add(a);
			
		}
		
		
		Collection<Fact> newGoals = GroundProblem.decompileFact(this.goal);
		this.goal = new And(newGoals);
		
		this.actions = refinedActions;
		
	}
	
	protected boolean decompileAction(Fact pc, Action a, Queue<Action> queue)
	{
		//TODO now that ADLFact has toSTRIPS(), most of these if-elses can be combined into a single if (pc instanceof ADLFact)
		if (pc instanceof SingleLiteral) //if a simple proposition, then just return
		{
			return false;
		}
		else if (pc instanceof And)
		{
			for (Fact f : pc.getFacts())
			{
				boolean res = this.decompileAction(f, a, queue);
				if (res == true)
				{
					return true;
				}
				//if false, then no modification to the action was required, so continue on as normal				
			}
			
			return true;
		}
		else if (pc instanceof Imply)
		{
			Collection<? extends STRIPSFact> strips = ((Imply)pc).toSTRIPS();
			Set<STRIPSFact> satisfiableStrips = new SetWrapper<STRIPSFact>();
			for (STRIPSFact and : strips)
			{
						if (this.isFactSatisfiable(and))
							satisfiableStrips.add(and);
					}
					
					//if the STRIPS version of the Imply cannot be achieved, then remove the Imply
					//conditions from the original action and re-add it to the queue
					if (satisfiableStrips.isEmpty())
					{
						InstantAction actionClone = (InstantAction) a.clone();
						And modifiedPCs = null;
						if (actionClone.getCondition() instanceof And)
						{
							modifiedPCs = (And) actionClone.getCondition().clone();
						}
						else //not an AND, so make it one
						{
							modifiedPCs = new And((Fact)actionClone.getCondition().clone());
						}
						
						modifiedPCs.literals.remove(pc);
						
						actionClone.setCondition(modifiedPCs);
						queue.add(actionClone);
					}
			else
					{
				for (STRIPSFact stripsPC : satisfiableStrips)
				{
						InstantAction actionClone = (InstantAction) a.clone();
						And modifiedPCs = null;
						if (actionClone.getCondition() instanceof And)
						{
							modifiedPCs = (And) actionClone.getCondition().clone();
						}
						else //not an AND, so make it one
						{
							modifiedPCs = new And((Fact)actionClone.getCondition().clone());
						}
						
						modifiedPCs.literals.remove(pc);

					modifiedPCs.addAll((Collection<Fact>) stripsPC.getFacts());
//							modifiedPCs.add(stripsPC);
						
						actionClone.setCondition(modifiedPCs);
						queue.add(actionClone);
					}
				}
			return true;
		}
				else if (pc instanceof ForAll)
				{
					//if it is grounded this should be a single And
			And compiledOut = (And) ((ForAll)pc).toSTRIPS().iterator().next();
					
					if (a instanceof InstantAction)
					{
						InstantAction actionClone = (InstantAction) a.clone();
				
						Set<Fact> modifiedPCs = a.getPreconditions();
						modifiedPCs.remove(pc);

						modifiedPCs.add(compiledOut);
						
						actionClone.setCondition(new And(modifiedPCs));
						queue.add(actionClone);
					}
					else
					{
				throw new IllegalArgumentException("Durative actions not supported in ADL decompilation");
//						DurativeAction stripsClone = (DurativeAction) a.clone();
//						
//						stripsClone.startCondition = and;
//						queue.add(stripsClone);
					}
			return true;
				}
				else if (pc instanceof Exists)
				{
			//TODO validate that this actually works!
			Collection<? extends STRIPSFact> strips = ((Exists)pc).toSTRIPS();
			if (a instanceof InstantAction)
			{
				for (STRIPSFact sf : strips)
				{
					InstantAction actionClone = (InstantAction) a.clone();
					
					Set<Fact> modifiedPCs = a.getPreconditions();
					modifiedPCs.remove(pc);
					
					modifiedPCs.add(sf);
					
					actionClone.setCondition(new And(modifiedPCs));
					queue.add(actionClone);
				}
			}
			
		}
				else if (pc instanceof Or)
				{
					throw new IllegalArgumentException("Decompiling Or not yet supported");
				}
				else if (pc instanceof Not)
				{
			boolean res = this.decompileAction(((Not) pc).literal, a, queue);
			if (res == true)
				return true;
		}
		
		return false;
	}

	private boolean isFactSatisfiable(Fact fact)
	{
		for (Fact f : fact.getFacts())
		{
			if (f.isStatic() && this.staticFacts.contains(f) == false)
				return false;
		}
		
		return true;
	}
}
