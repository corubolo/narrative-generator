
package javaff.data;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javaff.graph.FactMutex;
import uk.ac.liverpool.narrative.MapWrapper;
import uk.ac.liverpool.narrative.SetWrapper;

/**
 * Represents a series of facts and their associated mutexes.
 * @author David Pattison
 *
 */
public class MutexSpace implements Cloneable
{
	private Map<Fact, FactMutex> mutexMap;

	public MutexSpace()
	{
		this.mutexMap = new MapWrapper<Fact, FactMutex>();
	}
	
	public MutexSpace(MutexSet ms)
	{
		this();
		this.addMutexes(ms);
	}
	
	public void merge(MutexSpace mutexSet)
	{
		for (Entry<Fact, FactMutex> e : mutexSet.mutexMap.entrySet())
		{
			if (this.mutexMap.containsKey(e.getKey()))
			{
				this.mutexMap.get(e.getKey()).addMutex(e.getValue().getOthers());
			}
			else
				this.mutexMap.put(e.getKey(), e.getValue());
		}
//		this.mutexMap.putAll(mutexSet.mutexMap);
	}
	
	@Override
	public String toString()
	{
		return this.mutexMap.toString();
	}
	
	
	/**
	 * Gets the set of keys within this mutex set. Keys are facts which have a known mutex set.
	 * @return
	 */
	public Set<Fact> getKeys()
	{
		return this.mutexMap.keySet();
	}
	
	public boolean containsFact(Fact gc)
	{
		return this.mutexMap.containsKey(gc);
	}
	
	public boolean hasMutexes(Fact gc)
	{
		if (this.mutexMap.containsKey(gc))
			return this.mutexMap.get(gc).hasMutexes();
		
		return false;
	}
	
	/**
	 * Adds a mutex.
	 * 
	 * @param owner
	 * @param other
	 */
	public void addMutex(Fact owner, Fact other)
	{
//		if (this.mutexMap.containsKey(owner))
//		{
//			FactMutex existing = this.mutexMap.get(owner);
//			existing.addMutex(other);
//			
//			this.mutexMap.put(owner, existing);
//		}
//		else
//		{
			this.mutexMap.put(owner, new FactMutex(owner, other));
//		}
	}
	
	/**
	 * Add the mutex to the set. Note that this will overwrite existing mutexes for the parameter's
	 * "owner" property.
	 * @param mutex
	 */
	public void addMutexes(Collection<FactMutex> mutexes)
	{
		for (FactMutex mutex : mutexes)
			this.addMutex(mutex);
	}	
	
	/**
	 * Add the mutex to the set. Note that this will overwrite existing mutexes for the parameter's
	 * "owner" property.
	 * @param mutex
	 */
	public void addMutexes(MutexSet mutexes)
	{
		this.addMutex(mutexes.getFacts());
	}	
	
	/**
	 * Add the mutex to the set. Note that this will overwrite existing mutexes for the parameter's
	 * "owner" property.
	 * @param mutex
	 */
	public void addMutex(FactMutex mutex)
	{
		this.mutexMap.put(mutex.getOwner(), mutex);
	}	
	
	/**
	 * Creates a mutex for all facts in the set specified, such that f is mutex with (facts \ f) forall f in facts
	 * @param mutex
	 */
	public void addMutex(Set<Fact> facts)
	{
		for (Fact f : facts)
		{
			Set<Fact> mutex = new SetWrapper<Fact>(facts);
			mutex.remove(f);
			this.addMutex(f, mutex);
		}
	}	
	
	public void addMutex(Fact owner, Collection<Fact> other)
	{
//		if (this.mutexMap.containsKey(owner))
//		{
//			FactMutex existing = this.mutexMap.get(owner);
//			existing.addMutex(other);
//			
//			this.mutexMap.put(owner, existing);
//		}
//		else	
			this.mutexMap.put(owner, new FactMutex(owner, other));
	}	

	/**
	 * Gets the FactMutex for which the parameter is owner.
	 * @param gc
	 * @return The FactMutex, or null if the owner does not exist.
	 */
	public FactMutex getMutexes(Fact gc)
	{
		return this.mutexMap.get(gc);
	}
	
	/**
	 * Determines whether 2 proposition are mutex.
	 * @param a
	 * @param b
	 * @return
	 * @throws NullPointerException Thrown if the first parameter does not exist in this goal space.
	 */
	public boolean isMutex(Fact a, Fact b)
	{
		FactMutex m = this.mutexMap.get(a);
		if (m == null)
			return false;
//			throw new IllegalArgumentException("Cannot find "+ a.toString());
			
		return m.isMutexWith(b);
	}
	
	public void clear()
	{
		this.mutexMap.clear();
	}
	
	/**
	 * Removes all trace of the fact from this mutex space. 
	 * @param f
	 * @return
	 */
	public boolean removeMutexes(Fact f)
	{
		if (this.mutexMap.containsKey(f))
		{
			this.mutexMap.get(f).clear();
			
			for (Entry<Fact, FactMutex> m : this.mutexMap.entrySet())
			{
				if (m.getValue().getOthers().contains(f))
					m.getValue().getOthers().remove(f);
			}
			
			return this.mutexMap.remove(f) != null;
		}
		return false;
	}
	
	public Map<Fact, FactMutex> getMutexMap()
	{
		return mutexMap;
	}	
	
	/**
	 * Explicitely constructs a set of mutexes when called. Can be expensive on large problems.
	 * @return
	 */
	public Set<MutexSet> getMutexSets()
	{
		Set<MutexSet> sets = new SetWrapper<MutexSet>();
		for (FactMutex fm : this.mutexMap.values())
		{
			Collection<Fact> mut = fm.getAll();

			MutexSet ms = new MutexSet(mut);
			
			sets.add(ms);
		}
			
		
		return sets;
	}	
	
	public Collection<FactMutex> getMutexes()
	{
		return mutexMap.values();
	}
	
	public Object clone()
	{
		MutexSpace newThis = new MutexSpace();
		newThis.mutexMap = new MapWrapper<Fact, FactMutex>();
		for (Entry<Fact, FactMutex> e : this.mutexMap.entrySet())
		{
			Fact f = (Fact) e.getKey().clone();
			FactMutex mutex = (FactMutex) e.getValue().clone();
			newThis.mutexMap.put(f, mutex);
		}
		
		return newThis;
	}
}
