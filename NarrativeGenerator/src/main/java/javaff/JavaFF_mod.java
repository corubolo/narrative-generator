/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 * parts Copyright 2012 Fabio Corubolo
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

package javaff;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.GroundProblem;
import javaff.data.Metric;
import javaff.data.Parameter;
import javaff.data.Plan;
import javaff.data.TimeStampedPlan;
import javaff.data.TotalOrderPlan;
import javaff.data.UngroundFact;
import javaff.data.UngroundProblem;
import javaff.data.adl.ADLFact;
import javaff.data.adl.Exists;
import javaff.data.adl.ForAll;
import javaff.data.adl.Imply;
import javaff.data.metric.NumberFunction;
import javaff.data.strips.And;
import javaff.data.strips.Equals;
import javaff.data.strips.InstantAction;
import javaff.data.strips.Not;
import javaff.data.strips.Operator;
import javaff.data.strips.PDDLObject;
import javaff.data.strips.Predicate;
import javaff.data.strips.PredicateSymbol;
import javaff.data.strips.Proposition;
import javaff.data.strips.STRIPSFact;
import javaff.data.strips.UngroundInstantAction;
import javaff.data.strips.Variable;
import javaff.data.temporal.DurativeAction;
import javaff.parsermg.*;
import javaff.planning.HelpfulFilter;
import javaff.planning.MetricState;
import javaff.planning.NullFilter;
import javaff.planning.RelaxedPlanningGraph;
import javaff.planning.RelaxedTemporalMetricPlanningGraph;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.planning.TemporalMetricState;
import javaff.scheduling.JavaFFScheduler;
import javaff.scheduling.Scheduler;
import javaff.scheduling.SchedulingException;
import javaff.search.BestFirstSearch;
import javaff.search.EnforcedHillClimbingSearch;
import javaff.search.Search;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

/**
 * An implementation of the FF planner in Java. The planner currently only supports STRIPS style planning, but as it is a 
 * branch of the CRIKEY planner, the components for both Temporal and Metric planning exist, but are unused.
 * 
 * @author Keith Halsey
 * @author Amanda Coles
 * @author Andrew Coles
 * @author David Pattison
 * @author Fabio Corubolo
 */
public class JavaFF_mod
{
	public static BigDecimal EPSILON = new BigDecimal(0.01);
	public static BigDecimal MAX_DURATION = new BigDecimal("100000");
	public long seed = System.nanoTime();
	public static Random generator = new Random();

	public File domainFile;

	private File useOutputFile;
	/**
	 * Improves speed of planning at the cost of some possible solutions -- better leave to false!
	 */
	public boolean doFilterReachableFacts = false;

	public static Logger logger = Logger.getLogger("javaff.JavaFF");

	public JavaFF_mod(String domain, File solutionFile)
	{
		this.domainFile = new File(domain);
		this.useOutputFile = solutionFile;
	}

	public JavaFF_mod(File domain, File solutionFile)
	{
		this.domainFile = domain;
		this.useOutputFile = solutionFile;
	}

	public static void main(String args[])
	{
		Long seed2 = null;
		SetWrapper.setRandom(true);
		MapWrapper.setRandom(true);

		//		try {Thread.sleep(10000);
		//		
		//		} catch (Exception x) {};
		//		Handler h = new StreamHandler(System.out, new SimpleFormatter());
		//		h.setLevel(Level.ALL);
		//		logger.addHandler(h);
		//		logger.setLevel(Level.ALL);
		//		logger.setUseParentHandlers(false);

		EPSILON = EPSILON.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		MAX_DURATION = MAX_DURATION.setScale(2, BigDecimal.ROUND_HALF_EVEN);


		if (args.length < 2)
		{
			System.out
			.println("Parameters needed: domainFile.pddl problemFile.pddl [random seed] [outputfile.sol]");

		} 
		else
		{
			File domainFile = new File(args[0]);
			File problemFile = new File(args[1]);
			File solutionFile = null;
			if (args.length > 2)
			{
				if (args[2].endsWith(".sol") || args[2].endsWith(".soln"))
				{
					solutionFile = new File(args[2]);
				}
				else
				{
					seed2 = Long.valueOf(args[2]);

					if (args.length > 3)
					{
						solutionFile = new File(args[3]);
					}
				}
			}
			JavaFF_mod planner = new JavaFF_mod(domainFile, solutionFile);
			planner.seed = seed2.longValue();
			planner.generator = new Random(planner.seed);
			planner.doFilterReachableFacts = false;
			planner.plan(problemFile);
		}
	}

	/**
	 * Constructs plans over several problem files.
	 * 
	 * @param path The path to the folder containing the problem files.
	 * @param filenamePrefix The prefix of each problem file, usually "pfile".
	 * @param pfileStart The start index which will be appended to the filenamePrefix.
	 * @param pfileEnd The index of the last problem file.
	 * @param usePDDLpostfix Whether to use ".pddl" at the end of the problem files. Domains are assumed to already have this.
	 * @return A totally ordered plan.
	 */
	public List<Plan> plan(String path, String filenamePrefix, int pfileStart, int pfileEnd, boolean usePDDLpostfix)
	{
		List<Plan> plans = new ArrayList<Plan>(pfileEnd - pfileStart);
		for (int i = pfileStart; i < pfileEnd; i++)
		{
			String postfix = ""+i;
			if (i < 10)
				postfix = "0"+i;
			if (usePDDLpostfix)
				postfix = postfix+".pddl";

			File pfile = new File(path+"/"+filenamePrefix+postfix);
			plans.add(this.plan(pfile));
		}

		return plans;
	}


	/**
	 * Construct a plan for the single problem file provided. Obviously this problem must be intended for the domain
	 * associated with this object. @see JavaFF.getDomainFile(). Note- This method should only be called if
	 * there exists no UngroundProblem or GroundProblem instance in the program. Otherwise, use 
	 * plan(GroundProblem, String).
	 * @param pFile The file to parse.
	 * @return A totally ordered plan.
	 */
	public Plan plan(File pFile)
	{
		Plan plan = this.doFilePlan(pFile);

		if (plan != null)
		{
			if (useOutputFile != null)
			{
				writePlanToFile(plan, useOutputFile);
			}
		}


		return plan;
	}

	protected boolean isGoalValid(GroundProblem problem, GroundFact goal)
	{
		Set<? extends Fact> facts = goal.getFacts();

		for (Fact f : facts)
		{
			if (f instanceof Not)
			{
				if (problem.groundedPropositions.contains(((Not)f).literal) == false)
					return false;
			}
			else //TODO checks for Quantified facts etc.
			{
				if (problem.groundedPropositions.contains(f) == false)
					return false;
			}
		}

		return true;
	}



	public Plan plan(Reader domain, Reader problem)
	{
		// ********************************
		// Parse and Ground the Problem
		// ********************************
		long startTime = System.currentTimeMillis();
		UngroundProblem unground = null;
		try {
			unground = PDDL21parser.parseStreams(domain, problem);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (unground == null)
		{
			System.err.println("Parsing error - see console for details");
			return null;
		}

		GroundProblem ground = ground(unground);

		return this.doPlan(ground);

	}


	protected Plan doFilePlan(File pFile)
	{
		// ********************************
		// Parse and Ground the Problem
		// ********************************
		long startTime = System.currentTimeMillis();
		UngroundProblem unground = PDDL21parser.parseFiles(this.domainFile, pFile);

		if (unground == null)
		{
			System.err.println("Parsing error - see console for details");
			return null;
		}

		GroundProblem ground = ground(unground);

		return this.doPlan(ground);

	}

	/**
	 * @param unground
	 * @return
	 * @throws IllegalArgumentException
	 */
	private GroundProblem ground(UngroundProblem unground)
			throws IllegalArgumentException {
		logger.info("Preprocessing negative preconditions...");

		preprocessNegatativePC(unground);

		logger.info("Done...");

		logger.info("Grounding...");

		GroundProblem ground = unground.ground();

		logger.info("Done...");


		logger.info("Grounding complete");
		logger.info("Decompiling ADL... skipped!");
		//		int previousActionCount = ground.actions.size();
		//ground.decompileADL();
		//		int adlActionCount = ground.actions.size();
		//		logger.info("Decompiling ADL complete");
		//		logger.info(previousActionCount+" actions before ADL, "+adlActionCount+" after");
		//		for (Action a:ground.actions) {
		//			if (a.toString().contains("eat-person"))
		//				for (Fact f: a.getPreconditions())
		//					System.out.println(f);
		//		}


		//	doFilterReachableFacts= true;
		if (doFilterReachableFacts) { 
			logger.info("Performing reachability analysis...");

			filterReachableFacts(ground);
			logger.info("Reachability analysis complete");
		}
		return ground;
	}



	/**
	 * Method to prerpocess negative preconditions so that they are taken into account when using relaxed plan; 
	 * the issue is that otherwise negative preconditions are never reached in the relaxed planning, as relaxed planning takes into account only positive effects.
	 * This method is based on the one described in "Combining the Expressivity of UCPOP with the Effciency of Graphplan - B. Cenk Gazen and Craig A. Knoblock" page 9
	 * 
	 * @param unground the problem 
	 * @throws IllegalArgumentException
	 */
	private void preprocessNegatativePC(UngroundProblem unground)
			throws IllegalArgumentException {

		// for all the unground actions 
		// first we look in the PC for negations
		// and record them in the nots hashmap

		unground.buildTypeSets();

		Map<String, Predicate> nots = new MapWrapper<String, Predicate>();
		logger.info("Tracking and replacing negative PCs");
		for (Operator o: unground.actions)
		{
			if (o instanceof UngroundInstantAction) {
				UngroundInstantAction a = (UngroundInstantAction) o;
				// we look the condition
				Set<? extends Fact> ff =a.condition.getFacts();
				Set<Fact> toAdd = new SetWrapper<Fact>();
				Set<Fact> toDel = new SetWrapper<Fact>();
				//				Set<ForAll> nested = new SetWrapper<ForAll>();
				//				Set<Fact> nestedAdd = new SetWrapper<Fact>();
				//				Set<Fact> nestedDel = new SetWrapper<Fact>();
				// and we get all the facts of a condition 
				for (Fact f:ff) {
					//					boolean isnested = false;
					//					// we check if it is a negative PC (not)
					//					if (f instanceof ForAll) {
					//					//	System.out.println(f);
					//						nested.add((ForAll)f);
					//						isnested = true;
					//						Fact literal = ((ForAll) f).getCondition();
					//						for (Fact p : literal.getFacts()){
					//							if (p instanceof Not) {
					//								f = p;
					//							}
					//						}
					//					}
					if (f instanceof Not){
						Fact literal = ((Not) f).literal;
						for (Fact p : literal.getFacts()){
							if (p instanceof Predicate) {
								Predicate pp = (Predicate) p;
								//								if (isnested)
								//									nestedDel.add(f);
								//								else
								toDel.add(f);
								List<Parameter> pps= unground.predSymbolMap.get(pp.getPredicateSymbol().getName()).getParameters();
								List<Parameter> pps2= pp.getParameters();
								PredicateSymbol ps = new PredicateSymbol("not_" + pp.getPredicateSymbol().getName());
								ps.getParameters().addAll(pps);
								Predicate np = new Predicate(ps);
								np.setParameters(pps);
								nots.put(pp.getPredicateSymbol().getName(),np);
								np = (Predicate)np.clone();
								np.setParameters(pps2);
								//								if (isnested)
								//									nestedAdd.add(np);
								//								else
								toAdd.add(np);
								// aggiungere e rimpiazzare gli effetti delle azioni come da paper p and not p
							} else if (p instanceof Equals) {
								//								Equals eq = (Equals) p;
								//								NotEquals ne = new NotEquals(eq.parameters);
								//								toDel.add(f);
								//								toAdd.add(ne);
								//								nots.put("!=",ne);
								// No need to handle equality as it can only be in PCs , not in the effects!
							} 
						}
					}

				}
				if (a.condition instanceof And) {
					if (toAdd.size()!=0 || toDel.size()!=0) {
						logger.fine(a.name.toString());
						logger.fine("PC before " + a.condition);
						And and = (And) a.condition;
						and.literals.removeAll(toDel);
						and.addAll(toAdd);
						logger.fine("PC after  " + a.condition);
					}
					//					if (nested.size()>0) {
					//						for (ForAll n:nested) {
					//							if (((Imply)n.getCondition()).getA() instanceof Not)
					//								((Imply)n.getCondition()).setA(nestedAdd.iterator().next());
					//							if (((Imply)n.getCondition()).getB() instanceof Not)
					//								((Imply)n.getCondition()).setB(nestedAdd.iterator().next());
					//							//System.out.println(a.condition);
					//						}
					//					}

				} else {
					logger.severe("Unsupported");
					//TODO: support this if it ever happens (I think PC need to start with AND)
				}
			} else throw (new IllegalArgumentException("Durative actions unsupported"));
			// TODO: implement durative actions

		}

		for (Predicate p: nots.values()) {
			unground.predSymbols.add(p.getPredicateSymbol());
			unground.predSymbolMap.put(p.getPredicateSymbol().getName(), p.getPredicateSymbol());
		}
		logger.fine("Tracking and replacing effects involving NPC");
		for (Predicate s: nots.values())
			logger.fine("NPC: " + s);
		// then NOW we look in the effects of each action and apply changes 
		for (Operator o: unground.actions)
		{
			// For each action
			Set<Fact> toAdd = new SetWrapper<Fact>();
			if (o instanceof UngroundInstantAction) {
				UngroundInstantAction a = (UngroundInstantAction) o;
				// for each effect
				Set<? extends Fact> ff =a.effect.getFacts();
				for (Fact f:ff) {

					// If we are in a negative effect
					if (f instanceof Not) {
						Not nn = (Not) f;
						for (Fact p : nn.literal.getFacts()){
							if (p instanceof Predicate) {
								Predicate pp = (Predicate) p;
								Predicate npp = nots.get(pp.getPredicateSymbol().getName());
								// if we have the effect of a negative PC
								if (npp!=null) {
									Predicate np = (Predicate)npp.clone();
									np.setParameters(pp.getParameters());
									toAdd.add(np);
								}
							}
						}
					}
					// if it's a positive effect
					else if (f instanceof Predicate) {
						Predicate pp = (Predicate) f;
						Predicate npp = nots.get(pp.getPredicateSymbol().getName());

						if (npp!=null) {
							// add not (not_p) to the effects
							Predicate np = (Predicate)npp.clone();
							np.setParameters(pp.getParameters());
							Not nnp = new Not(np);
							toAdd.add(nnp);
						}
					}
				}

				if (toAdd.size()>0) {
					logger.fine("Action: "+ a);
					logger.fine("EF before" + a.effect);


					if (a.effect instanceof And) {
						And and = (And) a.effect;
						and.addAll(toAdd);
						a.effect = and;
					} else {
						And and = new And(a.effect);
						and.addAll(toAdd);
						a.effect = and;
					}

					logger.fine("EF after " + a.effect);
				}

			} else throw (new IllegalArgumentException("Durative actions unsupported"));

		}


		// Finally it's time to look at the initial conditions, and add all the negative instances
		// for each negative PC proposition instance : 
		for (Entry<String, Predicate> no:nots.entrySet()) {
			// we 'create' the proposition that appers in a negative PC
			Predicate p = new Predicate(unground.predSymbolMap.get(no.getKey()));
			List<Parameter> tt= p.getPredicateSymbol().getParameters();
			p.setParameters(tt);
			// and we get the negative proposition
			Predicate np = no.getValue();
			// and set the parameters (the same as the positive PC)

			// now the tricky part: 
			// 1 generate all possible valid combinations of the parameters; 
			// 2 look up if they are present as positive in the inital conditions
			// 3 if present do nothing; 
			// 3 if not present it means we must add the negation as true to the initial conditions
			// This will make teh IC consistent with the new created Propositions

			// 1 we call the appropriate method to generate all combinations of parameters (objects)  
			Map<String,PDDLObject[]> res = new MapWrapper<String,PDDLObject[]>();
			res.put("", new PDDLObject[p.getParameters().size()]);

			Map<String, PDDLObject[]> combo = null;
			try {
				p.getParameters().get(0);
			} catch (Exception e) {
				throw new IllegalArgumentException(p.toString()+ " Missing");
			}
			combo = getParameterCombinations(0, p.getParameters(), unground, res, p.getPredicateSymbol().getName());
			//			System.out.println("___ "+ p);
			//			for (PDDLObject[] x:combo.values()){
			//				for (PDDLObject o:x) {
			//					System.out.print(" "+ o.getName());
			//				}
			//				System.out.println();
			//			}
			Set<Proposition> initial = unground.initial;
			// 2 we look up and remove the POSITIVE propositions that are already present in the initial conditions; 
			for (Proposition ip: initial) {
				String s1 = ip.toString();
				Object n = combo.get(s1);
				// 3 if a combination is already in the IC, we remove it as we don't need to generate the negative IC;
				if (n!=null) {
					combo.remove(s1);
					//System.out.println("IP removed: "+ s1);
				}
			}
			// 3 Finally combo contains all the combinations of parameters that are not present in IC; 
			// 3 so we just need to create the negative instantiations and add them to the IC
			for (PDDLObject[] par: combo.values()) {
				// new negative proposition
				Proposition npg = new Proposition(np.getPredicateSymbol());
				Map <Variable, PDDLObject> m= new MapWrapper<Variable, PDDLObject>();
				// grounding is just setting the parameters to the objects 
				for (PDDLObject po:par)
				{
					npg.addParameter(po);

				}
				for (int i=0; i< par.length;i++) {
					Parameter pi = np.getParameters().get(i);
					m.put(new Variable(pi.getName(),pi.getType()), par[i]);
				}
				Proposition npg2 = np.ground(m);
				// 3 FINAL: we add the NEGATIVE proposition to the initial conditions
				initial.add(npg2);
				//System.out.println("IP added: "+ npg);
			}

		}

	}



	/**
	 * This method will generate all the possible combinations of parameters for a predicate; will generate the objects in the right order
	 * @param par
	 * @param unground
	 * @return List of objects 
	 */
	public Map<String, PDDLObject[]> getParameterCombinations(int c, List <Parameter> par, UngroundProblem unground, Map<String, PDDLObject[]> res, String prop ) {
		Parameter p = par.get(c);
		Set<PDDLObject> ob = unground.typeSets.get(p.getType());
		Map<String, PDDLObject[]> res2 = new MapWrapper<String, PDDLObject[]>();
		for (PDDLObject[] r:res.values()) {
			for (PDDLObject o:ob) {
				PDDLObject[] v = new PDDLObject[par.size()];
				for (int i=0;i<c;i++)
					v[i] = r[i];
				v[c] = o;

				StringBuilder b = new StringBuilder();
				b.append(prop);
				for (int i=0;i<c+1;i++)
					b.append(' ').append(v[i].toString());
				//System.out.println(b.toString());
				res2.put(b.toString() , v);

			}
		}
		++c;
		if (c == par.size())
			return res2;
		return getParameterCombinations(c, par, unground, res2, prop); 
	}

	/**
	 * This is a rough RPG for detecting unreachable facts and unused actions. Anything which does not appear
	 * before the graph stabilises is elminated from the groud problem and search.
	 * @param gproblem
	 */
	private boolean filterReachableFacts(GroundProblem gproblem)
	{
		boolean eureka = true;
		ArrayList<STRIPSState> layers = new ArrayList<STRIPSState>();
		Set<Action> actionsUsed = new SetWrapper<Action>();

		//		TemporalMetricState current = new TemporalMetricState(new SetWrapper<Action>(), gproblem.initial, new And(), );
		STRIPSState current = gproblem.getSTRIPSInitialState();
		//System.out.println("Start: " + current.facts.size());
		//		for (Proposition p: current.facts)
		//			System.out.println(p.toString().toUpperCase());
		layers.add(current);
		Set<Proposition> goal = new SetWrapper<Proposition>();
		Iterator csit = gproblem.goal.getFacts().iterator();
		while (csit.hasNext()) {
			Proposition p = (Proposition) csit.next();
			goal.add(p);
		}

		do
		{
			STRIPSState next = (STRIPSState) current.clone();
			//System.out.println(next.facts.size());
			for (Action a : gproblem.actions)
			{
				//					System.out.println(current.facts.contains(o))
				//System.out.println(a.getClass());
				if (a.isApplicable(current))
				{
					actionsUsed.add(a);
					for (Fact add : a.getAddPropositions())
					{
						if (add instanceof Proposition) {
							next.facts.add((Proposition) add);
						}
						else System.out.println("!!!@!!" + add);
					}
				}
			}
			layers.add(next);
			current = next;

		}

		while (layers.get(layers.size()-1).equals(layers.get(layers.size()-2)) == false );


		Iterator<Proposition> iterator = goal.iterator();
		while (iterator.hasNext()) {
			Proposition pp = iterator.next();
			if (((STRIPSState) layers.get(layers.size() - 1)).facts.contains(pp)) {
				goal.remove(pp);
			}
		}
		if (!goal.isEmpty()) {
			logger.info("Unreachable goal/s!");
			eureka = false;
			for (Proposition p:goal)
				logger.info("" + p);
		}
		logger.info("reachable facts are |"
				+ ((STRIPSState) layers.get(layers.size() - 1)).facts.size()
				+ "|, original is " + gproblem.groundedPropositions.size());
		logger.info("reachable actions are |" + actionsUsed.size()
				+ "|, original is " + gproblem.actions.size());

		if (doFilterReachableFacts ) {
			gproblem.actions = actionsUsed;
			gproblem.reachableFacts = layers.get(layers.size() - 1).facts;

		} else 
			logger.info("Original problem not modified");
		return eureka;
	}


	protected Plan doPlan(GroundProblem ground)
	{

		if (this.isGoalValid(ground, ground.goal) == false)
		{
			throw new IllegalArgumentException("Goal is at-least partially unreachable");
		}


		if (ground.sometimes.size()>0) {
			return doPlanIG(ground);
		}
		long startTime = System.currentTimeMillis();
		long afterBFSPlanning = 0, afterEHCPlanning = 0;	

		//		this.ground = gproblem;

		logger.info("Action set has "+ground.actions.size()+" in it");

		logger.info("Final Action set is "+ground.actions.size());
		//		TemporalMetricState currentInitState = (TemporalMetricState) ground.state.clone();

		//construct init
		Set na = new SetWrapper();
		Set ni = new SetWrapper();
		Iterator ait = ground.actions.iterator();
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

		logger.info("About to create init state");
		STRIPSState currentInitState = ground.getSTRIPSInitialState();
		STRIPSState originalInitState = (STRIPSState) currentInitState.clone(); //get default init tmstate then modify

		State goalState = null;
		//GroundFact goalFinal = ground.goal;

		//TotalOrderPlan incPlan = new TotalOrderPlan();
		TotalOrderPlan bestPlan = null;
		TotalOrderPlan ehcPlan = null;
		TotalOrderPlan bfsPlan = null;

		logger.info("Running FF with EHC...");
		goalState = performFFSearch(currentInitState, true, false);

		afterEHCPlanning = System.currentTimeMillis();
		if (goalState != null)
		{
			logger.info("Found EHC plan: ");
			bestPlan = (TotalOrderPlan)goalState.getSolution();
			afterEHCPlanning = System.currentTimeMillis();
			double planningEHCTime = (afterEHCPlanning - startTime) / 1000.00;
			logger.info("EHC Planning Time =\t" + planningEHCTime + "sec"); //have to write here or scripts will not pick up that an EHC plan was computed
			logger.info("Plan length is "+bestPlan.getActions().size());	
			ehcPlan = bestPlan;
		}
		else
		{
			//			System.exit(1);
			currentInitState = (STRIPSState) originalInitState.clone();
			logger.info("Running FF with BFS...");
			goalState = performFFSearch(currentInitState, false, true); //no landmarks, just do FF w BFS
			afterBFSPlanning = System.currentTimeMillis();
			if (goalState != null)
			{
				bfsPlan = (TotalOrderPlan) goalState.getSolution();
				if (ehcPlan == null || bfsPlan.getPlanLength() < ehcPlan.getPlanLength())
				{
					bestPlan = bfsPlan;

					logger.info("Found BFS plan: ");

					double planningBFSTime = (afterBFSPlanning - afterEHCPlanning) / 1000.00;
					logger.info("BFS Planning Time =\t" + planningBFSTime + "sec"); //have to write here or scripts will not pick up that an EHC plan was computed
					logger.info("Plan length is "+bestPlan.getActions().size());	
				}
			}

		}

		TimeStampedPlan tsp = null;
		if (bestPlan != null)
		{
			logger.info("Final plan...");
			bestPlan.print(System.out);

			// ***************0*****************
			// Schedule a plan
			// ********************************
			int counter = 0;
			tsp = new TimeStampedPlan();
			for (Object a : bestPlan.getActions())
				tsp.addAction((Action)a, new BigDecimal(counter++));
			//timePlan;

			//JavaFF scheduler does not work
			//			if (goalState != null)
			//			{
			//				logger.info("Scheduling");
			//
			//				Scheduler scheduler = new JavaFFScheduler(ground);
			//				tsp = scheduler.schedule(incPlan);
			//			}

			if (tsp != null)
			{
				//				tsp.print(System.out);
				logger.info("Final plan length is "+tsp.actions.size());				
			}
		}
		else
		{
			logger.info("No plan found");
		}

		long afterScheduling = System.currentTimeMillis();
		double planningEHCTime = (afterEHCPlanning - startTime) / 1000.00;
		double planningBFSTime = (afterBFSPlanning - afterEHCPlanning) / 1000.00;
		double schedulingTime = (afterScheduling - afterBFSPlanning) / 1000.00;

		logger.info("EHC Plan Time =\t" + planningEHCTime + "sec");
		logger.info("BFS Plan Time =\t" + planningBFSTime + "sec");
		logger.info("Scheduling Time =\t" + schedulingTime + "sec");

		return bestPlan;
	}

	
	
	
	private Plan doPlanIG(GroundProblem ground) {


		TotalOrderPlan bestPlan = null;
		for (GroundFact f: ground.sometimes) {
			if (!this.isGoalValid(ground, f))
				throw new IllegalArgumentException("Goal is at-least partially unreachable");
		}

		logger.info("Action set has "+ground.actions.size()+" in it");

		logger.info("Final Action set is "+ground.actions.size());

		GroundFact finalGoal = ground.goal;
		ground.sometimes.add(finalGoal);
		STRIPSState currentInitState = null;
		STRIPSState goalState = null;
		for (GroundFact f: ground.sometimes) {
				ground.goal = f;
			if (goalState!=null) {
				ground.initial = goalState.facts;
				goalState.goal = f;
				currentInitState = goalState;
			} else
				currentInitState = new STRIPSState(ground.actions, ground.initial, ground.goal);
			currentInitState.setRPG(new RelaxedPlanningGraph(ground));
			ground.state = currentInitState;
			currentInitState.always = ground.always;
			currentInitState.sometimes = ground.sometimes;
			
			STRIPSState originalInitState = (STRIPSState) currentInitState.clone();

			logger.info("Running FF with EHC...");
			goalState = (STRIPSState) performFFSearch(currentInitState, true, false);
			if (goalState != null)
			{
				logger.info("Found EHC plan: ");
			}
			else
			{
				currentInitState = (STRIPSState) originalInitState.clone();
				logger.info("Running FF with BFS...");
				goalState = (STRIPSState) performFFSearch(currentInitState, false, true); //no landmarks, just do FF w BFS
			}
			if (goalState != null)
			{
				bestPlan = (TotalOrderPlan) goalState.getSolution();
				logger.info("Plan length is "+bestPlan.getActions().size());
			} else
				bestPlan = null;
			if (bestPlan != null)
			{
				logger.info("Plan...");
				bestPlan.print(System.out);
			}
			else
			{
				logger.info("No plan found");
				return null;
			}

			
		}
		return bestPlan;

	}

	protected Plan doMetricPlan(GroundProblem ground)
	{
		long startTime = System.currentTimeMillis();
		long afterBFSPlanning = 0, afterEHCPlanning = 0;	

		//		this.ground = gproblem;

		System.out.println("Action set has "+ground.actions.size()+" in it");

		System.out.println("Final Action set is "+ground.actions.size());
		//		TemporalMetricState currentInitState = (TemporalMetricState) ground.state.clone();

		//construct init
		Set na = new SetWrapper();
		Set ni = new SetWrapper();
		Iterator ait = ground.actions.iterator();
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

		Metric metric;
		if (ground.metric == null)
			metric = new Metric(Metric.MINIMIZE, new NumberFunction(Metric.MINIMIZE));
		else
			metric = ground.metric;

		System.out.println("About to create init tmstate");
		//		MetricState ms = new MetricState(ni, ground.mstate.facts, ground.goal,
		//				ground.functionValues, metric);
		//		System.out.println("About to create gp");
		//		GroundProblem gp = new GroundProblem(na, ground.mstate.facts, ground.goal,
		//				ground.functionValues, metric);
		//		gp.getMetricInitialState();
		//		System.out.println("Creating RPG");
		//		ms.setRPG(new RelaxedMetricPlanningGraph(gp));
		MetricState currentInitState = ground.getMetricInitialState();

		MetricState originalInitState = (MetricState) currentInitState.clone(); //get default init tmstate then modify

		State goalState = null;
		GroundFact goalFinal = ground.goal;

		TotalOrderPlan incPlan = new TotalOrderPlan();
		TotalOrderPlan bestPlan = null;
		TotalOrderPlan ehcPlan = null;
		TotalOrderPlan bfsPlan = null;

		System.out.println("Running FF with EHC...");
		goalState = performFFSearch(currentInitState, true, false);

		afterEHCPlanning = System.currentTimeMillis();
		if (goalState != null)
		{
			System.out.println("Found EHC plan: ");
			bestPlan = (TotalOrderPlan)goalState.getSolution();
			afterEHCPlanning = System.currentTimeMillis();
			double planningEHCTime = (afterEHCPlanning - startTime) / 1000.00;
			logger.info("EHC Planning Time =\t" + planningEHCTime + "sec"); //have to write here or scripts will not pick up that an EHC plan was computed
			System.out.println("Plan length is "+bestPlan.getActions().size());	
			ehcPlan = bestPlan;
		}
		else
		{
			//			System.exit(1);
			currentInitState = (MetricState)originalInitState.clone();
			System.out.println("Running FF with BFS...");
			goalState = performFFSearch(currentInitState, false, true); //no landmarks, just do FF w BFS
			afterBFSPlanning = System.currentTimeMillis();
			if (goalState != null)
			{
				bfsPlan = (TotalOrderPlan)goalState.getSolution();
				if (ehcPlan == null || bfsPlan.getPlanLength() < ehcPlan.getPlanLength())
				{
					bestPlan = bfsPlan;

					System.out.println("Found BFS plan: ");

					double planningBFSTime = (afterBFSPlanning - afterEHCPlanning) / 1000.00;
					logger.info("BFS Planning Time =\t" + planningBFSTime + "sec"); //have to write here or scripts will not pick up that an EHC plan was computed
					System.out.println("Plan length is "+bestPlan.getActions().size());	
				}
			}

		}

		TimeStampedPlan tsp = null;
		if (bestPlan != null)
		{
			System.out.println("Final plan...");
			bestPlan.print(System.out);

			// ***************0*****************
			// Schedule a plan
			// ********************************
			int counter = 0;
			tsp = new TimeStampedPlan();
			for (Object a : bestPlan.getActions())
				tsp.addAction((Action)a, new BigDecimal(counter++));
			//timePlan;

			//JavaFF scheduler does not work
			//			if (goalState != null)
			//			{
			//				logger.info("Scheduling");
			//
			//				Scheduler scheduler = new JavaFFScheduler(ground);
			//				tsp = scheduler.schedule(incPlan);
			//			}

			if (tsp != null)
			{
				tsp.print(System.out);
				System.out.println("Final plan length is "+tsp.actions.size());				
			}
		}
		else
		{
			System.out.println("No plan found");
		}

		long afterScheduling = System.currentTimeMillis();
		double planningEHCTime = (afterEHCPlanning - startTime) / 1000.00;
		double planningBFSTime = (afterBFSPlanning - afterEHCPlanning) / 1000.00;
		double schedulingTime = (afterScheduling - afterBFSPlanning) / 1000.00;

		logger.info("EHC Plan Time =\t" + planningEHCTime + "sec");
		logger.info("BFS Plan Time =\t" + planningBFSTime + "sec");
		logger.info("Scheduling Time =\t" + schedulingTime + "sec");

		return bestPlan;
	}

	protected Plan doTemporalPlan(GroundProblem ground)
	{
		long startTime = System.currentTimeMillis();
		long afterBFSPlanning = 0, afterEHCPlanning = 0;	

		//		this.ground = gproblem;

		System.out.println("Action set has "+ground.actions.size()+" in it");

		System.out.println("Final Action set is "+ground.actions.size());
		//		TemporalMetricState currentInitState = (TemporalMetricState) ground.state.clone();

		//construct init
		Set na = new SetWrapper();
		Set ni = new SetWrapper();
		Iterator ait = ground.actions.iterator();
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

		Metric metric;
		if (ground.metric == null)
			metric = new Metric(Metric.MINIMIZE, new NumberFunction(Metric.MINIMIZE));
		else
			metric = ground.metric;

		System.out.println("About to create init tmstate");
		//		TemporalMetricState ts = new TemporalMetricState(ni, ground.tmstate.facts, ground.goal,
		//				ground.functionValues, metric);
		//		System.out.println("About to create gp");
		//		GroundProblem gp = new GroundProblem(na, ground.tmstate.facts, ground.goal,
		//				ground.functionValues, metric);
		//		gp.getTemporalMetricInitialState();
		//		System.out.println("Creating RPG");
		//		ts.setRPG(new RelaxedTemporalMetricPlanningGraph(gp));
		TemporalMetricState currentInitState = ground.getTemporalMetricInitialState();

		TemporalMetricState originalInitState = (TemporalMetricState) currentInitState.clone(); //get default init tmstate then modify

		State goalState = null;
		GroundFact goalFinal = ground.goal;

		TotalOrderPlan incPlan = new TotalOrderPlan();
		TotalOrderPlan bestPlan = null;
		TotalOrderPlan ehcPlan = null;
		TotalOrderPlan bfsPlan = null;

		System.out.println("Running FF with EHC...");
		goalState = performFFSearch(currentInitState, true, false);

		afterEHCPlanning = System.currentTimeMillis();
		if (goalState != null)
		{
			System.out.println("Found EHC plan: ");
			bestPlan = (TotalOrderPlan)goalState.getSolution();
			afterEHCPlanning = System.currentTimeMillis();
			double planningEHCTime = (afterEHCPlanning - startTime) / 1000.00;
			logger.info("EHC Planning Time =\t" + planningEHCTime + "sec"); //have to write here or scripts will not pick up that an EHC plan was computed
			System.out.println("Plan length is "+bestPlan.getActions().size());	
			ehcPlan = bestPlan;
		}
		else
		{
			//			System.exit(1);
			currentInitState = (TemporalMetricState)originalInitState.clone();
			System.out.println("Running FF with BFS...");
			goalState = performFFSearch(currentInitState, false, true); //no landmarks, just do FF w BFS
			afterBFSPlanning = System.currentTimeMillis();
			if (goalState != null)
			{
				bfsPlan = (TotalOrderPlan)goalState.getSolution();
				if (ehcPlan == null || bfsPlan.getPlanLength() < ehcPlan.getPlanLength())
				{
					bestPlan = bfsPlan;

					System.out.println("Found BFS plan: ");

					double planningBFSTime = (afterBFSPlanning - afterEHCPlanning) / 1000.00;
					logger.info("BFS Planning Time =\t" + planningBFSTime + "sec"); //have to write here or scripts will not pick up that an EHC plan was computed
					System.out.println("Plan length is "+bestPlan.getActions().size());	
				}
			}

		}

		TimeStampedPlan tsp = null;
		if (bestPlan != null)
		{
			System.out.println("Final plan...");
			bestPlan.print(System.out);

			// ***************0*****************
			// Schedule a plan
			// ********************************
			int counter = 0;
			tsp = new TimeStampedPlan();
			for (Object a : bestPlan.getActions())
				tsp.addAction((Action)a, new BigDecimal(counter++));
			//timePlan;

			//JavaFF scheduler does not work
			//			if (goalState != null)
			//			{
			//				logger.info("Scheduling");
			//
			//				Scheduler scheduler = new JavaFFScheduler(ground);
			//				tsp = scheduler.schedule(incPlan);
			//			}

			if (tsp != null)
			{
				tsp.print(System.out);
				System.out.println("Final plan length is "+tsp.actions.size());				
			}
		}
		else
		{
			System.out.println("No plan found");
		}

		long afterScheduling = System.currentTimeMillis();
		double planningEHCTime = (afterEHCPlanning - startTime) / 1000.00;
		double planningBFSTime = (afterBFSPlanning - afterEHCPlanning) / 1000.00;
		double schedulingTime = (afterScheduling - afterBFSPlanning) / 1000.00;

		logger.info("EHC Plan Time =\t" + planningEHCTime + "sec");
		logger.info("BFS Plan Time =\t" + planningBFSTime + "sec");
		logger.info("Scheduling Time =\t" + schedulingTime + "sec");

		return bestPlan;
	}

	public void stop() {
		current.stop();

	}

	Search current;


	protected State performFFSearch(STRIPSState initialState, boolean useEHC, boolean useBFS)
	{
		if (!useEHC && !useBFS)
			return null;

		State goalState = null;

		logger.info("INIT "+initialState.facts + "\nGOAL "+initialState.goal);
		if (useEHC)
		{
			logger.info("Performing search using EHC with standard helpful action filter");

			EnforcedHillClimbingSearch EHCS = new EnforcedHillClimbingSearch(
					initialState);
			current =EHCS;
			EHCS.setFilter(HelpfulFilter.getInstance()); // and use the helpful
			//															// actions neighbourhood

			// Try and find a plan using EHC
			goalState = EHCS.search();

			if (goalState != null)
				return goalState;
			else
				logger.info("Failed to find solution using EHC");
		}
		if (useBFS)
		{
			logger.info("Performing search using BFS");
			// create a Best-First Searcher
			BestFirstSearch BFS = new BestFirstSearch(initialState);
			current =BFS;
			BFS.setFilter(NullFilter.getInstance());
			goalState = BFS.search();

			if (goalState == null)
				logger.info("Failed to find solution using BFS");
		}
		return goalState;
	}



	/**
	 * If -macro switch is used, a unified domain must be produced for VAL to parse.
	 */
	private void writeValDomain(String filename)
	{
		//remove last ) from domain definition
		RandomAccessFile raf;
		try
		{
			File problemDomainFile = new File(filename);
			FileWriter fWriter = new FileWriter(problemDomainFile);
			BufferedWriter bufWriter = new BufferedWriter(fWriter);

			FileReader fReader = new FileReader(domainFile);
			BufferedReader bufReader = new BufferedReader(fReader);

			//clone domain file but leave off final ) -- assumes that the domain has no 
			String currentLine, nextLine = bufReader.readLine();
			while (nextLine != null)
			{
				currentLine = nextLine;
				nextLine = bufReader.readLine();
				if (nextLine != null)
					bufWriter.write(currentLine+"\n");
				else
				{
					bufWriter.flush();

					bufWriter.write(")"); //close domain
					break;
				}
			}
			bufReader.close();
			fReader.close();

			bufWriter.close();
			fWriter.close();
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}


	protected void writePlanToFile(Plan plan, File fileOut)
	{
		try
		{
			//			logger.info("plan is "+plan+", file is "+fileOut);
			fileOut.delete();
			fileOut.createNewFile();

			FileOutputStream outputStream = new FileOutputStream(fileOut);
			PrintWriter printWriter = new PrintWriter(outputStream);
			plan.print(printWriter);
			printWriter.close();
		} catch (FileNotFoundException e)
		{
			logger.severe(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.severe(e.toString());
			e.printStackTrace();
		}

	}


	private TotalOrderPlan doLandmarkSearch(TemporalMetricState currentInitState, GroundFact goalFinal,
			ArrayList<ArrayList<GroundFact>> lmPaths, boolean useEHC, boolean useBFS)
	{
		int lmPathCounter = 0;
		State goalState = null;
		TotalOrderPlan incPlan = new TotalOrderPlan();
		TemporalMetricState originalInit = (TemporalMetricState)currentInitState.clone();

		do
		{
			ArrayList<GroundFact> path = lmPaths.get(lmPathCounter);
			for (GroundFact p : path)
			{
				And and = new And();
				and.add(p);

				//do simple check to find out if final leaf/goal is already true- if so, skip plan
				if (currentInitState.facts.contains(path.get(path.size()-1)))
					continue;

				currentInitState.goal = and; //set current landmark goal
				currentInitState.plan.clear();
				//				logger.info("INIT "+currentInitState.facts + "\nGOAL "+currentInitState.goal);
				goalState = performFFSearch(currentInitState, useEHC, useBFS); //search to goal 
				if (goalState == null)
				{
					logger.info("Found no plan");
					break;
				}
				//add current plan to total plan
				//System.out.println("Found subplan");
				TotalOrderPlan brokenPlan = ((TotalOrderPlan)goalState.getSolution());
				ArrayList act = (ArrayList)brokenPlan.getActions();

				for (Object a : act)
				{
					//					System.out.println("Actions applied in subplan are: "+a);
					currentInitState = (TemporalMetricState)currentInitState.apply((Action)a);
					//					timePlan.addAction((Action)a, new BigDecimal(actionCounter++));
					incPlan.addAction((Action)a);
				}
			}

			lmPathCounter++;
		}
		while (goalState != null && 
				//			   goalState.goal.equals(goalFinal) == false &&
				lmPathCounter < lmPaths.size());

		//need to check all final goals are still true, if not, plan from where we are using normal FF
		currentInitState.goal = originalInit.goal;
		currentInitState.plan.clear();
		if (currentInitState.goalReached() == false)
		{
			//currentInitState.goal = goalFinal; //originalInit.goal;
			State finalState = performFFSearch(currentInitState, useEHC, useBFS);
			if (finalState == null)
				return null;

			TotalOrderPlan endPlan = (TotalOrderPlan)finalState.getSolution();
			for (Object a : endPlan.getActions()) //append to original
			{
				incPlan.addAction((Action)a);
			}
		}

		return incPlan;
	}

	public File getDomainFile()
	{
		return domainFile;
	}

	public void setDomainFile(File domainFile)
	{
		this.domainFile = domainFile;
	}
}
