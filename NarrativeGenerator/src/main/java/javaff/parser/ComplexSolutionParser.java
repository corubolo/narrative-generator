package javaff.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;
import java.util.StringTokenizer;

import javaff.data.Action;
import javaff.data.Fact;
import javaff.data.GroundProblem;
import javaff.data.Parameter;
import javaff.data.TotalOrderPlan;
import javaff.data.strips.OperatorName;
import javaff.data.strips.PDDLObject;
import javaff.data.strips.Proposition;
import uk.ac.liverpool.narrative.MapWrapper;

//TODO currently just the same as STRIPS parser- is this even needed?
/**
 * Parses temporal and numeric solution files.
 * @author David Pattison
 *
 */
public class ComplexSolutionParser
{
	public static TotalOrderPlan parseComplexPlan(GroundProblem gp, String solutionPath) throws IOException, ParseException
	{
		return ComplexSolutionParser.parseComplexPlan(gp, new File(solutionPath));
	}

	public static TotalOrderPlan parseComplexPlan(GroundProblem gp, File solutionFile) throws IOException, ParseException
	{		
		if (solutionFile.exists() == false)
			throw new FileNotFoundException("Cannot find solution file \""+solutionFile.getAbsolutePath()+"\"");
		//first, construct a map of the grounded parameters names to their objects
		GroundProblem groundProblem = (GroundProblem)gp.clone();
		Map<String, Parameter> map = new MapWrapper<String, Parameter>();
		for (Fact f : groundProblem.groundedPropositions)
		{
			Proposition p = (Proposition) f; 
			for (Object paro : p.getParameters())
			{
				Parameter param = (Parameter)paro;
//				System.out.println("Adding "+param.getName().toLowerCase());
				map.put(param.getName().toLowerCase(), param);
			}
		}
		
		FileReader fReader = new FileReader(solutionFile);
		
		BufferedReader bufReader = new BufferedReader(fReader);
		String line;
		
		TotalOrderPlan top = new TotalOrderPlan();
		Scanner scan;
		StringTokenizer strTok;
		
		try 
		{
			out : while (bufReader.ready())
			{
				line = bufReader.readLine();
				strTok = new StringTokenizer(line, ":() \t\n");
//				System.out.println("line is "+line);
				if (strTok.hasMoreTokens() == false || line.charAt(0) == ';' )
					continue;

				if (line.startsWith("(") == false)
						strTok.nextToken(); //skip action number
				
				OperatorName name = new OperatorName(strTok.nextToken());
				ArrayList<PDDLObject> vars = new ArrayList<PDDLObject>();
				while (strTok.hasMoreTokens())
				{
					String tok = strTok.nextToken().toLowerCase();
					Parameter var = map.get(tok);
					if (var == null)
						throw new NullPointerException("Cannot find parameter mapping: "+tok);
					
					vars.add((PDDLObject)var);
				}
				
				in : for (Object ao : groundProblem.actions)
				{
					Action a = (Action)ao;
					if (a.name.toString().equalsIgnoreCase(name.toString()))
					{
						for (int i = 0; i < vars.size(); i++)
						{
							Object ob = a.parameters.get(i);
							PDDLObject v = vars.get(i);
							if (ob.toString().equalsIgnoreCase(v.toString()) == false)
							{
								continue in;
							}
						}
						top.addAction(a);
						continue out;
					}
				}
				throw new ParseException("Parse error on line \n\t"+line);
			}
		} 
		catch (IOException e) 
		{
			throw new IOException("Incorrectly formatted solution file");
		}
		finally
		{
			bufReader.close();
			fReader.close();
		}
		
		return top;
	}}
