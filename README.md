narrative-generator
===================
This software is Copyright (C) 2013 Fabio Corubolo Meriem Bendis
The University of Liverpool
Released under GPL version 3 license.

The aim of this application is to generate automatically plots that contain multiple branches. 

# How to build and run the application:

The narrative genrator includes both a commandline interface, and a web interface. 
In order to build this software, it is possible to use Maven from Apache; first, run 'mvn install' (possibly skipping tests) on the comamndline version; after that it will be possible to run 'mvn package' on the web interface project; this will create a war file (that can be deployed to any servlet container) in the 'target' folder. 

To quickly test the web application, it is also possilbe to run 'mvn jetty:run' on the web interface folder; this will start an instance if Jetty with a properly loaded web app. After this command, the web application can be opened on a browser at the local address: 'http://127.0.0.1:8080/NarrativeWebApp/'

# Application description:

Given an initial setup (defined in the "Story world" tab), the service will generate multiple plots in a single run, ("Story generation" tab).

This is obtained by using Automated planning techniques (based on heuristic search, FF) adapted for generating branching plots while maximising the randomness of the generated plots . 

After the generation process, it is possible to filter the stories ("Story analysis" tab) using different techniques and can be finally merged (based on the world state at each step) so to obtain a single branching story.

This set up allows to generate a number of variations on the same initial setup, and even to merge different generations coming from different set-ups having the same initial state and propositions, but for example different objectives.

The application works in three phases:

* Story world creation: 
the author must write the domain and problem specifications using the standard PDDL language.
Our system supports the basic STRIPS functionalities and negated preconditions; currently it will not support ADL and other advanced features.
Finally, a simple text template is used to convert actions as defined in PDDL into a more readable form.

* Story world generation:
Stories are generated based on a specific problem and a set of parameters for the generation process. There is a set of branching algorithms we have created to generate branching stories that are variated and generated efficiently; this can be chosen as ‘Branching algorithm’. Help on the specific parameters is given as tooltips on the Story generation stage.

* Story analysis:
- Similarity filtering: This section allows to apply different filters to the generated stories. Since many generated stories are only trivially different, it is possible to filter out similar stories and keep only the most different stories. The user can select the similarity measure to filter stories that are too similar. 
It is also possible to use the text template to filter based on the text representation of the actions (that usually contains less details of the action). For example, the actions: 
(kill hunter wolf forest gun)  and (kill hunter wolf home gun) are converted using the text template to: the hunter killed the wolf using his powerful gun. This means that using the text template, the location is considered not relevant and the two actions are considered to be the same.
- Property based filtering: To support the expression of the personality of the characters by defining filtering based on:
-  Action properties: a set of characteristics for the actions: for each action, we can define the level of each characteristic, in a 0_1 scale, for example: 
Action kill: violence 0.8 airness 0.1 adventurous 0. 3
-  Character properties: a set of characteristics for each character, that define the personality traits of the character. This can be expressed in the same characteristics used for the actions
-  Story properties: set of characteristics for the whole story.
The story is then filtered base on how good is the match between action and character or story properties. Only the best stories are filtered and the user can chose the percentage of stories to keep.
It is finally possible to see the filtered stories as text, or in a tree or graph representation. It is also possible to play the merged story graph as in interactive fiction. 


## Paul Watry: Principal Investigator
## Fabio Corubolo, Meriem Bendris: Authors, Co-Investigators, SW development


#Credits: 
Uses the JavaFF planner (Amanda Coles, Andrew Coles, Maria Fox and Derek Long.), available at http://www.inf.kcl.ac.uk/staff/andrew/JavaFF/
in the version maintained by David Pattison, at https://personal.cis.strath.ac.uk/david.pattison/
With large modifications from us.
The REST service was created using Resteasy: http://www.jboss.org/resteasy and Jackson
Graph visualization using Jung: http://jung.sourceforge.net/ and Jackson
