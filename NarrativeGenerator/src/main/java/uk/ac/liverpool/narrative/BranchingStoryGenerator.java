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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import javaff.data.Action;
import javaff.data.GroundFact;
import javaff.data.GroundProblem;
import javaff.data.Plan;
import javaff.data.TotalOrderPlan;
import javaff.data.UngroundProblem;
import javaff.planning.Filter;
import javaff.planning.HelpfulFilter;
import javaff.planning.STRIPSState;
import javaff.planning.State;
import javaff.search.BranchingBestFirstSearch;
import javaff.search.BranchingSearch;
import javaff.search.CharacterStateFilter;
import javaff.search.HValueComparatorTotalCostActionCosts;
import javaff.search.SolutionListener;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Predicates;
import com.google.common.base.Stopwatch;

/**
 * Java FF-based branching story generator; uses JavaFF
 * 
 * @author Fabio Corubolo
 * @author Meriem Bendris
 */
public class BranchingStoryGenerator extends javaff.JavaFF_mod {

	public static final File DestinationFolder = new File(".", "solutions");

	private static final String copyright = "Copyright (C) 2012 Fabio Corubolo and Meriem Bendis - The University of Liverpool";

	private static String CURRENT_ARGS = null;

	public BranchingStoryGenerator(File domain, File solutionFile) {
		super(domain, solutionFile);

	}

	public BranchingStoryGenerator() {
		super((File) null, null);
	}

	public File problemFile;
	public File templateFile;

	public final static String actionCostType = "ActionCost";
	public final static String characterCostType = "CharacterCost";
	public final static String authorCostType = "AuthorDirection";

	public Thread current;

	@Parameter(names = { "-p", "-problem" }, description = "Path to problem file", required = true)
	public String problemFilePath;
	@Parameter(names = { "-d", "-domain" }, description = "Path to domain file", required = true)
	public String domainFilePath;
	@Parameter(names = { "-t", "-template" }, description = "Path to template file", required = true)
	public String templateFilePath;

	@Parameter(names = { "-ac" }, description = "Path to action cost file")
	public String actionCostsFilePath;
	@Parameter(names = { "-cc" }, description = "Path to character cost file")
	public String characterCostsFilePath;
	@Parameter(names = { "-ad" }, description = "Path to author direction file")
	public String authorDirection;
	@Parameter(names = { "-r" }, description = "Random seed")
	public long randomSeed = 0;
	@Parameter(names = { "-a" }, description = "Branching algorithm used")
	public String algorithm;
	@Parameter(names = { "-f" }, description = "Filtering algorithm used")
	public String filter;
	@Parameter(names = { "-c" }, description = "Acting character/s (comma separated)")
	public String character;
	@Parameter(names = { "-s" }, description = "Number of steps")
	public int steps = 0;
	@Parameter(names = { "-bd" }, description = "Branch depth")
	public int branchDepth = 0;
	@Parameter(names = { "-max" }, description = "Maximum plan length")
	public int maxPL = 0;

	@Parameter(names = { "-doStatics" }, description = "Try to remove statics from initial state")
	public boolean doStatics = false;
	@Parameter(names = { "-deterministic" }, description = "Use deterministic sets and maps")
	public boolean deterimistic = false;

	public boolean writeSolutions = true;

	public boolean silent = false;

	volatile boolean stopNow = false;

	public List<Solution> so;

	public Class<? extends BranchingSearch> algoClass = BranchingBestFirstSearch.class;
	public Class<? extends Filter> filterClass = HelpfulFilter.class;

	static Set<Class<? extends BranchingSearch>> algos = getAlgos();
	static Set<Class<? extends Filter>> filters = getFilters();

	public BranchingSearch branchingSearh;

	public List<State> sol;

	private String pname;

	public Set<SolutionListener> sollis = new HashSet<SolutionListener>();

	public static Set<Class<? extends BranchingSearch>> getAlgos() {

		Reflections reflections = new Reflections("javaff.search");

		return reflections.getSubTypesOf(BranchingSearch.class);
	}

	public static Set<Class<? extends Filter>> getFilters() {

		Reflections reflections = new Reflections("javaff.planning");

		return reflections.getSubTypesOf(Filter.class);
	}

	public static void main(String args[]) {
		// try {Thread.sleep(10000);
		//
		// } catch (Exception x) {};

		Stopwatch s = new Stopwatch();
		s.start();
		EPSILON = EPSILON.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		MAX_DURATION = MAX_DURATION.setScale(2, BigDecimal.ROUND_HALF_EVEN);

		BranchingStoryGenerator planner = new BranchingStoryGenerator();
		planner.current = Thread.currentThread();
		JCommander jc = new JCommander(planner);
		jc.setProgramName("BranchingStoryGenerator");
		try {
			jc.parse(args);
		} catch (ParameterException x) {
			jc.usage();
			usage();
			System.out.println(x.getMessage());
			System.exit(-1);
		}
		usage();
		// reminder: this seems to rule out many new solutions when run on
		// branching EHCSearch! So leave to false
		// check
		planner.doFilterReachableFacts = false;

		System.out.println("Current invocation:");
		CURRENT_ARGS = Arrays.toString(args);
		System.out.println(CURRENT_ARGS);
		if (planner.domainFilePath == null || planner.problemFilePath == null
				|| planner.templateFilePath == null) {
			// usage();
			System.exit(-1);
		}

		planner.post_process_options();
		planner.plan(planner.problemFile);
		System.out.println("Planning took: " + s.toString());

	}

	private void post_process_options() {
		domainFile = new File(domainFilePath);
		templateFile = new File(templateFilePath);
		problemFile = new File(problemFilePath);
		for (Class<? extends BranchingSearch> c : algos) {
			if (c.getSimpleName().equals(algorithm)) {
				algoClass = c;

			}
		}
		if (filter != null) {
			for (Class<? extends Filter> c : filters) {
				if (c.getSimpleName().equals(filter)) {
					filterClass = c;
				}
			}
		}
		if (randomSeed != 0) {
			seed = randomSeed;

		}
		if (doStatics) {
			UngroundProblem.doStatics = true;
			STRIPSState.doStatics = true;
		}

		generator = new Random(seed);
		System.out.println("Seed: " + seed);
		SetWrapper.setRandom(!deterimistic);
		MapWrapper.setRandom(!deterimistic);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				stopGeneration();

			}
		});
	}

	public void stopGeneration() {
		stopNow = true;
		branchingSearh.stop();
		try {
			current.join();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	private static void usage() {

		System.out.print("\n\nAlgorithms: \n");
		for (Class<? extends BranchingSearch> c : algos) {
			System.out.print(" " + c.getSimpleName() + "\t\t[");
			Set<Method> mm = ReflectionUtils.getAllMethods(c, Predicates.and(
					ReflectionUtils.withModifier(Modifier.PUBLIC),
					ReflectionUtils.withPrefix("set")));
			for (Method m : mm) {
				System.out.print(m.getName() + " ");
			}
			System.out.println("]");
		}

		System.out.print("\n\nFilters: ");
		for (Class<? extends Filter> c : filters) {
			System.out.print(" " + c.getSimpleName());
		}
		System.out.println();

	}

	@Override
	protected Plan doPlan(GroundProblem ground) {

		if (this.isGoalValid(ground, ground.goal) == false)
			throw new NullPointerException(
					"Goal is at-least partially unreachable");
		pname = ground.name;
		if (ground.sometimes.size() > 0) {
			for (GroundFact f : ground.sometimes) {
				if (!this.isGoalValid(ground, f))
					throw new IllegalArgumentException(
							"Intermediate goal is at-least partially unreachable");
			}
		}
		if (!silent) {
			logger.info("Random Set: " + SetWrapper.isRandom());
			logger.info("Random map: " + MapWrapper.isRandom());
		}
		logger.info("Final Action set is " + ground.actions.size());

		STRIPSState currentInitState;
		currentInitState = ground.getSTRIPSInitialState();

		currentInitState.sometimes = ground.sometimes;
		currentInitState.always = ground.always;

		// always are implemented in : public Set<State> getNextStates(Set
		// actions)
		// sometimes require step by step planning, so is implemented in the
		// different search algos (BS.search)
		TotalOrderPlan bestPlan = null;

		logger.info("Running FF with Branching search...");
		logger.info("Algorithm: " + algoClass.getSimpleName() + " "
				+ "Filter: " + filterClass);
		sol = null;

		try {

			Filter f = filterClass.newInstance();
			branchingSearh = algoClass.newInstance();
			branchingSearh.setFilter(f);
			branchingSearh.silent = silent;

			// Action costs
			if (actionCostsFilePath != null
					&& (new File(actionCostsFilePath)).exists()) {
				HValueComparatorTotalCostActionCosts tc = new HValueComparatorTotalCostActionCosts();

				if (actionCostsFilePath != null
						&& (new File(actionCostsFilePath)).exists()) {
					tc.setActionCostMap(readCosts(actionCostsFilePath,
							actionCostType));
				}
				if (characterCostsFilePath != null
						&& (new File(characterCostsFilePath)).exists()) {
					tc.setCharacterCostMap(readCosts(characterCostsFilePath,
							characterCostType));
				}
				if (authorDirection != null
						&& (new File(authorDirection)).exists()) {
					tc.setAuthorDirection(readCosts(authorDirection,
							authorCostType).get(authorCostType));
				}
				branchingSearh.setComp(tc);
			}

			if (character != null) {

				branchingSearh.setSf(new CharacterStateFilter(character));
				branchingSearh.setStep(1);
			}
			if (branchDepth != 0) {
				branchingSearh.setBranchDepth(branchDepth);
			}
			if (steps != 0) {
				branchingSearh.setStep(steps);
			}

			branchingSearh.addSolutionListener(sollis);
			// final SolutionGraphics sg = new
			// SolutionGraphics(templateFilePath);
			// sg.createVisualStateGraph(true);
			// b.addSolutionListener(new SolutionListener() {
			//
			//
			//
			// @Override
			// public void processSolution(final Solution s) {
			// try {
			// SwingUtilities.invokeAndWait(new Runnable() {
			//
			// @Override
			// public void run() {
			// try {
			// sg.sg.addSolution(s);
			// } catch (Exception e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// sg.relayout();
			//
			// }
			// });
			//
			// } catch (Exception e) {
			// sg.relayout();
			// }
			//
			// }
			// });

			// THe actual search
			sol = branchingSearh.search(currentInitState);

			if (sol.size() == 0) {
				logger.info("No solution found! ");
				return null;
			}

			System.out.println("Total plans: " + BranchingSearch.totalSolutions
					+ " afrer filtering: " + sol.size() + " with "
					+ BranchingSearch.totalBranches + " branching operations");

			// Sort results alphabetically
			Collections.sort(sol, new Comparator<State>() {
				@Override
				public int compare(State o1, State o2) {
					List<Action> a1 = o1.plan.getActions();
					List<Action> a2 = o2.plan.getActions();
					int a = Math.min(a1.size(), a2.size());
					for (int i = 0; i < a; i++) {
						Action x = a1.get(i);
						Action y = a1.get(i);
						int c = x.toString().compareTo(y.toString());
						if (c != 0)
							return c;
					}
					return a1.size() - a2.size();

				}
			});

			// write out the solutions to file
			if (!silent) {
				System.out.println("Writing raw solutions");
				so = new LinkedList<Solution>();
				for (State s : sol) {
					so.add(new Solution(currentInitState, s, pname,
							CURRENT_ARGS, seed, domainFilePath, problemFilePath));
					System.out.println(Integer.toHexString(currentInitState
							.hashCode()
							+ s.hashCode()
							+ s.getPlan().getActions().hashCode()));
				}

				if (writeSolutions) {
					writeSolutions(so);
				}
			}
			// int cc=0;
			// for (Solution ss : so) {
			// int nn = StringUtils.countMatches(ss.toString(), "kill");
			// nn+= StringUtils.countMatches(ss.toString(), "eat-person");
			// cc+=nn;
			// System.out.println(nn);
			// }
			// System.out.println("Average: " + ((double)cc) /
			// ((double)so.size()));

			// final SolutionGraphics sg = new SolutionGraphics(so,
			// Template.readTemplate(templateFile));
			// SwingUtilities.invokeAndWait(sg);
			//
			// SwingUtilities.invokeAndWait(new Runnable() {
			//
			// @Override
			// public void run() {
			// sg.paintMinSpanningTree();
			//
			// }
			// });
			// SwingUtilities.invokeLater(new Runnable() {
			//
			// @Override
			// public void run() {
			// sg.createVisualStateGraph(false);
			//
			// }
			// });
			//
			// try {
			//
			// sg.sg.runStory(sg.template);
			// } catch (NumberFormatException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// } catch (InvocationTargetException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
			// } catch (InterruptedException e1) {
			// // TODO Auto-generated catch block
			// e1.printStackTrace();
		}

		return bestPlan;

	}

	/**
	 * @param ground
	 * @param currentInitState
	 * @param so
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private void writeSolutions(List<Solution> so) throws IOException,
			FileNotFoundException {
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd_HH-mm-ss");
		if (!DestinationFolder.exists()) {
			DestinationFolder.mkdirs();
		}
		File solutionFile = new File(DestinationFolder, so.get(0).domainName
				+ dateFormat.format(new Date()) + ".sol");
		ObjectOutputStream dao = new ObjectOutputStream(new GZIPOutputStream(
				new FileOutputStream(solutionFile)));
		dao.writeObject(so);
		dao.close();
		System.out.println("Written solution file: " + solutionFile);
	}

	public static Map<String, float[]> readCosts(String filepath,
			String fileType) {

		try {
			FileInputStream fstream = new FileInputStream(filepath);
			BufferedReader br = new BufferedReader(new InputStreamReader(
					fstream));
			return readCosts(br, fileType);
		} catch (Exception e) {
			System.out.println("Problem parsing " + filepath
					+ " action costs file");
			return null;
		}

	}

	/**
	 * @param filepath
	 * @param fileType
	 * @param costs
	 * @param br
	 * @return
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws NumberFormatException
	 */
	public static Map<String, float[]> readCosts(BufferedReader br,
			String fileType) throws IOException, IllegalArgumentException,
			NumberFormatException {
		String line;
		Map<String, float[]> costs = new HashMap<String, float[]>();
		if (((line = br.readLine()) == null)
				|| !fileType.equalsIgnoreCase(line.trim()))
			throw new IllegalArgumentException("Invalid file type");

		while ((line = br.readLine()) != null) {
			if (!line.startsWith("#") && !line.equals("")) { // Comment lines
																// starts with #
				String[] tmp = line.split("\\s");
				if (tmp.length < 2)
					return null;
				String action_name = tmp[0];
				float[] vec_costs = new float[tmp.length - 1];
				for (int i = 1; i < tmp.length; i++) {
					if (tmp[i].equals("n")) {
						vec_costs[i - 1] = Float.NaN;
					} else {
						vec_costs[i - 1] = Float.parseFloat(tmp[i]);
					}
				}

				costs.put(action_name, vec_costs);
			}
		}
		br.close();
		return costs;
	}

}
