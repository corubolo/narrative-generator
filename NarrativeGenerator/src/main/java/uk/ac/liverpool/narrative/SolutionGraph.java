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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javaff.data.Action;
import javaff.data.strips.OperatorName;
import javaff.planning.STRIPSState;
import javaff.search.BranchingSearch;
import javaff.search.BranchingUtils;
import javaff.search.CharacterFilter;
import javaff.search.SolutionFilter;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;

/**
 * @author fabio
 *
 */
public class SolutionGraph implements Serializable {
	/**
	 *
	 */
	enum EdgeColor {
		black, discovery
	}

	private static final String copyright = "C o p y r i g h t  ( C ) 2 0 1 2  F a b i o  C o r u b o l o  -  T h e   U n i v e r s i t y  o f L i v e r p o o l ";

	private static final long serialVersionUID = -1355921271022514656L;
	public Graph<STRIPSState, Action> ggraph;
	public Set<STRIPSState> ends;
	public List<Solution> solutions = new LinkedList<Solution>();
	public STRIPSState start;
	Map<Action, Action> realActions;
	MultiMap<String, Action> actionToEdges;
	public BranchingSearch.CompareMethod usm = BranchingSearch.CompareMethod.LCSDelta;
	public double delta = 3;
	public int generalCount = 0;
	public Map<Action, EdgeColor> edgeColors;
	public Map<STRIPSState, Integer> frequency;
	protected Template template;

	public List<SolutionFilter> filters = new LinkedList<SolutionFilter>();

	public boolean doVisit = true;

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

	public SolutionGraph(List<Solution> s) {
		solutions = s;
		try {
			populateGraph();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public SolutionGraph() {
		initGraph();
	}

	public void setSolutions(List<Solution> s) {
		solutions = new LinkedList<Solution>(s);
		// try {
		// populateGraph();
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	public void initGraph() {
		ggraph = new DirectedSparseGraph<STRIPSState, Action>();
		ends = new SetWrapper<STRIPSState>();
		realActions = new MapWrapper<Action, Action>();
		actionToEdges = new MultiHashMap<String, Action>();
		edgeColors = new MapWrapper<Action, SolutionGraph.EdgeColor>();
		frequency = new MapWrapper<STRIPSState, Integer>();

	}

	public void validateStart() {
		if (solutions.isEmpty())
			throw new IllegalArgumentException("There are no solutions");
		for (Solution s : solutions) {
			if (start == null) {
				start = s.start;
			}
			if (!s.start.equals(start))
				throw new IllegalArgumentException(
						"Solutions with multiple different start states!");
		}
	}

	public static void main(String[] args) throws Exception {
		SolutionGraph sg = new SolutionGraph();
		for (File f : BranchingStoryGenerator.DestinationFolder.listFiles()) {
			if (!f.isFile() || !f.getName().endsWith(".sol")) {
				continue;
			}
			sg.readSolutionFile(f);
		}
		sg.validatePlans();

	}

	public void validatePlans() throws IOException, InterruptedException,
			URISyntaxException {
		int n = 0;
		for (Solution s : solutions) {
			n++;
			File tempPlan = File.createTempFile("plan", ".plan");
			FileWriter fw = new FileWriter(tempPlan);
			for (Action a : s.actions) {
				fw.write("(" + a + ")\n");
			}
			fw.close();
			ProcessBuilder pb;
			File domain = new File(s.domain);
			File problem = new File(s.problem);
			File val = new File("validate");//
			pb = new ProcessBuilder(val.getAbsolutePath(),
					domain.getAbsolutePath(), problem.getAbsolutePath(),
					tempPlan.getAbsolutePath());
			pb.redirectErrorStream(true);
			Process p = pb.start();
			InputStream io = p.getInputStream();
			StringWriter sw = new StringWriter();
			int i;
			while ((i = io.read()) != -1) {
				sw.append((char) i);
			}
			p.waitFor();
			if (!sw.toString().contains("Plan valid")) {
				System.out.println(sw.toString());
			} else {
				System.out.println("Plan valid " + n);
			}
		}

	}

	public int DFSVisit(STRIPSState s, Set explored, Map<Action, EdgeColor> m,
			Map<STRIPSState, Integer> frequency) {
		explored.add(s);
		int k = 1;
		Collection<Action> aa = ggraph.getOutEdges(s);
		for (Action a : aa) {
			STRIPSState e = ggraph.getDest(a);
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

	public void readSolutionFile(File f) throws FileNotFoundException,
			IOException, ClassNotFoundException {
		ObjectInputStream dao = new ObjectInputStream(new GZIPInputStream(
				new FileInputStream(f)));
		System.out.println(f);
		List<Solution> so1 = (List<Solution>) dao.readObject();
		dao.close();
		solutions.addAll(so1);
		// System.out.println(so1.get(0).generatingConditions);
	}

	/**
	 * @param sol
	 * @param start
	 * @throws Exception
	 */
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
				try {
					ggraph.addEdge(a2, from, to, EdgeType.DIRECTED);
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
			// System.out.println(from.goalReached());
		}
		if (doVisit) {
			DFSVisit(start, new SetWrapper(), edgeColors, frequency);
		}
		System.out.println("Graph populated with :" + solutions.size()
				+ " solutions and " + ends.size() + " ends");
	}

	public void cleanupLoops(int i) {
		Set<Action> toRemove = new SetWrapper<Action>();
		Set<STRIPSState> trs = new SetWrapper<STRIPSState>();
		DFSVisit2(start, ggraph, toRemove, trs, i, new HashSet<STRIPSState>());
		for (Action s : toRemove) {
			ggraph.removeEdge(s);
		}
		for (STRIPSState s : trs) {
			ggraph.removeVertex(s);
		}

	}

	public void DFSVisit2(STRIPSState s, Graph<STRIPSState, Action> f,
			Set<Action> toRemovef, Set<STRIPSState> toRemovesf, int i,
			Set<STRIPSState> explored) {
		explored.add(s);
		Collection<Action> aa = f.getOutEdges(s);
		if (aa != null) {
			for (Action a : aa) {
				STRIPSState e = f.getDest(a);
				EdgeColor c = edgeColors.get(a);
				if (c != null && c == EdgeColor.black) {
					Set<Action> toRemove = new SetWrapper<Action>();
					Set<STRIPSState> toRemoves = new SetWrapper<STRIPSState>();
					toRemove.add(a);
					while (f.getOutEdges(s).size() == 1) {
						toRemove.add(a);
						toRemoves.add(s);
						Collection<Action> ss = f.getInEdges(s);
						a = ss.iterator().next();
						s = f.getSource(a);
					}
					// finally we remove only branches with less then i deleted
					// actions
					if (toRemove.size() < i) {
						toRemovef.addAll(toRemove);
						toRemovesf.addAll(toRemoves);
					}
				}
				if (!explored.contains(e)) {
					DFSVisit2(e, f, toRemovef, toRemovesf, i, explored);
				}

			}
		}

	}

	/**
	 * @param sol
	 * @param start
	 * @throws Exception
	 */
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
			try {
				ggraph.addEdge(a2, from, to, EdgeType.DIRECTED);
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
		// System.out.println(from.goalReached());
	}

	// public void printSolutions() {
	// for (Solution s : solutions) {
	// System.out.println(s);
	// }
	//
	// }

	public String printSolutions() throws IOException {
		String lcs = "";
		StringBuilder sb = new StringBuilder(1000);
		if (solutions.size() == 0)
			return "No solution\n";

		List<Action> actions = solutions.iterator().next().actions;

		if (solutions.size() > 1) {
			String x;

			x = template.apply_template(actions);
			for (int i = 1; i < solutions.size(); i++) {
				String y = template.apply_template(solutions.get(i).actions);
				x = BranchingUtils.longestCommonSubstring(x, y);
			}
			if (x.lastIndexOf('.') != -1) {
				lcs = x.substring(0, x.lastIndexOf('.') + 1);
			}
		}

		if (!solutions.isEmpty()) {
			sb.append("Solutions: \n");

			if (template.apply_template(actions).startsWith(lcs)) {
				sb.append("common text:\n");
				sb.append(lcs);
			}

			for (Solution s : solutions) {
				try {
					sb.append("\nNr of actions: " + s.actions.size() + "\n");
					String ss = template.apply_template(s.actions);
					if (ss.startsWith(lcs)) {
						ss = ss.replaceFirst(lcs, "");
					}
					sb.append(ss);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		return sb.toString();

	}

	public void simulateCleanup() {
		System.out.println("Applying different clean up methods: ");
		List<Solution> tmp;
		for (BranchingSearch.CompareMethod m : BranchingSearch.CompareMethod
				.values()) {
			System.out.print(m);
			tmp = BranchingSearch.cleanupSolutions(m, delta, solutions);
			System.out.print(" - " + tmp.size() + " - ");
			for (Solution s : tmp) {
				System.out.print(s.ids() + " ");
			}
			System.out.println();
		}

	}

	public void doCleanup() {
		List<Solution> tmp;
		// int cc=0;
		// for (Solution ss : solutions) {
		// int nn = StringUtils.countMatches(ss.toString(), "kill");
		// nn+= StringUtils.countMatches(ss.toString(), "eat-person");
		// cc+=nn;
		// System.out.println(nn);
		// }
		// System.out.println("Average: " + ((double)cc) /
		// ((double)solutions.size()));
		// tmp = filterSolutions();
		// solutions = tmp;
		// // cc=0;
		// for (Solution ss : solutions) {
		// int nn = StringUtils.countMatches(ss.toString(), "kill");
		// nn+= StringUtils.countMatches(ss.toString(), "eat-person");
		// cc+=nn;
		// System.out.println(nn);
		// }
		// System.out.println("Average: " + ((double)cc) /
		// ((double)solutions.size()));
		tmp = BranchingSearch.cleanupSolutions(usm, delta, solutions);
		solutions = tmp;
		System.out.println("Final cleanup with " + usm + ": "
				+ solutions.size());

	}

	public List<Solution> filterSolutions() {
		List<Solution> ret = solutions;
		for (SolutionFilter sf : filters) {
			ret = sf.filterSolutions(ret);
		}

		// List<Solution> ret = new LinkedList<Solution>(solutions);
		// Iterator<Solution> r = ret.iterator();
		// for (SolutionFilter sf:filters) {
		// while (r.hasNext()) {
		// Solution s = r.next();
		// boolean res = sf.acceptSolution(s);
		// if (!res) {
		// System.out.println("removing " + s);
		// // int nn = StringUtils.countMatches(s.toString(), "kill");
		// // nn+= StringUtils.countMatches(s.toString(), "eat-person");
		// // System.out.println(nn);
		// r.remove();
		// }
		// }
		//
		// }
		return ret;
	}

	public void runStory(Template template, JTextArea out, JTextField in)
			throws NumberFormatException, IOException {
		if (template == null)
			return;

		out.append("Welcome to the interactive : "
				+ solutions.get(0).domainName + " Story\n");
		out.append("\n");
		out.append("A story with " + solutions.size() + " paths and "
				+ ends.size() + " endings\n\n\n");

		STRIPSState current = start;
		mainLoop(template, out, in, current);

	}

	/**
	 * @param template
	 * @param out
	 * @param in
	 * @param current
	 * @return
	 * @throws IOException
	 */
	private StringBuilder mainLoop(final Template template,
			final JTextArea out, final JTextField in, STRIPSState current)
			throws IOException {
		Collection<Action> aa = ggraph.getOutEdges(current);
		StringBuilder ssf = new StringBuilder();
		if (aa == null) {
			out.append("No action from the start?\n");
			return ssf;
		}

		do {

			if (aa.size() == 1) {
				Action a = aa.iterator().next();
				out.append(actionToString(template, a));
				ssf.append(actionToString(template, a));
				if (a != null) {
					current = ggraph.getOpposite(current, a);

				}
			} else if (aa.size() > 1) {
				int n = 0;
				final Map<Integer, Action> hm = new MapWrapper<Integer, Action>();
				out.append("   Please select the next action: " + "\n");
				Iterator<Action> it = aa.iterator();
				while (it.hasNext()) {
					n++;
					Action a = it.next();
					out.append("   " + n + ") " + actionToString(template, a));
					hm.put(n, a);
				}
				out.append("" + "\n");
				final STRIPSState c2 = current;
				in.addKeyListener(new KeyAdapter() {
					@Override
					public void keyReleased(KeyEvent e) {
						if (e.getKeyCode() != KeyEvent.VK_ENTER)
							return;
						int a = 0;
						String s = in.getText();
						try {
							a = Integer.parseInt(s);
						} catch (Exception x) {

						}
						try {
							a = Integer.parseInt(s);
							Action a2 = hm.get(a);
							if (a2 != null) {
								out.append(actionToString(template, a2));
								in.removeKeyListener(this);
								in.setText("");
								mainLoop(template, out, in,
										ggraph.getOpposite(c2, a2));
							}

						} catch (Exception x) {
							return;
						}
						super.keyReleased(e);
					}
				});
				return ssf;
			} else {
				break;
			}

			aa = ggraph.getOutEdges(current);
		} while (aa.size() > 0);
		out.append("The end");
		return ssf;
	}

	public void runStory(Template template, PrintStream out, InputStream in)
			throws NumberFormatException, IOException {
		if (template == null)
			return;

		out.println("Welcome to the interactive : "
				+ solutions.get(0).domainName + " Story");
		out.println();
		out.println("A story with " + solutions.size() + " paths and "
				+ ends.size() + " endings");

		STRIPSState current = start;
		Collection<Action> aa = ggraph.getOutEdges(current);
		if (aa == null) {
			out.println("No action from the start?");
			return;
		}
		StringBuilder ssf = new StringBuilder();
		do {

			if (aa.size() == 1) {
				Action a = aa.iterator().next();
				out.print(actionToString(template, a));
				ssf.append(actionToString(template, a));
				if (a != null) {
					current = ggraph.getOpposite(current, a);

				}
			} else if (aa.size() > 1) {
				int n = 0;
				Map<Integer, Action> hm = new MapWrapper<Integer, Action>();
				out.println("   Please select the next action: ");
				Iterator<Action> it = aa.iterator();
				while (it.hasNext()) {
					n++;
					Action a = it.next();
					out.print("   " + n + ") " + actionToString(template, a));
					hm.put(n, a);
				}
				out.println("");
				// BufferedReader is = new BufferedReader(new InputStreamReader(
				// in),2);
				while (true) {
					int a = in.read();
					if (a == 10) {
						a = in.read();
					}
					a -= 48;
					try {

						Action a2 = hm.get(a);
						if (a2 != null) {
							current = ggraph.getOpposite(current, a2);
							out.println("   You chose: "
									+ actionToString(template, a2));
							break;
						}
					} catch (Exception x) {
						x.printStackTrace();
					}
				}
			} else {
				break;
			}

			aa = ggraph.getOutEdges(current);
		} while (aa.size() > 0);

		out.println("The End");
		out.println("Complete story :");
		out.println(ssf);
	}

	/**
	 * @param t
	 * @param a
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	protected String actionToString(Template t, Action a) throws IOException {
		// Pair<STRIPSState> p = g.getEndpoints(a);
		// Collection<Action> cc = g.findEdgeSet(p.getFirst(), p.getSecond());
		// String s1= a.name.toString();
		//
		// int c =
		// Integer.decode(s1.substring(s1.indexOf('@')+1,s1.lastIndexOf('-')));
		// a = (Action)a.clone();
		// a.name = new OperatorName(s1.substring(0,s1.indexOf('@')));
		// String b = s1.substring(s1.indexOf('@')+1);
		// s1 = template.apply_template(a);
		return "" + t.apply_template(realActions.get(a));
	}

	// public void DFSVisitChar(STRIPSState s, Graph<STRIPSState, Action> f,
	// Set<Action> toRemovef, Set<STRIPSState> toRemovesf, Set<STRIPSState>
	// explored, CharacterFilter cf) {
	// explored.add(s);
	// Collection<Action> aa = f.getOutEdges(s);
	// if (aa.size()>1)
	//
	// for (Action a : aa) {
	// boolean keep = cf.isActionFromCharacter(a);
	// STRIPSState e = f.getDest(a);
	// if (!keep) {
	// toRemovef.add(a);
	// toRemovesf.add(s);
	// }
	//
	// if (!explored.contains(e))
	// DFSVisitChar(e, f, toRemovef, toRemovesf, explored, cf);
	//
	// }
	// else
	// for (Action a : aa) {
	// STRIPSState e = f.getDest(a);
	// if (!explored.contains(e))
	// DFSVisitChar(e, f, toRemovef, toRemovesf, explored, cf);
	// }
	//
	// }
	//

	public void keepCharacterBranching(CharacterFilter charFilter) {

		Collection<Action> c = ggraph.getEdges();
		Set<Action> charActions = new HashSet<Action>();
		Set<Action> keepAct = new SetWrapper<Action>();
		Set<STRIPSState> KeepState = new SetWrapper<STRIPSState>();
		for (Action a : c) {
			boolean keep = charFilter.isActionFromCharacter(a);
			if (keep) {
				charActions.add(a);
			}
		}
		Iterator<Action> it = charActions.iterator();
		while (it.hasNext()) {
			Action a = it.next();
			Collection<Action> ne = ggraph.getOutEdges(ggraph.getSource(a));
			boolean keep = false;
			for (Action b : ne) {
				if (!b.equals(a)) {
					keep = charFilter.isActionFromCharacter(b);
				}
			}
			if (!keep) {
				it.remove();
			}

		}
		Collection<STRIPSState> cc = ggraph.getVertices();
		for (Action a : charActions) {
			// for each action involving character c
			STRIPSState s = ggraph.getSource(a);
			// add the state preceding the action
			KeepState.add(s);
			// move backwards until the start
			Collection<Action> b = ggraph.getInEdges(s);

			while (b.size() > 0) {
				Action prev = b.iterator().next();
				for (Action pa : b) {
					if (charActions.contains(pa)) {
						prev = pa;
					}
				}
				keepAct.add(prev);
				s = ggraph.getSource(prev);
				KeepState.add(s);
				b = ggraph.getInEdges(s);
			}
			// now we move forward
			s = ggraph.getDest(a);
			b = ggraph.getOutEdges(s);
			while (b.size() > 0) {
				Action prev = b.iterator().next();
				for (Action pa : b) {
					if (charActions.contains(pa)) {
						prev = pa;
					}
				}
				keepAct.add(prev);
				s = ggraph.getDest(prev);
				KeepState.add(s);
				b = ggraph.getOutEdges(s);
			}

		}
		Set<Action> ra = new HashSet<Action>();
		Set<STRIPSState> rs = new HashSet<STRIPSState>();

		for (Action a : c) {
			if (!(charActions.contains(a) || keepAct.contains(a))) {
				ra.add(a);
			}

		}
		for (STRIPSState s : cc)
			if (!KeepState.contains(s)) {
				rs.add(s);
			}
		for (Action a : ra) {
			ggraph.removeEdge(a);
		}
		for (STRIPSState s : rs) {
			ggraph.removeVertex(s);
		}

	}

}
