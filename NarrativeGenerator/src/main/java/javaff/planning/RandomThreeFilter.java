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

import java.util.Iterator;
import java.util.Set;

import javaff.JavaFF_mod;
import uk.ac.liverpool.narrative.SetWrapper;

public class RandomThreeFilter implements Filter
{
	private static RandomThreeFilter rf = null;
	
	protected HelpfulFilter hf;

	public RandomThreeFilter()
	{
		hf = HelpfulFilter.getInstance();
	}

	
	public Set getActions(State S)
	{
		Set helpfulFiltered = hf.getActions(S);
		Set subset = new SetWrapper();
		
		if (helpfulFiltered.size() <= 3)
			return helpfulFiltered;
		
		int[] indices = new int[3];
		for (int i = 0; i < 3; i++) //init
			indices[i] = -1;

		//System.out.println("about to select randoms from possible list of "+helpfulFiltered.size());
		for (int i = 0; i < 3; i++)
		{
			int rand;
			boolean done;
			do
			{
				done = true;
				rand = JavaFF_mod.generator.nextInt(helpfulFiltered.size());
				//System.out.println("generated "+rand);
				
				
				for (int j = 0; j < 3; j++)
				{
					if (rand == indices[j])
					{
						done = false;
						break;
					}
				}
			}
			while (!done);
			indices[i] = rand;
		}
		//System.out.println("indices as "+indices[0]+", "+indices[1]+", "+indices[2]);
		
		int iterCounter = 0, arrCounter = 0;
		Iterator iter = helpfulFiltered.iterator();
		while (iter.hasNext() && arrCounter < 3)
		{
			if (indices[arrCounter] == iterCounter)
			{
				subset.add(iter.next());
				arrCounter++;
			}
			else
			{
				iterCounter++;
				iter.next(); //skip
			}
		}
		
		return subset;
	}

}