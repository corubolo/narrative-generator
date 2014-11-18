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

package javaff.data.metric;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javaff.data.Fact;
import javaff.data.strips.Constant;
import javaff.data.strips.PDDLObject;
import javaff.data.strips.Variable;
import javaff.planning.MetricState;
import javaff.scheduling.MatrixSTN;
import uk.ac.liverpool.narrative.SetWrapper;

public class NamedFunction extends javaff.data.Literal implements Function
{
	protected NamedFunction()
	{

	}

	public NamedFunction(FunctionSymbol fs)
	{
		super.setPredicateSymbol(fs);
	}
	
	@Override
	public Set<? extends Fact> getFacts()
	{
		Set<Fact> s = new SetWrapper<Fact>();
		s.add(this);
		return s;
	}

	public BigDecimal getValue(MetricState ms)
	{
		return ms.getValue(this);
	}

	public BigDecimal getMaxValue(MatrixSTN stn)
	{
		return getValue(null);
	}

	public BigDecimal getMinValue(MatrixSTN stn)
	{
		return getValue(null);
	}

	public boolean effectedBy(ResourceOperator ro)
	{
		return this.equals(ro.resource);
	}

	public Function replace(ResourceOperator ro)
	{
		if (ro.resource.equals(this))
		{
			if (ro.type == MetricSymbolStore.INCREASE)
				return new BinaryFunction(MetricSymbolStore.PLUS, this,
						ro.change);
			else if (ro.type == MetricSymbolStore.DECREASE)
				return new BinaryFunction(MetricSymbolStore.MINUS, this,
						ro.change);
			else if (ro.type == MetricSymbolStore.SCALE_UP)
				return new BinaryFunction(MetricSymbolStore.MULTIPLY, this,
						ro.change);
			else if (ro.type == MetricSymbolStore.SCALE_DOWN)
				return new BinaryFunction(MetricSymbolStore.DIVIDE, this,
						ro.change);
			else if (ro.type == MetricSymbolStore.ASSIGN)
				return ro.change;
			else
				return this;
		} else
			return this;
	}

	public Function staticify(Map fValues)
	{
		if (isStatic())
		{
			BigDecimal d = (BigDecimal) fValues.get(this);
			return new NumberFunction(d);
		} else
			return this;
	}

	public Function makeOnlyDurationDependent(MetricState s)
	{
		return new NumberFunction(getValue(s));
	}

	public Function ground(Map varMap)
	{
		NamedFunction nf = new NamedFunction((FunctionSymbol) name);
		Iterator pit = parameters.iterator();
		while (pit.hasNext())
		{
			Object next = pit.next();
			if (next instanceof Constant)
			{
				Constant c = (Constant) next;
				
				nf.addParameter(c);
					
//				Constant c = (Constant) next;
//				Constant po = (Constant) varMap.get(c);
//				nf.addParameter(po);
			}
			else
			{
				Variable v = (Variable) next;
				PDDLObject po = (PDDLObject) varMap.get(v);
				nf.addParameter(po);
			}
		}
		return nf;
	}

	public String toString()
	{
		return "(" + super.toString() + ")";
	}

	public String toStringTyped()
	{
		return "(" + super.toStringTyped() + ")";
	}

	public int hashCode()
	{
		int hash = 7;
		hash = 31 * hash ^ name.hashCode();
		hash = 31 * hash ^ parameters.hashCode();
		return hash;
	}

	@Override
	public Object clone()
	{
		return new NamedFunction(new FunctionSymbol(super.getPredicateSymbol().getName()));
	}
}
