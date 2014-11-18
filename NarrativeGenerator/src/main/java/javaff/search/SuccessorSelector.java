//
//  SuccessorSelector.java
//  JavaFF
//
//  Created by Andrew Coles on Thu Jan 31 2008.
//

package javaff.search;

import java.util.Set;

import javaff.planning.State;

public interface SuccessorSelector {

	State choose(Set toChooseFrom);

};
