
package javaff.data.adl;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.Literal;
import javaff.data.PDDLPrinter;
import javaff.data.UngroundFact;
import javaff.data.metric.NamedFunction;
import javaff.data.strips.And;
import javaff.data.strips.Not;
import javaff.data.strips.PDDLObject;
import javaff.data.strips.PredicateSymbol;
import javaff.data.strips.STRIPSFact;
import javaff.data.strips.Variable;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public class Imply extends Literal implements GroundFact, UngroundFact, ADLFact
{
	private Fact a, b;
	
	
	public Imply(Fact a, Fact b)
	{
		this.a = a;
		this.b = b;
	}
	
	public Object clone()
	{
		return new Imply((Fact)this.a.clone(), (Fact)this.b.clone());
	}
	
	@Override
	public Set<Fact> getFacts()
	{
		Set<Fact> s = new SetWrapper<Fact>();
		s.add(a);
		s.add(b);
		return s;
	}
	
	public Fact getA()
	{
		return a;
	}


	public void setA(Fact a)
	{
		this.a = a;
	}


	public Fact getB()
	{
		return b;
	}


	public void setB(Fact b)
	{
		this.b = b;
	}


	@Override
	public Set<NamedFunction> getComparators()
	{
		return ((GroundFact)this.a).getComparators();
	}


//	/**
//	 * Gets the conditional propositions of the Imply condition, i.e. condition A (Note, B is not included!)
//	 */
//	@Override
//	public Set<Fact> getConditionalPropositions()
//	{
//		Set<Fact> s = new SetWrapper<Fact>();
//		s.add(this);
//		return s;
//		//return ((GroundCondition)this.a).getConditionalPropositions();
//	}

	/**
	 * Returns true iff 
	 * 
	 * | A | B | A -> B |
	 * | 0 | 0 | 1 |
	 * | 0 | 1 | 1 |
	 * | 1 | 0 | 0 |
	 * | 1 | 1 | 1 |
	 */
	@Override
	public boolean isTrue(State s)
	{
		boolean aTrue = ((GroundFact)this.a).isTrue(s);
		boolean bTrue = ((GroundFact)this.b).isTrue(s);
		
		//a -> b
		if (aTrue == false) //(~a -> b) || (~a -> ~b)
			return true;
		else if (aTrue && bTrue) //a -> b
			return true;
		else 
			return false; //a -> ~b
	}


	@Override
	public GroundFact staticify(Map fValues)
	{
//		return this;// 
		GroundFact newA = ((GroundFact)this.a).staticify(fValues);
		GroundFact newB = ((GroundFact)this.b).staticify(fValues);
		
//		if (newB instanceof TrueCondition)
//			return TrueCondition.getInstance();
		
		return new Imply(newA, newB);
	}


	/**
	 * Is A static (doesn't check B)
	 */
	@Override
	public boolean isStatic()
	{
//		return ((GroundCondition)this.a).isStatic();
		return false;
	}


	@Override
	public void PDDLPrint(PrintStream p, int indent)
	{
		p.print("(imply (");
		PDDLPrinter.printToString(this.a, p, false, true, indent);
		p.print(") (");
		PDDLPrinter.printToString(this.b, p, false, true, indent);
		p.print(")");
	}


	@Override
	public String toStringTyped()
	{
		return "imply ("+this.a.toStringTyped()+") ("+this.b.toStringTyped()+")";
	}
	
	@Override
	public String toString()
	{
		return "imply ("+this.a.toString()+") ("+this.b.toString()+")";
	}


	@Override
	public void apply(State s)
	{
		if (this.isTrue(s))
		{
			((GroundFact)this.b).apply(s);
		}
	}


	@Override
	public void applyAdds(State s)
	{
		this.apply(s);
	}


	@Override
	public void applyDels(State s)
	{
		this.apply(s);
	}

	@Override
	public Set getOperators()
	{
		return ((GroundFact)this.b).getOperators();
	}


	@Override
	public Set getStaticPredicates()
	{
		return new SetWrapper();
		//return ((GroundFact)this.b).get
	}


	@Override
	public GroundFact ground(Map<Variable, PDDLObject> varMap)
	{
		GroundFact ga = ((UngroundFact)this.a).ground(varMap);
		GroundFact gb = ((UngroundFact)this.b).ground(varMap);
		
		return new Imply(ga, gb);
	}


	@Override
	public UngroundFact minus(UngroundFact effect)
	{
		return ((UngroundFact)this.b).minus(effect);
	}


	@Override
	public boolean effects(PredicateSymbol ps)
	{
		return ((UngroundFact)this.b).effects(ps);
	}


	@Override
	public UngroundFact effectsAdd(UngroundFact cond)
	{
		return ((UngroundFact)this.b).effectsAdd(cond);
	}
	
	/**
	 * Returns a set of ANDs which correspond to the three states of A and B in which this Implys would be true,
	 * e.g. (and (not(a)) (not(b))); (and (not(a)) (b)); (and (a) (b)). However, less than 3 conjunctions
	 * may be returned if either A or B are static. This is because a negated static fact will always be
	 * true, but illegal in terms of applicability in any state.
	 * @return
	 */
	public Collection<? extends STRIPSFact> toSTRIPS()
	{
		Set<STRIPSFact> ands = new SetWrapper<STRIPSFact>();

		if (this.a.isStatic() == false)
		{
			if (this.b.isStatic() == false)
			{
				And one = new And();
				one.add(new Not((Fact) this.a.clone()));
				one.add(new Not((Fact) this.b.clone()));
				ands.add(one);
			}
			
			And two = new And();
			two.add(new Not((Fact) this.a.clone()));
			two.add((Fact) this.b.clone());
			ands.add(two);
		}
		
		And three = new And();
		three.add((Fact) this.a.clone());
		three.add((Fact) this.b.clone());
		
		ands.add(three);
		
		return ands;
	}
}
