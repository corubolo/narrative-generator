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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.data.Action;
import javaff.data.strips.OperatorName;
import javaff.planning.STRIPSState;
import javaff.search.BranchingSearch;

import org.apache.commons.collections15.multimap.MultiHashMap;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.util.EdgeType;

public class JGTSolutionGraph extends SolutionGraph implements Serializable {

	private static final long serialVersionUID = -1355921271022514656L;
	public DirectedGraph<STRIPSState, Action> jtggraph;

	public JGTSolutionGraph(List<Solution> s) {

		this.solutions = s;
		try {
			populateGraph();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public JGTSolutionGraph() {
		initGraph();
	}

	public static void main(String[] args) throws Exception {
		JGTSolutionGraph sg = new JGTSolutionGraph();
		for (File f : BranchingStoryGenerator.DestinationFolder.listFiles()) {
			if (!f.isFile() || !f.getName().endsWith(".sol")) {
				continue;
			}
			sg.readSolutionFile(f);
		}
		sg.populateGraph();
		sg.generateJungGraph();

		// DirectedGraph<STRIPSState,Action > jtggraph =
		// ((JGTSolutionGraph)sgg.sg).jtggraph;

		// BronKerboschCliqueFinder<STRIPSState,Action > a= new
		// BronKerboschCliqueFinder<STRIPSState, Action>(jtggraph);
		// sgg.cliques = a.getBiggestMaximalCliques();
		//
		// StrongConnectivityInspector<STRIPSState, Action> ci = new
		// StrongConnectivityInspector<STRIPSState, Action>(jtggraph);
		// sgg.cliques = ci.stronglyConnectedSets();//connectedSets();
		// int n = 0;
		// for (Set<STRIPSState> ss : sgg.cliques) {
		// n++;
		// System.out.println(n);
		// for (STRIPSState s:ss) {
		// Color c = Color.getHSBColor(((n + 7) * 1337 % 256) / 256.0f,((n + 7)
		// * 1237 % 256) / 256.0f, 0.7f);
		// sgg.cm.put(s, c);
		// }
		// }
		// CycleDetector<STRIPSState, Action> ci = new
		// CycleDetector<STRIPSState, Action>(jtggraph);
		// Set<STRIPSState> ss= ci.findCycles();//connectedSets();
		// int n = 0;
		//
		// for (STRIPSState s:ss) {
		// Color c = Color.getHSBColor(((n + 7) * 1337 % 256) / 256.0f,((n + 7)
		// * 1237 % 256) / 256.0f, 0.7f);
		// sgg.cm.put(s, c);
		// System.out.println(s);
		// }
		//
		// ((JGTSolutionGraph)sgg.sg).generateJungGraph();

		sg.runStory(Template
				.readTemplate(new File(
						"/Users/fabio/Documents/workspace2/Domains/stories/LRRH/red.tpl")));

	}

	public void generateJungGraph() {
		ggraph = new DirectedSparseGraph<STRIPSState, Action>();
		for (Action a : jtggraph.edgeSet()) {
			ggraph.addEdge(a, jtggraph.getEdgeSource(a),
					jtggraph.getEdgeTarget(a), EdgeType.DIRECTED);
		}

	}

	@Override
	public void initGraph() {
		jtggraph = new SimpleDirectedGraph<STRIPSState, Action>(Action.class);// <STRIPSState,
																				// Action>();
		ends = new SetWrapper<STRIPSState>();
		realActions = new MapWrapper<Action, Action>();
		actionToEdges = new MultiHashMap<String, Action>();
		edgeColors = new MapWrapper<Action, JGTSolutionGraph.EdgeColor>();
		frequency = new MapWrapper<STRIPSState, Integer>();
	}

	/**
	 * @param sol
	 * @param start
	 * @throws Exception
	 */
	@Override
	public void populateGraph() {
		int c = 0;
		validateStart();
		initGraph();
		for (Solution ac : solutions) {
			c++;
			STRIPSState from = (STRIPSState) ac.start.clone();
			for (int n = 0; n < ac.actions.size(); n++) {
				Action a1 = ac.actions.get(n);
				STRIPSState to = (STRIPSState) from.apply(a1);
				Action a2 = (Action) a1.clone();
				a2.name = new OperatorName(a2.name.toString() + "@" + c + "-"
						+ n);
				jtggraph.addVertex(from);
				jtggraph.addVertex(to);
				try {
					jtggraph.addEdge(from, to, a2);
				} catch (Exception x) {
					x.printStackTrace();
				}
				realActions.put(a2, a1);
				if (template != null) {
					actionToEdges.put(template.apply_template(a1), a2);
				}
				from = to;
			}
			ends.add(from);
			if (!ac.end.equals(from))
				throw new IllegalArgumentException("Invalid end state!");

		}
		DFSVisit(start, new SetWrapper(), edgeColors, frequency);

	}

	@Override
	public int DFSVisit(STRIPSState s, Set explored, Map<Action, EdgeColor> m,
			Map<STRIPSState, Integer> frequency) {
		explored.add(s);
		int k = 1;

		Collection<Action> aa = jtggraph.outgoingEdgesOf(s);
		for (Action a : aa) {
			STRIPSState e = jtggraph.getEdgeTarget(a);
			if (!explored.contains(e)) {
				k += DFSVisit(e, explored, m, frequency);
				m.put(a, EdgeColor.discovery);
			} else {
				m.put(a, EdgeColor.black);
			}
		}
		frequency.put(s, k);
		return k;
	}

	/**
	 * @param sol
	 * @param start
	 * @throws Exception
	 */
	@Override
	public void addSolution(Solution ac) throws Exception {

		if (!BranchingSearch.checkIfNewSol(ac, usm, delta, solutions)) {
			System.out.println("Duplicate solution");
			return;
		}

		if (start == null) {
			start = ac.start;
		}
		if (!ac.start.equals(start))
			throw new IllegalArgumentException(
					"Solutions with different start state!");

		generalCount++;
		STRIPSState from = (STRIPSState) ac.start.clone();
		for (int n = 0; n < ac.actions.size(); n++) {
			Action a1 = ac.actions.get(n);
			STRIPSState to = (STRIPSState) from.apply(a1);
			Action a2 = (Action) a1.clone();
			a2.name = new OperatorName(a1.name.toString() + "@" + generalCount
					+ "-" + n);
			jtggraph.addVertex(from);
			jtggraph.addVertex(to);
			try {
				jtggraph.addEdge(from, to, a2);
			} catch (Exception x) {
				x.printStackTrace();
			}
			realActions.put(a2, a1);
			if (template != null) {
				actionToEdges.put(template.apply_template(a1), a2);
			}
			from = to;
		}
		ends.add(from);
		if (!ac.end.equals(from))
			throw new Exception("Invalid end state!");
		solutions.add(ac);
	}

	protected void runStory(Template template) throws NumberFormatException,
			IOException {

		STRIPSState current = start;
		Collection<Action> aa = jtggraph.outgoingEdgesOf(current);
		if (aa == null) {
			System.out.println("No action from the start?");
			return;
		}
		do {

			if (aa.size() == 1) {
				Action a = aa.iterator().next();
				System.out.println(actionToString(template, a));
				if (a != null) {
					current = jtggraph.getEdgeTarget(a);

				}
			} else if (aa.size() > 1) {
				int n = 0;
				Map<Integer, Action> hm = new MapWrapper<Integer, Action>();
				System.out.println("You can now chose between: ");
				Iterator<Action> it = aa.iterator();
				while (it.hasNext()) {
					n++;
					Action a = it.next();
					System.out.println(n + ") " + actionToString(template, a));
					hm.put(n, a);
				}
				System.out.println("What is your choice? ");
				BufferedReader is = new BufferedReader(new InputStreamReader(
						System.in));
				while (true) {
					String a = is.readLine();
					try {

						Action a2 = hm.get(Integer.parseInt(a.trim()));
						if (a2 != null) {
							current = jtggraph.getEdgeTarget(a2);
							break;
						}
					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			} else {
				break;
			}

			aa = jtggraph.outgoingEdgesOf(current);
		} while (aa.size() > 0);

		System.out.println("The End");
	}

}
