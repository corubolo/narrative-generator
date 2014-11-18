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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class TimeStampedPlan implements Plan
{
	public SortedSet<TimeStampedAction> actions = new TreeSet<TimeStampedAction>(new Comparator<TimeStampedAction>()
			{
				public int compare(TimeStampedAction a, TimeStampedAction b)
				{
					if (a.getTime().compareTo(b.getTime()) < 0)
						return -1;
					else if (a.getTime().compareTo(b.getTime()) > 0)
						return 1;
					else
					{
						int hashA = a.hashCode();
						int hashB = b.hashCode();
						if (hashA < hashB)
							return -1;
						else if (hashA > hashB)
							return 1;
						else
						{
							//this is the tricky one. What if an unscheduled plan has the same action
							//appear twice, at different times, but the scheduled tries to add them at the
							//same timestep? In this case the latter action would be ignored/overwritten
							//because they are absolutely identical. This is why we use
							//tiny epsilonAccuracy values on each timestamp. So that action A will never equal
							//action B's time. Eg, t(A) = 1.0001, t(B) = 1.0002.
							return 0;
						}
					}
				}
			});
	
//	public SortedSet<TimeStampedAction> actions;
	
	public TimeStampedPlan()
	{
		this.actions = new TreeSet<TimeStampedAction>();
	}
	
	public int length()
	{
		return this.actions.size();
	}

	public void addAction(TimeStampedAction a)
	{
		this.actions.add(a);
	}	
	
	public TimeStampedAction addAction(Action a, BigDecimal t)
	{
		return this.addAction(a, t, null);
	}

	public TimeStampedAction addAction(Action a, BigDecimal t, BigDecimal d)
	{
		TimeStampedAction tsa = new TimeStampedAction(a, t, d);
		actions.add(tsa);
		
		return tsa;
	}
	
	@Override
	public String toString()
	{
		StringBuffer buf = new StringBuffer();
		for (TimeStampedAction a : actions)
		{
			buf.append(a.toString()+"\n");
		}
		buf.deleteCharAt(buf.length()-1);
		
		return buf.toString();
	}

	public void print(PrintStream p)
	{
		Iterator<TimeStampedAction> ait = actions.iterator();
		while (ait.hasNext())
		{
			TimeStampedAction a = (TimeStampedAction) ait.next();
			p.println(a);
		}
	}

	public void print(PrintWriter p)
	{
		Iterator<TimeStampedAction> ait = actions.iterator();
		while (ait.hasNext())
		{
			TimeStampedAction a = (TimeStampedAction) ait.next();
			p.println(a);
		}
	}

	public List<Action> getActions()
	{
		ArrayList<Action> s = new ArrayList<Action>();
		for (Action a : this.actions)
		{
			s.add(a);
		}
		return s;
	}
}
