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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

/**
 * @author fabio
 *
 */
public class TestPlanner {
	File domain,pfile;
	JavaFF_mod planner;
	
	final String rec = "(give mother red cake home)\n" + 
			"(tell-about mother red wolf home)\n" + 
			"(tell-about mother red grannyhome home)\n" + 
			"(give mother red butter home)\n" + 
			"(walk-to red home forest)\n" + 
			"(tell-about red wolf grannyhome forest)\n" + 
			"(eat-person wolf red forest)\n" + 
			"(walk-to hunter forest grannyhome)\n" + 
			"(walk-to wolf forest grannyhome)\n" + 
			"(kill hunter wolf grannyhome gun)\n" + 
			"(escape-from-belly red wolf grannyhome)\n" + 
			"(resurrect witch wolf)\n" + 
			"(eat-person wolf granny grannyhome)\n" + 
			"(kill hunter wolf grannyhome gun)\n" + 
			"(escape-from-belly granny wolf grannyhome)\n" + 
			"(give red granny butter grannyhome)\n" + 
			"(give red granny cake grannyhome)\n";
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		SetWrapper.setRandom(true);
		MapWrapper.setRandom(true);
		domain = new File("domains/LRRH/domain.pddl");
		pfile = new File("domains/LRRH/pfile0s");
		planner = new JavaFF_mod(domain, null);
		planner.EPSILON = planner.EPSILON.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		planner.MAX_DURATION = planner.MAX_DURATION.setScale(2, BigDecimal.ROUND_HALF_EVEN);
		planner.doFilterReachableFacts = false;
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		
	}

	public String writePlan(Plan p) {
		StringWriter fw = new StringWriter();
		for (Action a : p.getActions()) {
			fw.write("(" + a + ")\n");
		}
		return fw.toString();
	}
	
	/**
	 * Test method for {@link javaff.JavaFF_mod#plan(java.io.File)}.
	 */
	@Test
	public void testValidRandomPlanning() {
		JavaFF_mod.generator = new Random();
		planner.doFilterReachableFacts = false;
		
		Plan p = planner.plan(pfile);
		File tempPlan;
		try {
			tempPlan = File.createTempFile("plan", ".plan");
			FileWriter fw = new FileWriter(tempPlan);
			fw.write(writePlan(p));
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
		} catch (Exception e) {
			fail(e.getMessage()	);
		}

	}
	/**
	 * Test method for {@link javaff.JavaFF_mod#plan(java.io.File)}.
	 */
	@Test
	public void testForPlanningChages() {
		
		JavaFF_mod.generator = new Random(432432432);
		Plan p = planner.plan(pfile);
		File tempPlan;
		try {
			tempPlan = File.createTempFile("plan", ".plan");
			FileWriter fw = new FileWriter(tempPlan);
			fw.write(writePlan(p));
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
			//assertEquals(writePlan(p), rec);
			
		} catch (Exception e) {
			fail(e.getMessage()	);
		}

	}

}
