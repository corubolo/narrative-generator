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
import java.io.IOException;
import java.util.Hashtable;

import uk.ac.liverpool.narrative.RESTService.Job;

public class User {

	public static final String HOME = System.getProperty("user.home");
	public static File base;
	
	public User(String name) {
		if (base == null)
			base = new File(HOME, ".narrativewebapp");
		this.name = name;
		try {
			File f = base;
			f = new File(f,name);
			if (f.exists()) {
				domains = new Hashtable<String, StoryWorld>(User.GENERALDOMAINS);
				for (File ff: f.listFiles()) {
					if (ff.isDirectory()) {
						String fn = ff.getName();
						try {
							StoryWorld sw = StoryWorld.parseDomain(ff);
							domains.put(fn, sw);
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Hashtable<String, Job> jobs  = new Hashtable<String, Job>();
	public static Hashtable<String, StoryWorld> GENERALDOMAINS= null;
	public Hashtable<String, StoryWorld> domains = null;
	public String name;


	public void saveDomain(StoryWorld sw) {
		if (domains==null)
			domains = new Hashtable<String, StoryWorld>(User.GENERALDOMAINS);
		domains.put(sw.name, sw);
		try {
			File f;
			f = base;
			f = new File(f,name);
			if (!f.exists())
				f.mkdirs();
			File ff = new File(f, sw.name);
			if (!ff.exists())
				ff.mkdirs();
			System.out.println("Save domain:" + ff + " for user " + name);
			sw.writeDomain(ff);

		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	
	



}
