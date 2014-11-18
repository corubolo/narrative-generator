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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;


public class StoryWorld {
	public String domain;
	public String[] problem;
	public String template;
	public String name;
	public String actionProp;
	public String charProp;
	public String storyProp;
	@Override
	public  StoryWorld clone() throws CloneNotSupportedException {

		StoryWorld c = new StoryWorld();
		c.domain = domain;
		c.problem = problem;
		c.template = template;
		c.name = name;
		c.actionProp=actionProp;
		c.charProp = charProp;
		c.storyProp = storyProp;
		return c;
	}
	
	/**
	 * @param ff
	 * @return
	 * @throws IOException 
	 */
	public static StoryWorld parseDomain(File ff)
			throws IOException {
		System.out.println("Parse Domain for " + ff);
		String fn = ff.getName();
		String a = convertStreamToString(new File(ff,"domain.pddl"));
		int n = 1;
		File pf = new File(ff,"problem" + n);
		List<String> b =new LinkedList<String>();
		do {
			String bb = convertStreamToString(pf);
			b.add(bb);
			n++;
			pf = new File(ff,"problem" + n);
		}
		while (pf.exists());

		String  c = convertStreamToString(new File(ff,"template"));
		StoryWorld sw = new StoryWorld();
		try {
			String  ap = convertStreamToString(new File(ff,"ActionCost.txt"));
			String  cp = convertStreamToString(new File(ff,"CharacterCost.txt"));
			String  sp = convertStreamToString(new File(ff,"AuthorDirection.txt"));
			sw.actionProp = ap;
			sw.charProp = cp;
			sw.storyProp = sp;
		} catch (Exception e) {

		}
		sw.domain  =  a;
		sw.problem = b.toArray(new String[0]);
		sw.template = c;
		sw.name = fn;
		return sw;
	}
	
	private static String convertStreamToString(File file) throws IOException {
		List<String>l = Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
		StringBuilder ss = new StringBuilder();
		for (String s:l)
			ss.append(s).append('\n');
		return ss.toString();
	}

	public  void writeDomain(File ff) throws IOException {
		FileWriter fw = new FileWriter(new File(ff,"domain.pddl"));
		fw.write(domain);
		fw.close();
		int n=1;
		for (String s:problem) {
			fw = new FileWriter(new File(ff,"problem"+n));
			fw.write(s);
			n++;
			fw.close();
		}
		fw = new FileWriter(new File(ff,"template"));
		fw.write(template);
		fw.close();
		if (actionProp!=null) {
			fw = new FileWriter(new File(ff,"ActionCost.txt"));
			fw.write(actionProp);
			fw.close();
		}
		if (charProp!=null) {
			fw = new FileWriter(new File(ff,"CharacterCost.txt"));
			fw.write(charProp);
			fw.close();
		}
		if (storyProp!=null) {
			fw = new FileWriter(new File(ff,"AuthorDirection.txt"));
			fw.write(storyProp);
			fw.close();
		}
		
	}

}
