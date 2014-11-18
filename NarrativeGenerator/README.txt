Automated Narrative Generator - Main package and command-line interface

This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com - and Meriem Bendis - m.bendris@gmail.com

Released under GPL version 3 license.

The narrative generator was developed under guidance from Paul Watry. 

The aim of this application is to generate automatically plots that contain multiple branches. 
Given an initial setup (defined in the "Story world" tab), the service will generate multiple plots in a single run, ("Story generation" tab).
This is obtained by using Automated planning techniques (based on heuristic search, FF) adapted for generating branching plots while maximising the randomness of the generated plots . 
After the generation process, it is possible to filter the stories ("Story analysis" tab) using different techniques and can be finally merged (based on the world state at each step) so to obtain a single branching story.


Uses the JavaFF planner (Amanda Coles, Andrew Coles, Maria Fox and Derek Long.), 
available at http://www.inf.kcl.ac.uk/staff/andrew/JavaFF/
in the version maintained by David Pattison, at 
https://personal.cis.strath.ac.uk/david.pattison/
With modifications and additions from us (in particular in the "search" package)


 * This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com - and Meriem Bendis - m.bendris@gmail.com
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
 * along with BranchingStoryGenerator.  If not, see <http://www.gnu.org/licenses/>.
