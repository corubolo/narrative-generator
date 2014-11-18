/**
 * 
 */
package uk.ac.liverpool.narrative.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.Random;

import javaff.JavaFF_mod;
import javaff.data.Action;
import javaff.data.Plan;
import javaff.search.BranchingEnforcedHillClimbingSearch;
import javaff.search.EnforcedHillClimbingSearchMod;

//import org.apache.commons.lang.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Stopwatch;

import uk.ac.liverpool.narrative.BranchingStoryGenerator;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;
import uk.ac.liverpool.narrative.Solution;

/**
 * @author fabio
 *
 */
public class TestBranchingStoryGeneratorActionCosts {
	File domain = new File("domains/LRRH/domain.pddl");
	File pfile = new File("domains/LRRH/pfile0s");
	File template = new File("domains/LRRH/red.tpl");
	File ac = new File("domains/LRRH/ActionCost.txt");
	File ad = new File("domains/LRRH/AuthorDirectionPeaceful.txt");
	File adv = new File("domains/LRRH/AuthorDirectionViolent.txt");
	


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {

	}

	public String writePlan(Solution p) {
		StringWriter fw = new StringWriter();
		for (Action a : p.actions) {
			fw.write("(" + a + ")\n");
		}
		return fw.toString();
	}

	/**
	 * Test method for {@link javaff.JavaFF_mod#plan(java.io.File)}.
	 */
	@Test
	public void testValidRandomPlanning() {

		BranchingStoryGenerator bs = new BranchingStoryGenerator();
		bs.writeSolutions = false;
		Stopwatch s = new Stopwatch();
		s.start();
		bs.EPSILON = bs.EPSILON.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		bs.MAX_DURATION = bs.MAX_DURATION.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		bs.doFilterReachableFacts = false;
		JavaFF_mod.generator = new Random(1);
		bs.domainFile = domain;
		bs.problemFile = pfile;
		bs.templateFile = template;
		bs.actionCostsFilePath = ac.getAbsolutePath();
		bs.authorDirection = ad.getAbsolutePath();
		bs.algoClass = BranchingEnforcedHillClimbingSearch.class;
		bs.plan(bs.problemFile);

		System.out.println("Took: " + s);
		for (Solution ss: bs.so) {
			System.out.println("Validating " + ss);
			File tempPlan;
			try {
				tempPlan = File.createTempFile("plan", ".plan");
				FileWriter fw = new FileWriter(tempPlan);
				fw.write(writePlan(ss));
				fw.close();
				File val = new File(new File("..").getCanonicalPath(),
						"Val/validate");//
				ProcessBuilder pb = new ProcessBuilder(val.getAbsolutePath(),
						domain.getAbsolutePath(), pfile.getAbsolutePath(),
						tempPlan.getAbsolutePath());
				pb.redirectErrorStream(true);
				Process pr = pb.start();
				InputStream io = pr.getInputStream();
				StringWriter sw = new StringWriter();
				int i;
				while ((i = io.read()) != -1) {
					sw.append((char) i);
				}
				pr.waitFor();
				assertTrue("Plan not valid: "+ sw.toString(), sw.toString().contains("Plan valid"));
				//ss.toString().
				
//				int n  = StringUtils.countMatches(ss.toString(), "kill");
//				n+= StringUtils.countMatches(ss.toString(), "eat-person");
//				System.out.println(n);
			} catch (Exception e) {
				fail(e.getMessage()	);
			}
		}

	}

}
