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

package uk.ac.liverpool.narrative;

import java.io.Serializable;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;

import javaff.data.Action;
import javaff.planning.STRIPSState;
import javaff.planning.State;

public class Solution implements Serializable {

	public Solution(STRIPSState currentInitState, State s, String name,
			String cURRENT_ARGS, long se, String dom, String pro) {
		start = currentInitState;
		end = s;
		domainName = name;
		generatingConditions = cURRENT_ARGS;
		actions = s.plan.getActions();
		seed = se;
		domain = dom;
		problem = pro;

	}

	public Solution(STRIPSState currentInitState, State s) {
		start = currentInitState;
		end = s;
		actions = s.plan.getActions();
	}

	public Solution(Solution s) {
		// TODO Auto-generated constructor stub
	}

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public List<Action> actions = new LinkedList<Action>();

	public STRIPSState start;

	public State end;

	String domainName;

	String generatingConditions;

	String domain, problem;

	long seed;

	@Override
	public int hashCode() {
		return start.hashCode() + end.hashCode() + actions.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Solution) {
			Solution s2 = (Solution) obj;
			return s2.start.equals(start) && s2.end.equals(end)
					&& s2.actions.equals(actions);
		} else
			return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(Integer.toHexString(hashCode()) + ", ");
		for (Action a : actions) {
			sb.append(a.toString() + " , ");
		}
		return sb.toString();
	}

	public String ids() {
		return "" + Integer.toHexString(hashCode());
	}

	public StringSolution convert() {
		StringSolution s = new StringSolution();
		for (Action a : actions) {
			s.actions.add(a.toString());
			s.domain = domain;
			s.domainName = domainName;
			s.generatingConditions = generatingConditions;
			s.problem = problem;
			s.seed = seed;
		}
		return s;
	}

	public StringSolution convert(String template) {
		StringSolution s = new StringSolution();
		Template t;
		try {
			t = Template.readTemplate(new StringReader(template));
			for (Action a : actions) {
				s.actions.add(t.apply_template(a));
				s.domain = domain;
				s.domainName = domainName;
				s.generatingConditions = generatingConditions;
				s.problem = problem;
				s.seed = seed;
			}
			return s;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			return convert();
		}

	}
}
