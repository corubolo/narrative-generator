
package javaff.scheduling;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.GroundProblem;
import javaff.data.Literal;
import javaff.data.MutexSpace;
import javaff.data.TimeStampedAction;
import javaff.data.TimeStampedPlan;
import javaff.data.TotalOrderPlan;
import javaff.data.strips.And;
import javaff.data.strips.Not;
import javaff.data.strips.Proposition;
import javaff.data.strips.STRIPSInstantAction;
import javaff.data.strips.SingleLiteral;
import javaff.planning.STRIPSState;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

/**
 * A basic scheduler for STRIPS only plans. Actions are assigned timestamps based upon their earliest possible application in the plan.
 * @author dpattiso
 *
 */
public class STRIPSScheduler implements Scheduler
{
	protected GroundProblem problem;
	protected MutexSpace factMutexes;
	protected Map<Action, Set<Action>> actionMutexes;
	//FIXME storing action mutexes is a very brute force approach
	
	private String epsilonAccuracy;
	
	public STRIPSScheduler(GroundProblem problem)
	{
		this.problem = problem;
		this.factMutexes = new MutexSpace();
		this.actionMutexes = new MapWrapper<Action, Set<Action>>();
		
		this.epsilonAccuracy = this.getEpsilonString(3);
	}	
	
	protected String getEpsilonString(int leadingZeros)
	{
		StringBuffer buf = new StringBuffer("0.");
		for (int i = 0; i < leadingZeros; i++)
		{
			buf.append('0');
		}
		buf.append('1');
		
		return buf.toString();
	}
	
	/**
	 * Detects mutexes between actions and then schedules each action in the linear plan to allow
	 * for maximum concurrency with the earliest start times.
	 */
	@Override
	public TimeStampedPlan schedule(TotalOrderPlan top) throws SchedulingException
	{		
		this.detectMutexes(top.getActions());
		System.out.println("Finished mutex detection");
		
		System.out.println("Plan to schedule is ");
//		top.print(System.out);
		
		//this map contains the last achiever of a fact, which will potentially be a PC to another
		Map<Fact, TimeStampedAction> achieverMap = new MapWrapper<Fact, TimeStampedAction>();
		Map<Fact, Set<TimeStampedAction>> requiredBy = new MapWrapper<Fact, Set<TimeStampedAction>>();
		
		Map<BigDecimal, Set<TimeStampedAction>> actionTimes = new MapWrapper<BigDecimal, Set<TimeStampedAction>>();
		
		//setup initial action, which adds initial state
		STRIPSInstantAction initialAction = new STRIPSInstantAction("initialise_action");
		And effect = new And(new SetWrapper<Fact>(this.problem.initial));
		initialAction.setEffect(effect);
		
		TimeStampedAction initialActionScheduled = new TimeStampedAction(initialAction, new BigDecimal(-1), BigDecimal.ONE);
		for (Fact f : initialActionScheduled.getAction().getAddPropositions())
		{
			achieverMap.put(f, initialActionScheduled);
		}
		for (Fact pc : initialActionScheduled.getAction().getPreconditions())
		{
			requiredBy.put(pc, new SetWrapper<TimeStampedAction>());
			requiredBy.get(pc).add(initialActionScheduled);
		}
		
		BigDecimal epsilonOffset = new BigDecimal(this.epsilonAccuracy); //bizarre requirement for BigDecimal creation
		epsilonOffset.setScale((this.epsilonAccuracy+"").length());
		TimeStampedPlan tsp = new TimeStampedPlan();
		BigDecimal bigEpsilon = new BigDecimal(this.epsilonAccuracy);
		
		//now actually schedule the plan
		for (Action a : top.getActions())
		{
			//first, find out when this action becomes applicable. this will be equal to the time at which
			//its precondition with the largest value for T becomes true, which is equal to the time
			//that the precondition becomes true, plus 1 (as this is STRIPS only)
			BigDecimal maxPcTime = new BigDecimal(0);
			
			//track the ancestors of this action
//			TreeSet<TimeStampedAction> ancestors = new TreeSet<TimeStampedAction>();
			
			for (Fact pc : a.getPreconditions())
			{
				if (pc.isStatic() || (pc instanceof Proposition == false && pc instanceof Not == false))
					continue;
				
				if (pc instanceof Not && (((Not)pc).literal instanceof Proposition == false))
					continue;
				
				//this action is required to appear before the current action
				TimeStampedAction ancestor = null;
				//check for STRIPS PCs
				if (pc instanceof Not)
				{
					Not npc = (Not)pc;
					//if the negated literal is not present, we are happy
					if (achieverMap.containsKey(npc.literal) == false)
					{
						continue;
					}
				}
//				else
//					continue; //skip Equals tests and other non-STRIPS conditions
				
				ancestor = achieverMap.get(pc);
				if (ancestor == null)
					throw new SchedulingException("No way to achieve precondition "+pc+" in "+a);
				
				if (ancestor.getMajorTime().add(BigDecimal.ONE).compareTo(maxPcTime) > 0)
				{
					maxPcTime = ancestor.getMajorTime().add(BigDecimal.ONE);
				}
				
//				ancestors.add (ancestor);
			}
			
			//now have max time. Action now needs to be checked to see if any of it's effects are mutex with another action
			//which is scheduled for the same time.
			//make sure to floor() the asspciated timestamp to get the key, and in turn the correct
			//integer start time
			BigDecimal currentIntegerTime = maxPcTime.setScale(0, RoundingMode.FLOOR);//.add(BigDecimal.ONE);
			
			TimeStampedAction tsa = new TimeStampedAction(a, currentIntegerTime, BigDecimal.ONE);
			
			//check each action at time t, until there are no mutexes
			boolean nonMutex = false;
			out: while (nonMutex == false)
			{
				nonMutex = false;
				
				//I really hate Java generics
				Set<TimeStampedAction> existingTSA = actionTimes.get(currentIntegerTime);
				if (existingTSA == null)
					break out;
				
				Set<Action> existingAtT = new SetWrapper<Action>(existingTSA);
				if (this.areActionsMutex(tsa, existingAtT) != MutexType.None)
				{
					nonMutex = false;
					currentIntegerTime = currentIntegerTime.add(BigDecimal.ONE);
					continue out;
				}
				
				//the above code may decide that an action is applicable at time T, but ignore the fact there may
				//already be an identical action at this time. That is, the plan has the same action more than once. If action A 
				//has been scheduled for time T, then action B will also fit these requirements. This has to be detected, or
				//the action which is meant to appear later in the plan will not be correcty scheduled and the plan becomes invalid
				for (Action ex : existingAtT)
				{
					if (tsa.getAction().equals(((TimeStampedAction)ex).getAction()))
					{
						//if we have found a scheduled action which is identical to the unscheduled action, then find at what time the 
						//scheduled action is required, and make (that+1) the new currentTime to start looking for a suitable schedule point
						BigDecimal latestRequirement = currentIntegerTime.abs();
						for (Fact pc : ex.getPreconditions())
						{
							Set<TimeStampedAction> requiresPC = requiredBy.get(pc);
							if (requiresPC == null)
								continue;
							
							for (TimeStampedAction reqA : requiresPC)
							{
								if (reqA.getMajorTime().compareTo(latestRequirement) > 0)
								{
									latestRequirement = reqA.getMajorTime().abs();
								}
							}
						}
						//now have latest time the action is required, so start at this + 1
						nonMutex = false;
						currentIntegerTime = latestRequirement.setScale(0, RoundingMode.FLOOR).add(BigDecimal.ONE);
						continue out;
					}
				}
				
				
				//if made it this far, then actions are not mutex in a lifted context.
				//But, this action's effects may threaten already-scheduled actions at future times
				for (Not del : tsa.getDeletePropositions())
				{
					Fact f = del.literal;
					Set<TimeStampedAction> requires = requiredBy.get(f);

					//if the action is deleted then re-added at the same time
					if (requires == null || (tsa.deletes(f) && tsa.adds(f)))
						continue;
					
					for (TimeStampedAction req : requires)
					{
						BigDecimal requiredByTime = req.getMajorTime();
						if (requiredByTime.compareTo(tsa.getMajorTime()) >= 0)
						{
							//illegal -- this action will block the achievement of a later action
							nonMutex = false;
							currentIntegerTime = currentIntegerTime.add(BigDecimal.ONE);
							continue out;
						}
					}
				}
			
				nonMutex = true;
			}

			//THIS IS A MUST! without it there is the risk that two identical actions can be scheduled at
			//the same timestep. This would mean the later action being ignored or overwritten. That is,
			//the action would vanish, resulting in an invalid plan
			BigDecimal epsilonTime = currentIntegerTime.add(epsilonOffset); 
			
			//set final time
			tsa.setTime(epsilonTime);
			
			tsp.addAction(tsa); //add the scheduled action to the plan
			
			if (actionTimes.containsKey(currentIntegerTime) == false)
			{
				actionTimes.put(currentIntegerTime, new SetWrapper<TimeStampedAction>());
			}
					
			//if we;ve gotten to this point, the action is schedulable at the computed time
//			System.out.println("Scheduled "+tsa);
			actionTimes.get(currentIntegerTime).add(tsa); //add new schedule time
			
			//record that this action requires its preconditions be true by time T
			for (Fact f : tsa.getPreconditions())
			{
				if (f.isStatic())
					continue;
				
				if (requiredBy.containsKey(f) == false)
				{
					requiredBy.put(f, new SetWrapper<TimeStampedAction>());
				}
				
				boolean needsCleared = false;
				for (TimeStampedAction req : requiredBy.get(f))
				{
					if (req.getMajorTime().compareTo(tsa.getMajorTime()) < 0)
					{
						needsCleared = true;
					}
				}
				if (needsCleared)
					requiredBy.get(f).clear();
				
				requiredBy.get(f).add(tsa);
			}
					
			//now need to setup what the scheduled fact achieved or deleted
			for (Not del : tsa.getDeletePropositions())
			{
				achieverMap.remove(del.literal);
				//only add the negation achiever if the delete effect is STRIPS syntax
				if (del.literal instanceof Proposition == true)
					achieverMap.put(del, tsa);
			}
			
			for (Fact add : tsa.getAddPropositions())
			{
				achieverMap.put(add, tsa);
			}
			
			
			
			//update the epsilonAccuracy offset before considering the next action
			epsilonOffset = epsilonOffset.add(bigEpsilon);
		}
		
		return tsp;
	}
	
	


	protected MutexType areActionsMutex(Action a, Set<Action> others)
	{
		for (Action b : others)
		{
			MutexType mut = this.areActionsMutex(a, b);
			if (mut != MutexType.None)
				return mut;
		}
		
		return MutexType.None;
	}
	
	protected MutexType areActionsMutex(Action a, Action b)
	{
		MutexType mutex = MutexType.None;
		
		//first check that parameters dont contain conflicting causal graph leads
		if (a == b)
			return MutexType.None;
				
		mutex = this.checkPauseMutex(a, b);
		if (mutex != MutexType.None)
			return mutex;
			
		mutex = this.checkDeleteMutex(a, b);
		if (mutex != MutexType.None)
			return mutex;
		
		mutex = this.checkPreconditionMutex(a, b);
		if (mutex != MutexType.None)
			return mutex;
		
		
//		for (Proposition p : a.getAddPropositions())
//		{
//			for (Proposition q : b.getAddPropositions())
//			{
//				mutex = this.checkCompetingEffectMutex(p, q);
//				if (mutex != MutexType.None)
//				{
//					return mutex; 
//				}
//			}
//		}
		
		return MutexType.None;
	}

	
	protected boolean areStatesMutex(STRIPSState a, STRIPSState b)
	{
		for (Object afo : a.facts)
		{
			Proposition af = (Proposition) afo;
			for (Object bfo : b.facts)
			{
				Proposition bf = (Proposition) bfo;
				
				if (this.factMutexes.isMutex(af, bf))
					return true;
			}
		}
			
		return false;
	}


	protected MutexType checkCompetingPreconditionMutex(Fact p, Fact q, Collection<Action> actionSet)
	{
		//find all achievers of fact p
		Set<Action> achieversA = new SetWrapper<Action>();
		for (Action a : actionSet)
		{
			//ignore pause mutexes
			if (a.adds(p) && a.deletes(p) == false)
				achieversA.add(a);
		}
		
		//find all achievers of fact q
		Set<Action> achieversB = new SetWrapper<Action>();
		for (Action b : actionSet)
		{
			if (b.adds(q) && b.deletes(q) == false)
				achieversB.add(b);
		}
		
		//determine if ALL achievers of p and q are mutex with one-another
		if (achieversA.isEmpty() || achieversB.isEmpty())
			return MutexType.None;
		
		for (Action a : achieversA)
		{
			for (Action b : achieversB)
			{
				if (a == b)
					continue;
				
				Set<Action> aMut = this.actionMutexes.get(a);
				Set<Action> bMut = this.actionMutexes.get(b);
				if (aMut.contains(b) == false && bMut.contains(a) == false)
					return MutexType.None;
			}
		}
		
		return MutexType.CompetingPCs;
	}

	protected MutexType checkPreconditionMutex(Action a, Action b)
	{
		//check to see if A deletes any of Bs preconditions and vice versa
		//ignore anything which deletes and adds in the same action effect, because it is 
		//not really blocking the other action
		for (Not apc : a.getDeletePropositions())
		{
			if (a.adds(apc.literal))
				continue; //ignore if deleted and added in same action
			
			if (b.requires(apc.literal) == true)
			{
				return MutexType.AdeleteBpc;
			}
		}
		//TODO be smarter about which should get delayed first- it should be the one which doesnt delete a fact first
		for (Not bpc : b.getDeletePropositions())
		{
			if (b.adds(bpc.literal))
				continue; //ignore if deleted and added in same action
			
			if (a.getPreconditions().contains(bpc.literal))
			{
				return MutexType.BdeleteApc;
			}
		}
		
		return MutexType.None;
	}

	protected MutexType checkDeleteMutex(Action a, Action b)
	{
		//check if A's add effects deletes any of B's add effects and vice versa
		//ignore anything which deletes and adds in the same action effect, because it is 
		//not really blocking the other action
		for (Not apc : a.getDeletePropositions())
		{
			if (b.adds(apc.literal) == true &&
				b.deletes(apc.literal) == false)
			{
				return MutexType.AdeleteBadd;
			}
		}

		for (Not bpc : b.getDeletePropositions())
		{
			if (a.adds(bpc.literal) == true &&
				a.deletes(bpc.literal) == false)
			{
				return MutexType.BdeleteAadd;
			}
		}
		
		return MutexType.None;
	}

	protected MutexType checkPauseMutex(Action a, Action b)
	{

		//check to see if A both adds and deletes the same fact in it's effects. If this fact is 
		//in B's preconditions or effects, A must block B
		Set<SingleLiteral> aPauseSet = new SetWrapper<SingleLiteral>();
		for (Not d : a.getDeletePropositions())
		{
			aPauseSet.add((SingleLiteral) d.literal);
		}
		
		aPauseSet.retainAll(a.getAddPropositions());
		if (aPauseSet.size() > 0) //if anything exists after a logical AND operation
		{
			for (SingleLiteral blockFact : aPauseSet)
			{
				if (b.getAddPropositions().contains(blockFact))
				{ 
					for (Not n : b.getDeletePropositions())
					{
						if (n.literal.equals(blockFact))
							return MutexType.ApauseB;
					}
				}
			}
		}
		
		Set<SingleLiteral> bPauseSet = new SetWrapper<SingleLiteral>();
		for (Not d : a.getDeletePropositions())
			bPauseSet.add((SingleLiteral) d.literal);
		
		bPauseSet.retainAll(b.getAddPropositions());
		if (bPauseSet.size() > 0) //if anything exists after a logical AND operation
		{
			for (SingleLiteral blockFact : bPauseSet)
			{
				if (a.getAddPropositions().contains(blockFact))
				{
					for (Not n : a.getDeletePropositions())
					{
						if (n.literal.equals(blockFact))
							return MutexType.BpauseA;
					}
				}
			}
		}
		
		return MutexType.None;
	}

	protected void detectMutexes(List<Action> actions)
	{
		this.actionMutexes.clear();
		this.factMutexes.clear();
		System.out.println("Detecting mutexes amongst "+actions.size()+" actions");
		
		this.detectStaticFacts(actions);
		
		//detect all effect and PC delete mutexes
		for (Action a : actions)
		{
			Set<Action> mutex = new SetWrapper<Action>();
			
			//TODO optimise- only check tail of list
			for (Action b : actions)
			{			
				if (a == b)
					continue;
				
				if (this.areActionsMutex(a, b) != MutexType.None)
					mutex.add(b);
			}
			this.actionMutexes.put(a, mutex);
		}
		
		//now use the above mutexes to detect any PC fact mutexes
		Set<Fact> allPCs = new SetWrapper<Fact>();
		for (Action a : actions)
		{
			for (Fact pc : a.getPreconditions())
				allPCs.add(pc);
		}
		
		for (Fact p : allPCs)
		{
			if (p.isStatic())
			{
//				System.out.println(p+" is static");
				continue;
			}
			
			Set<Fact> mutexGroup = new SetWrapper<Fact>();
			for (Fact q : allPCs)
			{
				if (q.isStatic() || p == q)
					continue;
				
				MutexType mutex = this.checkCompetingPreconditionMutex(p, q, actions);
				if (mutex != MutexType.None)
				{
					mutexGroup.add(q);
				}
			}
			this.factMutexes.addMutex(p, mutexGroup); //TODO optimise for q, p
		}
		
		//finally, add the fact mutex info into the action mutexes
		for (Action a : actions)
		{
			//TODO optimise- only check tail of list
			for (Action b : actions)
			{		
				if (a == b || (this.actionMutexes.containsKey(a) && this.actionMutexes.get(a).contains(b)))
					continue;
				
				for (Fact p : a.getPreconditions())
				{
					if (p.isStatic())
						continue;
						
					for (Fact q : b.getPreconditions())
					{
						if (p == q || q.isStatic())
							continue;
						
						if (this.factMutexes.isMutex((GroundFact)p, (GroundFact)q))
						{
							if (this.actionMutexes.containsKey(a))
							{
								this.actionMutexes.get(a).add(b);
							}
							else
							{
								Set<Action> mut = new SetWrapper<Action>();
								mut.add(b);
								this.actionMutexes.put(a, mut);
							}
						}	
					}
				}
			}
		}
	}

	protected void detectStaticFacts(Collection<Action> act)
	{
		Set<Proposition> allPCs = new SetWrapper<Proposition>();
		for (Action a : act)
		{
			for (Fact pc : a.getPreconditions())
				if (pc instanceof Proposition)
					allPCs.add((Proposition) pc);
		}
		
		for (Proposition p : allPCs)
		{
			boolean found = false;
			for (Action a : act)
			{
				for (Fact add : a.getAddPropositions())
				{
					if (((Literal)add).getPredicateSymbol().equals(p.getPredicateSymbol()))
					{
						found = true;
						break;
					}
				}
			}
			
			if (!found)
				p.getPredicateSymbol().setStatic(true);
			else
				p.getPredicateSymbol().setStatic(false);
		}
	}

	public String getEpsilon()
	{
		return epsilonAccuracy;
	}

	public void setEpsilon(int epsilon)
	{
		this.epsilonAccuracy = this.getEpsilonString(epsilon);
	}
}

