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

package javaff.data.strips;

import java.util.Map;
import java.util.Set;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundFact;
import javaff.data.metric.NamedFunction;
import javaff.planning.State;
import uk.ac.liverpool.narrative.SetWrapper;

public abstract class InstantAction extends Action
{
	//private because we want to go thorugh setupAddDeletes() every time the effect is modified
	 private GroundFact condition;
	 private GroundFact effect;
	
	transient public Set<Fact> adds;
	transient public Set<Not> deletes; //local lookup of adds and deletes

	public InstantAction()
	{
		super();
		this.condition = TrueCondition.getInstance();
		this.effect = TrueCondition.getInstance();
		this.adds = new SetWrapper<Fact>();
		this.deletes = new SetWrapper<Not>();
	}
	
	public InstantAction(String name)
	{
		this.name = new OperatorName(name);
		this.condition = TrueCondition.getInstance();
		this.effect = TrueCondition.getInstance();
		this.adds = new SetWrapper<Fact>();
		this.deletes = new SetWrapper<Not>();
	}

	@Override
//	public boolean equals(Object obj)
//	{
//		boolean supeq = super.equals(obj);
//		
//		InstantAction other = (InstantAction) obj;
//		if (this.condition.equals(other.condition) == false || this.effect.equals(other.effect) == false)
//			return false;
//		
//		return true;
//	}
	
	public boolean isApplicable(State s)
	{
	
		return getCondition().isTrue(s) && s.checkAvailability(this);
	}

	public void apply(State s)
	{
		getEffect().applyDels(s);
		getEffect().applyAdds(s);
		
		this.setupAddDeletes();
	}

	protected void setupAddDeletes()
	{
		this.adds = new SetWrapper<Fact>();
		this.deletes = new SetWrapper<Not>();
		
		for (Fact f : getEffect().getFacts())
		{
			if (f instanceof Not)
			{
				this.deletes.add((Not) f);
			}
			else
			{
				this.adds.add(f);
			}
		}
	}

	public Set<Fact> getPreconditions()
	{
		return getCondition().getFacts();
	}

	@Override
	public Set<Fact> getAddPropositions()
	{
//		return effect.getFacts();
		return this.adds;
	}

	@Override
	public Set<Not> getDeletePropositions()
	{
//		return effect.getFacts();
		return this.deletes;
	}

	public Set<NamedFunction> getComparators()
	{
		return getCondition().getComparators();
	}

	public Set getOperators()
	{
		Set addset = getEffect().getOperators();
		addset.addAll(getEffect().getOperators());
		return addset;
	}

	public void staticify(Map fValues)
	{
		setCondition(getCondition().staticify(fValues));
		setEffect(getEffect().staticify(fValues));
	}

	public void setCondition(GroundFact condition)
	{
		this.condition = condition;
	}

	public GroundFact getCondition()
	{
		return condition;
	}

	public void setEffect(GroundFact effect)
	{
		this.effect = effect;
		this.setupAddDeletes(); //for fast lookup
	}

	public GroundFact getEffect()
	{
		return effect;
	}

}
