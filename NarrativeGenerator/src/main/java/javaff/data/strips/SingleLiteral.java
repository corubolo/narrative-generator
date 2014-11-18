package javaff.data.strips;

import java.util.List;

import javaff.data.Parameter;

public interface SingleLiteral extends STRIPSFact
{
	public PredicateSymbol getPredicateSymbol();

	public void setPredicateSymbol(PredicateSymbol n);
	
	public List<Parameter> getParameters();
	
	public void setParameters(List<Parameter> params);
	
	public Object clone();
}
