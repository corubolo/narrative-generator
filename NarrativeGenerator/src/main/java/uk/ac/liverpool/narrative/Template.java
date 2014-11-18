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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;

import javaff.data.Action;
import javaff.data.Parameter;
import javaff.data.Plan;

public class Template extends HashMap<String, String> {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public static String apply_template(File templateFile, Plan plan)
			throws IOException {
		Template t = readTemplate(templateFile);
		List<Action> ac = plan.getActions();
		StringBuilder b = new StringBuilder();
		for (Action a : ac) {
			String out = t.get("" + a.name);
			int m = 1;
			for (Parameter p : a.parameters) {
				out = out.replaceAll(">" + m, "" + p.getName());
				m++;

			}
			out = Character.toUpperCase(out.charAt(0)) + out.substring(1);

			b.append(out);
			b.append(".\n");
		}
		return b.toString();
	}

	public String apply_template(List<Action> ac) throws IOException {

		StringBuilder b = new StringBuilder();
		for (Action a : ac) {
			String out = get("" + a.name);
			int m = 1;
			for (Parameter p : a.parameters) {
				out = out.replaceAll(">" + m, "" + p.getName());
				m++;

			}
			out = Character.toUpperCase(out.charAt(0)) + out.substring(1);

			b.append(out);
			b.append(".\n");
		}
		return b.toString();
	}

	public String apply_template(Action a) {

		StringBuilder b = new StringBuilder();
		{
			String out = get("" + a.name);
			int m = 1;
			for (Parameter p : a.parameters) {
				out = out.replaceAll(">" + m, "" + p.getName());
				m++;

			}
			out = Character.toUpperCase(out.charAt(0)) + out.substring(1);

			b.append(out);
			b.append(".\n");
		}
		return b.toString();
	}

	public static Template readTemplate(File templateFile) throws IOException {
		BufferedReader f = new BufferedReader(new FileReader(templateFile));
		Template t = new Template();
		String s = f.readLine();
		while (s != null) {
			int c = s.indexOf(' ');
			t.put(s.substring(0, c), s.substring(c + 1));
			s = f.readLine();
		}
		f.close();
		return t;
	}

	public static Template readTemplate(Reader r) throws IOException {
		BufferedReader f = new BufferedReader(r);
		Template t = new Template();
		String s = f.readLine();
		while (s != null) {
			int c = s.indexOf(' ');
			t.put(s.substring(0, c), s.substring(c + 1));
			s = f.readLine();
		}
		f.close();
		return t;
	}

}
