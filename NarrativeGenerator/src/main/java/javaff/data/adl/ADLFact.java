
package javaff.data.adl;

import java.util.Collection;

import javaff.data.Fact;
import javaff.data.strips.STRIPSFact;

/**
 * Represents a fact which exists as part of the ADL subset of PDDL.
 * 
 * @author dpattiso
 *
 */
public interface ADLFact extends Fact
{
	/**
	 * Converts this ADL fact into an equivalent STRIPS representation. Note that while the members of the collection returned will be derived
	 * from a STRIPSFact, the facts contained within these may not be. That is, the returned STRIPSFact may just encompass another ADLFact etc.
	 * @return
	 */
	public Collection<? extends STRIPSFact> toSTRIPS();
}
