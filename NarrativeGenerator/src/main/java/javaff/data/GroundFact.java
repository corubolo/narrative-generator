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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import javaff.data.metric.NamedFunction;
import javaff.planning.State;

public interface GroundFact extends Cloneable, Fact, Serializable
{
	//old GroundCondition
	public boolean isTrue(State s); // returns whether this conditions is true
									// is State S
	
	public Set<Fact> getFacts();

	public Set<NamedFunction> getComparators();

	public GroundFact staticify(Map fValues);
	
	public Object clone();

	public void apply(State s); // carry out the effects of this on State s

	public void applyAdds(State s);

	public void applyDels(State s);
	
	public Set getOperators();

//	public GroundFact staticifyEffect(Map fValues);
}
