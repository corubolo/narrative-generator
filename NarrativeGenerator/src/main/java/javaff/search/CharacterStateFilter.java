/***********************************************************************
 *
 * This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com - and Meriem Bendis
 * The University of Liverpool
 *
 *
 * BranchingStoryGenerator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * BranchingStoryGenerator is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 *
 ************************************************************************/
package javaff.search;

import java.util.List;

import javaff.data.Action;
import javaff.planning.State;

public class CharacterStateFilter extends StateFilter{
	
	private String[] character;

	public CharacterStateFilter(String character) {
		this.character = character.split(",");
	}

	@Override
	public boolean isStateOK(State s) {
		
		if (s == null || s.getPlan() == null)
			return false;
		List<Action> actions = s.getPlan().getActions();
		if (actions.size() == 0)
			return false;
		Action lastAction = actions.get(actions.size()-1);
		
		// here we make the stron assumptions that the acting character is always the first parameter in the action. 
		// this must be taken as a convention when creating the plans!
		String firstParam =lastAction.parameters.get(0).toString();
		
		for (String c: character) {
			if (firstParam.equals(c))
				return true;
		}
		return false;
	}
	

}
