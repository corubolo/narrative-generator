
package javaff.landmark;

import java.util.Collection;

import javaff.data.Fact;
import javaff.data.strips.SingleLiteral;

public interface ILandmarkGenerator 
{
	public void generateLandmarks(java.util.Collection<SingleLiteral> goals);
	
	public void clearLandmarks();

    public Collection<Fact> getLandmarks();
    
    public LandmarkGraph getLandmarkGraph();
    
}
