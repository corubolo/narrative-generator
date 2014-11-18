/***********************************************************************
 *
 * This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com
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

import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javaff.JavaFF_mod;
import javaff.data.Action;
import javaff.data.strips.InstantAction;
import javaff.planning.Filter;
import javaff.planning.HelpfulFilter;
import javaff.planning.STRIPSState;
import javaff.search.BranchingBestFirstSearch;
import javaff.search.BranchingEnforcedHillClimbingSearch;
import javaff.search.BranchingHillClimbingSearch;
import javaff.search.BranchingMultipleSearch;
import javaff.search.BranchingSearch;
import javaff.search.BranchingSearch.CompareMethod;
import javaff.search.CharacterActionSolutionFilter;
import javaff.search.SolutionListener;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@Provider
@Path("/narrative/{username}")
public class RESTService {




	private static Hashtable<String, User> users  = new MyHT<String, User>();



	//ExecutorService xs = Executors.newFixedThreadPool(2);
	static List<Class<? extends BranchingSearch>> algos  = new LinkedList<Class<? extends BranchingSearch>>();
	static {
		algos.add(BranchingEnforcedHillClimbingSearch.class);
		algos.add(BranchingBestFirstSearch.class);
		algos.add(BranchingMultipleSearch.class);
		algos.add(BranchingHillClimbingSearch.class);
	}
	//	static Set<Class<? extends Filter>> filters = getFilters();
	//
	//	public static Set<Class<? extends BranchingSearch>> getAlgos() {
	//
	//		Reflections reflections = new Reflections("javaff.search");
	//
	//		return reflections.getSubTypesOf(BranchingSearch.class);
	//	}
	//
	//	public static Set<Class<? extends Filter>> getFilters() {
	//
	//		Reflections reflections = new Reflections("javaff.planning");
	//
	//		return reflections.getSubTypesOf(Filter.class);
	//	}

	public class Job {
		public BranchingStoryGenerator b;
		public List<Solution> sol;
		public Thread t;
		public boolean isDone() {
			if (t!=null)
				return !t.isAlive();
			return true;
		}
		public int upTo = 0;
		public StoryWorld sw;
	}
	public class StringJob {
		public String domainName;
		public String id;
		public int numSol;
	}
	
	public RESTService() {
		
		if (User.GENERALDOMAINS == null)
			try {
				System.out.println("Parsing domains");
				User.GENERALDOMAINS = new Hashtable<String, StoryWorld>();
				URL u= this.getClass().getResource("domains/");
				File f = new File (u.toURI());
				for (File ff: f.listFiles()) {
					if (ff.isDirectory()) {
						String fn = ff.getName();
						try {
							StoryWorld sw = StoryWorld.parseDomain(ff);
							User.GENERALDOMAINS.put(fn, sw);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

	}

	@GET
	@Path("/getName")
	@Produces("text/plain")

	public String getName(@PathParam("username") String userName) {
		return userName;
	}

	@GET
	@Path("/getBrAlgos")
	@Produces("application/json")

	public List<String> getBrAlgos() {
		List<String> s = new LinkedList<String>();

		for (Class<? extends BranchingSearch> c: algos) {
			s.add(c.getSimpleName());
		}
		return s;
	}
	@GET
	@Path("/retrieveID")
	@Produces("application/json")

	public List<StringSolution> retrievePlans(@QueryParam("id") final String id,@PathParam("username") String userName) {
		User u = users.get(userName);
		Job job = u.jobs.get(id);
		if (!job.isDone()) {
			return null;
		}
		Thread tr = job.t;
		if (tr!=null)
			try {
				tr.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		List<Solution> l = job.sol;
		List<StringSolution > sol  = new LinkedList<StringSolution>();
		for (Solution s: l) {
			sol.add(s.convert(job.sw.template));
		}
		return sol;
	}


	@GET
	@Path("/cleanupPlans")
	@Produces ("application/json")

	public List<StringSolution> cleanupPlans(@QueryParam("id") final String id,@QueryParam("method") String method, @QueryParam("amount")int amount,@PathParam("username") String userName) {
		User u = users.get(userName);
		Job job = u.jobs.get(id);

		SolutionGraph sg = new SolutionGraph();
		sg.setSolutions(job.sol);

		try {
			BranchingSearch.setTemplate(Template.readTemplate(new StringReader(job.sw.template)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		sg.usm = CompareMethod.valueOf(method);
		sg.delta = amount /10;
		sg.doCleanup();


		List<StringSolution > sol  = new LinkedList<StringSolution>();
		for (Solution s: sg.solutions) {
			sol.add(s.convert(job.sw.template));
		}
		job.sol = sg.solutions;
		return sol;
	}

	@GET
	@Path("/getGraphData")
	@Produces("application/json")

	public StringGraph getGraphData(@QueryParam("id") final String id,@PathParam("username") String userName) {
		User u = users.get(userName);
		Job job = u.jobs.get(id);
		if (job == null)
			return null;
		Template t = null;
		try {
			t = Template.readTemplate(new StringReader(job.sw.template));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		SolutionGraph sg = new SolutionGraph();
		sg.template = t;
		sg.setSolutions(job.sol);
		sg.populateGraph();
		StringGraph ret = new StringGraph();
		Collection<Action> edges = sg.ggraph.getEdges();
		Collection<STRIPSState> vertices = sg.ggraph.getVertices();
		
		for (Action e: edges) {
			StringEdge n = new StringEdge();
			try {
				n.label = sg.actionToString(t, e);
			} catch (IOException e1) {
				n.label = e.toString();
			}
			n.destination = ""+sg.ggraph.getDest(e).hashCode();
			n.source = ""+sg.ggraph.getSource(e).hashCode();
			if (sg.frequency.get(sg.ggraph.getEndpoints(e).getFirst())!=null)
				n.size = (int) (Math.log(sg.frequency.get(sg.ggraph.getEndpoints(e).getFirst()))*6.0);
			ret.edges.add(n);

		}
		for (STRIPSState s: vertices) {
			StringVertex v = new StringVertex();
			v.name = ""+s.hashCode();
			if (sg.start.equals(s))
				v.type = "start";			
			if (sg.ends.contains(s))
				v.type = "end";
			if (sg.frequency.get(s)!=null) {
				int dd= sg.frequency.get(s);
				v.size = (int) (Math.log(dd)*8.0); 
			}
			ret.vertices.add(v);
		}
		deHogSolution(job.sol);
		return ret;
	}


	@GET
	@Path("/applyStoryFilter")
	@Produces("application/json")

	public List<StringSolution> applyStoryFilter(@QueryParam("id") final String id,@QueryParam("actionProp") String actionProp,@QueryParam("charProp") String charProp,@QueryParam("authorProp") String authorProp, @QueryParam("amount")int amount,@PathParam("username") String userName) {
		User u = users.get(userName);
		Job job = u.jobs.get(id);
		SolutionGraph sg = new SolutionGraph();
		sg.setSolutions(job.sol);
		CharacterActionSolutionFilter tc = new CharacterActionSolutionFilter();
		try {
			tc.setActionCosts( BranchingStoryGenerator.readCosts(new BufferedReader(new StringReader(actionProp)), BranchingStoryGenerator.actionCostType));

			if (charProp!=null) 
				tc.setCharacterCosts( BranchingStoryGenerator.readCosts(new BufferedReader(new StringReader(charProp)),  BranchingStoryGenerator.characterCostType));
			else
				tc.setAuthorDirection( BranchingStoryGenerator.readCosts(new BufferedReader(new StringReader(authorProp)),  BranchingStoryGenerator.authorCostType).get( BranchingStoryGenerator.authorCostType));
			sg.filters.add(tc);
			tc.setThreshold(amount/100.0);

			sg.solutions  = sg.filterSolutions();
			job.sol = sg.solutions;
			List<StringSolution > sol  = new LinkedList<StringSolution>();
			for (Solution s: sg.solutions) {
				sol.add(s.convert(job.sw.template));
				//System.out.println(s.convert(job.sw.template));
			}
			return sol;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {}
		return null;
	}


	@GET
	@Path("/listSw")
	@Produces("application/json")

	public List<StoryWorld> listSw(@PathParam("username") String userName) {
		User u = users.get(userName);
		if (u.domains!=null)
			return new LinkedList<StoryWorld>(u.domains.values());
		else 
			return new LinkedList<StoryWorld>(User.GENERALDOMAINS.values());
	}

	@POST
	@Path("/saveSw")
	@Produces("text/plain")
	@Consumes("application/json")

	public String saveSw(@PathParam("username") String userName, StoryWorld sw) {
		User u = users.get(userName);
		u.saveDomain(sw);
		return "OK";
	}


	@GET
	@Path("/status")
	@Produces("text/plain")

	public String status(@QueryParam("id") String id,@PathParam("username") String userName) {
		User u = users.get(userName);
		Job job = u.jobs.get(id);
		if (job == null)
			return "";
		return ""+job.isDone();

	}
	
	
	@GET
	@Path("/activeJobs")
	@Produces("application/json")

	public List<StringJob> activeJobs(@PathParam("username") String userName) {
		User u = users.get(userName);
		Collection<Entry<String, Job>> jobs = u.jobs.entrySet();
		LinkedList<StringJob> ret = new LinkedList<>();
		for (Entry<String, Job> j:jobs) {
			if (!j.getValue().isDone()) {
				StringJob s = new StringJob();
				s.domainName = j.getValue().sw.name;
				s.id = j.getKey();
				s.numSol = j.getValue().sol.size();
 				ret.add(s);
				
			}
		}
		return ret;

	}
	@GET
	@Path("/stop")
	@Produces("text/plain")

	public String stop(@QueryParam("id") String id,@PathParam("username") String userName) {
		User u = users.get(userName);
		if (id == null)
			return  "Specify ID please";
		Job job = u.jobs.get(id);
		if (job == null)
			return  "not found";
		BranchingStoryGenerator b = job.b;
		if (b!=null) {
			Thread tr = job.t;
			b.current = tr;
			b.stopGeneration();
			b.current = null;
			b.so = null;
			b.sol = null;
			b.branchingSearh = null;
			job.b = null;
			deHogSolution(job.sol);
			System.gc();
		}
		return "Stopped";
	}


	@GET
	@Path("/solutionIds")
	@Produces("application/json") 

	public List<String> getSolutionIds(@PathParam("username") String userName) {
		User u = users.get(userName);

		return new LinkedList<String>(u.jobs.keySet());

	}
	@GET
	@Path("/solutionId")
	@Produces("application/json") 

	public List<String> getSolutionId(@QueryParam("domain") String domain,@PathParam("username") String userName) {
		User u = users.get(userName);

		LinkedList<String> l = new LinkedList<String>();
		for (Entry<String, Job> j: u.jobs.entrySet()) {
			//System.out.println(j.getValue().sw.name);
			if (j.getValue().sw.name.equals(domain)) {
				if (j.getValue().isDone())
					l.add(j.getKey());
			}
		}
		return l;

	}
	@GET
	@Path("/retrievePlansDomain")
	@Produces("application/json")

	public List<StringSolution> retrievePlansDomain(@QueryParam("domain") String domain,@PathParam("username") String userName) {
		User u = users.get(userName);
		LinkedList<StringSolution> ret = new LinkedList<StringSolution>();
		for (Entry<String, Job> j: u.jobs.entrySet()) {
			if (j.getValue().sw.name.equals(domain)) {
				if (j.getValue().isDone()) {
					List<Solution> l = j.getValue().sol;
					for (Solution s: l) {
						ret.add(s.convert(j.getValue().sw.template));
					}
				}

			}
		}
		return ret;
	}

	@GET
	@Path("/copyPlansDomain")
	@Produces ("text/plain")

	public String copyPlansDomain(@QueryParam("domain") String domain,@PathParam("username") String userName) {
		User u = users.get(userName);
		String id = "" + Long.toHexString(new Random().nextLong());
		StoryWorld sw = null;
		List<Solution> ret = new LinkedList<Solution>();
		for (Entry<String, Job> j: u.jobs.entrySet()) {
			if (j.getValue().sw.name.equals(domain)) {
				if (j.getValue().isDone()) {
					List<Solution> l = j.getValue().sol;
					ret.addAll(l);
					if (sw==null)
						sw = j.getValue().sw;
				}
			}


		}
		Job j = new Job();
		j.sol = ret;
		j.b = null;
		try {
			j.sw = sw.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		j.sw.name="";
		u.jobs.put(id, j);
		return id;
	}


	@GET
	@Path("/solutionIdNum")
	@Produces("application/json") 

	public int getSolutionIdNum(@QueryParam("domain") String domain,@PathParam("username") String userName) {
		User u = users.get(userName);
		int ret = 0;
		for (Entry<String, Job> j: u.jobs.entrySet()) {
			//System.out.println(j.getValue().sw.name);
			if (j.getValue().sw.name.equals(domain)) {
				if (j.getValue().isDone())
					ret+=j.getValue().sol.size();
			}
		}
		return ret;

	}

	@GET
	@Path("/deleteAllId")
	@Produces("application/json") 

	public int deleteAllId(@QueryParam("domain") String domain,@PathParam("username") String userName) {
		User u = users.get(userName);
		int ret = 0;
		LinkedList<String> remove = new LinkedList<String>();
		for (Entry<String, Job> j: u.jobs.entrySet()) {
			//System.out.println(j.getValue().sw.name);
			if (j.getValue().sw.name.equals(domain)) {
				if (j.getValue().isDone())
					remove.add(j.getKey());
			}
		}
		for (String s:remove) {
			u.jobs.remove(s);
			ret++;
		}
		return ret;

	}
	@GET
	@Path("/statusSol")
	@Produces("application/json") 

	public List<StringSolution> statusSol(@QueryParam("id") final String id,@PathParam("username") String userName) {
		User u = users.get(userName);
		List<StringSolution > sol  = new LinkedList<StringSolution>();
		if (id == null)
			return sol;
		Job job = u.jobs.get(id);
		if (job == null)
			return null;
		List<Solution> l = job.sol;

		if (l == null)
			return sol;
		int c = l.size();
		for (int a = job.upTo; a< c; a++) {
			Solution s = l.get(a);
			sol.add(s.convert(job.sw.template));
		}
		job.upTo = c;
		return sol;

	}

	@GET
	@Path("/generateTree")
	@Produces("image/png") 

	public byte[] generateTree(@QueryParam("id") final String id,@PathParam("username") String userName) {
		User u = users.get(userName);
		Job job = u.jobs.get(id);
		if (job == null)
			return null;
		List<Solution> l = job.sol;
		SolutionGraphics graph = new SolutionGraphics();
		SolutionGraph sg = new SolutionGraph();
		sg.setSolutions(job.sol);
		sg.populateGraph();
		
		//sg.cleanupLoops(4);
		try {
			graph.setTemplate(Template.readTemplate(new StringReader(job.sw.template)));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		graph.sg = sg;
		BufferedImage bi = graph.drawMinSpanningTree();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		deHogSolution(job.sol);
		try {
			ImageIO.write(bi, "png", baos);
			return baos.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@POST
	@Path("/getID")
	@Consumes("application/x-www-form-urlencoded")
	@Produces ("text/plain")

	public String getPlansID (@FormParam("name")  final String name,@FormParam("domain")  final String domain,  @FormParam("problem") final String problem, @FormParam("template") final String template, @FormParam("algo") final String algorithm, @FormParam("step") final int step,  @FormParam("rseed") final long rseed,@PathParam("username") String userName) {
		User u = users.get(userName);
		Class<? extends BranchingSearch> algoClass = null;
		if (algorithm!=null)
			for (Class<? extends BranchingSearch> c: algos) {
				if (c.getSimpleName().equals(algorithm)) {
					algoClass = c;

				}
			}
		final BranchingStoryGenerator branchingGenerator = new BranchingStoryGenerator();
		setUp(branchingGenerator);
		if (algoClass!=null)
			branchingGenerator.algoClass = (Class<? extends BranchingSearch>) algoClass;
		if (rseed!=0)
			JavaFF_mod.generator = new Random(rseed);
		if (step!=0)
			branchingGenerator.steps=step;
		String id = "" + Long.toHexString(new Random().nextLong());
		final List<Solution> l = new LinkedList<Solution>();
		final Job j = new Job();
		branchingGenerator.sollis.add(new SolutionListener() {
			@Override
			public void processSolution(Solution s) {
				l.add(s);

			}
		});

		Thread d = new Thread() {public void run() {
			final StringReader dr;
			dr = new StringReader(domain);
			final StringReader pr = new StringReader(problem);
			branchingGenerator.plan(dr, pr);
			j.b.current = null;
			j.b.so = null;
			j.b.sol = null;
			j.b.branchingSearh = null;
			j.b = null;
			deHogSolution(j.sol);
			
			System.gc();
		}};
		u.jobs.put(id, j);
		j.b = branchingGenerator;
		j.sol = l;
		j.t = d;
		StoryWorld sw = new StoryWorld();
		sw.domain = domain; 
		sw.problem = new String[]{problem}; 
		sw.template = template;
		sw.name = name;
		j.sw = sw;

		d.start();
		return id;

	}

	@POST
	@Path("/post")
	@Consumes("application/x-www-form-urlencoded")
	@Produces ("text/plain")
	
	public String validate(@FormParam("domain")  String dom,  @FormParam("problem") String prob) {
		try {
			return validateProblem(dom, prob);
		} catch (Exception e) {

			return e.getMessage();
		}
	}


	@POST
	@Path("/post")
	@Consumes("application/x-www-form-urlencoded")
	@Produces ("application/json")
	public List<StringSolution> getPlansPost(@FormParam("domain")  String dom,  @FormParam("problem") String prob) {
		BranchingStoryGenerator branchingGenerator = new BranchingStoryGenerator();
		setUp(branchingGenerator);
		branchingGenerator.plan(new StringReader(dom),new StringReader(prob));
		List<StringSolution > sol  = new LinkedList<StringSolution>();
		for (Solution s: branchingGenerator.so) {
			sol.add(s.convert());
		}
		return sol;	


	}

	/**
	 * @return
	 */
	private BranchingStoryGenerator setUp(BranchingStoryGenerator branchingGenerator) {

		branchingGenerator.doFilterReachableFacts = false;
		branchingGenerator.writeSolutions = false;
		branchingGenerator.silent = true;
		SetWrapper.setRandom(true);
		MapWrapper.setRandom(true);
		JavaFF_mod.EPSILON = JavaFF_mod.EPSILON.setScale(2,
				BigDecimal.ROUND_HALF_EVEN);
		JavaFF_mod.MAX_DURATION = JavaFF_mod.MAX_DURATION
				.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		branchingGenerator.algoClass = (Class<? extends BranchingSearch>) BranchingBestFirstSearch.class;
		branchingGenerator.filterClass = (Class<? extends Filter>) HelpfulFilter.class;
		return branchingGenerator;
	}


	public String validateProblem(String domain, String problem) throws IOException, InterruptedException,
	URISyntaxException {
		File tempProb = File.createTempFile("problem", ".prob");
		File tempDomain = File.createTempFile("domain", ".pddl");
		FileWriter fwp = new FileWriter(tempProb);
		FileWriter fwd = new FileWriter(tempDomain);
		fwd.write(domain);
		fwd.close();
		fwp.write(problem);
		fwp.close();
		URL u= this.getClass().getResource("ff/");
		File f = new File (u.toURI());
		File val = new File(f,"ff");
		String s = "";
		try {
			s = validate2(tempProb, tempDomain, val);
		} catch (Exception x) {
			x.printStackTrace();
			val= new File(f,"ff.exe");
			try {
				s = validate2(tempProb, tempDomain, val);
			} catch (Exception e) {
				x.printStackTrace();
				val= new File(f,"ff-linux");
				s = validate2(tempProb, tempDomain, val);
			}
		}
		return s;


	}
	
	public void deHogSolution(List<Solution> l ) {
		for (Solution s: l) {
			removeHog(s.start);
			removeHog((STRIPSState) s.end);
			removeHog(s.actions);
		}
		
	}

	private void removeHog(List<Action> actions) {
		for (Action a: actions) {
			((InstantAction)a).adds = null;
			((InstantAction)a).deletes = null;
		}
		
	}
	private void removeHog(STRIPSState start) {
		start.actions = null;
		start.RelaxedPlan = null;
		start.helpfulActions = null;
		start.setRPG(null);
		start.plan= null;
		start.sometimes= null;
		start.always= null;
		
	}
	/**
	 * @param tempProb
	 * @param tempDomain
	 * @param val
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws HeadlessException
	 */
	private String validate2(File tempProb, File tempDomain, File val)
			throws IOException, InterruptedException, HeadlessException {
		ProcessBuilder pb;
		pb = new ProcessBuilder(val.getAbsolutePath(),
				"-o",tempDomain.getAbsolutePath(), "-f",tempProb.getAbsolutePath());
		pb.redirectErrorStream(true);
		Process p = pb.start();
		InputStream io = p.getInputStream();
		StringWriter sw = new StringWriter();
		int i;
		while ((i = io.read()) != -1) {
			sw.append((char) i);
		}
		p.waitFor();
		String title="";
		String c = sw.toString();
		String found = "found legal plan as follows";
		if ((c.indexOf(found)!=-1)) {
			title = "Found valid plan:";
			c = c.substring(c.indexOf("found legal plan as follows")+found.length(), c.indexOf("time spent:"));
		} else
			title  =("No plan found:");
		return title +"\n"+ c;
	}



}