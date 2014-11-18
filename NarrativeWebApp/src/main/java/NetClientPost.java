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


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import uk.ac.liverpool.narrative.StringSolution;

import com.google.common.net.MediaType;

public class NetClientPost {

	// http://localhost:8080/RESTfulExample/json/product/post
	public static void main(String[] args) {

		try {
			final String dom  ="(define (domain tempdom)\r\n" + 
					"\r\n" + 
					"(:requirements :strips :typing )\r\n" + 
					"\r\n" + 
					"(:types place locatable - object\r\n" + 
					"        thing character - locatable\r\n" + 
					"        animal person - character )\r\n" + 
					"\r\n" + 
					"(:constants\r\n" + 
					" low medium high - level)\r\n" + 
					"\r\n" + 
					"(:predicates \r\n" + 
					"			 (alive ?x - character)\r\n" + 
					"             (knows ?x - character ?y - object)\r\n" + 
					"             (has ?x - character ?y - thing)\r\n" + 
					"			 (at ?loc - place ?char - character)\r\n" + 
					"			 (connected ?loca ?locb - place)\r\n" + 
					"\r\n" + 
					"			 (good ?c - character)\r\n" + 
					"			 (sociable ?c - character)\r\n" + 
					"			 (hunger ?x - character ?l - level)\r\n" + 
					"			 (fear ?x - character ?y - character ?l - level)\r\n" + 
					"             (isclosed ?x - place)\r\n" + 
					"			 )\r\n" + 
					"\r\n" + 
					"(:action goto\r\n" + 
					":parameters (?x - character ?y - place ?z - place)\r\n" + 
					":precondition (and (at ?y ?x) (connected ?y ?z) (knows ?x ?z) (alive ?x) (not(isclosed ?z)))\r\n" + 
					":effect (and (not (at ?y ?x)) (at ?z ?x))\r\n" + 
					")\r\n" + 
					"\r\n" + 
					"(:action give\r\n" + 
					"  :parameters (?giver ?taker - person ?t - thing ?p - place)\r\n" + 
					"  :precondition (and (has ?giver ?t) (alive ?giver) (alive ?taker)  (at ?p ?taker) (at ?p ?giver) (not (= ?taker ?giver)) (knows ?giver ?taker))\r\n" + 
					"  :effect (and (has ?taker ?t) (not (has ?giver ?t)))\r\n" + 
					")\r\n" + 
					"\r\n" + 
					"(:action greet\r\n" + 
					"  :parameters (?chara ?charb - character ?p - place)\r\n" + 
					"  :precondition (and (sociable ?chara) (alive ?chara) (alive ?charb) (at ?p ?chara) (at ?p ?charb) (not (= ?chara ?charb)) )\r\n" + 
					"  :effect (and (knows ?chara ?charb) (knows ?charb ?chara) )\r\n" + 
					")\r\n" + 
					"\r\n" + 
					"(:action open\r\n" + 
					"  :parameters (?x - character ?p - place)\r\n" + 
					"  :precondition (and (isclosed ?p) (at ?p ?x))\r\n" + 
					"  :effect (and (not (isclosed ?p)) )\r\n" + 
					")\r\n" + 
					"\r\n" + 
					"(:action close\r\n" + 
					"  :parameters (?x - character ?p - place)\r\n" + 
					"  :precondition (and (not (isclosed ?p)) (at ?p ?x))\r\n" + 
					"  :effect (and (isclosed ?p) )\r\n" + 
					")\r\n" + 
					"\r\n" + 
					")\r\n";
			String pr= "(define (problem templateProblem) (:domain tempdom)\r\n" + 
					"(:requirements :strips :typing )\r\n" + 
					"(:objects\r\n" + 
					"	placea placeb - place\r\n" + 
					"	cake - thing\r\n" + 
					"	john tony - person\r\n" + 
					"    wolf - animal \r\n" + 
					") \r\n" + 
					"\r\n" + 
					"(:init	  \r\n" + 
					"  	\r\n" + 
					"	\r\n" + 
					"  	(alive john)\r\n" + 
					"	(alive tony)\r\n" + 
					"  \r\n" + 
					"	(at placea john)\r\n" + 
					"	(at placeb tony)\r\n" + 
					"	\r\n" + 
					"	(isclosed placeb)\r\n" + 
					"	(connected placea placeb)\r\n" + 
					"	\r\n" + 
					"	(knows john placeb)\r\n" + 
					"   	 (sociable john)\r\n" + 
					"	(has john cake)\r\n" + 
					"\r\n" + 
					")\r\n" + 
					" \r\n" + 
					"(:goal\r\n" + 
					"	(and \r\n" + 
					"		(has tony cake)\r\n" + 
					"	)\r\n" + 
					")\r\n" + 
					"\r\n" + 
					")\r\n" + 
					"";
			URL url = new URL(
					"http://10.0.0.92:8080/NarrativeWebApp/narrative/post?asynch=true");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("POST"); 
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
			connection.setRequestProperty("charset", "utf-8");
			String urlParameters = "domain="+URLEncoder.encode(dom, "UTF-8")+"&problem="+URLEncoder.encode(pr,"UTF-8")+"&asynch=true";
			byte[] data = urlParameters.getBytes("UTF-8"); 
			connection.setRequestProperty("Content-Length", "" + Integer.toString(data.length));
			connection.setUseCaches (false);

			OutputStream outputStream = connection.getOutputStream ();
			outputStream.write(data);
			outputStream.flush();
			outputStream.close();
			
			System.out.println(" RC " +connection.getResponseCode());
			System.out.println(connection.getHeaderField("Location"));

			BufferedReader br = new BufferedReader(new InputStreamReader(
					(connection.getInputStream())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {

				System.out.println(output);
			}

			connection.disconnect();

		} catch (MalformedURLException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();

		}

	}

}