/***********************************************************************
 *
 * This software is Copyright (C) 2013 Fabio Corubolo - corubolo@gmail.com - and Meriem Bendis
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javaff.JavaFF_mod;
import javolution.util.FastSet;

public class SetWrapper<E> implements Set<E>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 8347860872867852415L;
	private Set<E> wrapped;
	private static Class<? extends Set> generatingClass = FastSet.class;
	private static boolean random = false;
	private List<E> ran;
	private static boolean randomizeEach = false;

	public static boolean isRandomizeEach() {
		return randomizeEach;
	}

	public static void setRandomizeEach(boolean randomizeEach) {
		SetWrapper.randomizeEach = randomizeEach;
	}

	public static boolean isRandom() {
		return random;
	}

	public static void setRandom(boolean r) {
		random = r;
	}

	public static Class<? extends Set> getImplementationClass() {
		return generatingClass;
	}

	public static void setSetImplementation(Class<? extends Set> s) {
		generatingClass = s;
	}

	public SetWrapper() {
		try {
			wrapped = generatingClass.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public SetWrapper(int k) {
		try {
			wrapped = generatingClass.newInstance();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public SetWrapper(Collection<? extends E> s) {
		try {
			wrapped = generatingClass.newInstance();
			wrapped.addAll(s);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return wrapped.contains(o);
	}

	@Override
	public Iterator<E> iterator() {

		if (random) {
			if (ran != null && !randomizeEach)
				return ran.iterator();
			ran = (List<E>) Arrays.asList(wrapped.toArray());
			Collections.shuffle(ran, JavaFF_mod.generator);
			return ran.iterator();
		} else
			return wrapped.iterator();
	}

	@Override
	public Object[] toArray() {

		if (random) {
			if (ran != null && !randomizeEach)
				return ran.toArray();
			ran = (List<E>) Arrays.asList(wrapped.toArray());
			Collections.shuffle(ran, JavaFF_mod.generator);
			return ran.toArray();
		} else
			return wrapped.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {

		if (random) {
			if (ran != null && !randomizeEach)
				return ran.toArray(a);
			ran = (List<E>) Arrays.asList(wrapped.toArray());
			Collections.shuffle(ran, JavaFF_mod.generator);
			return ran.toArray(a);
		} else
			return wrapped.toArray(a);
	}

	@Override
	public boolean add(E e) {
		ran = null;
		return wrapped.add(e);
	}

	@Override
	public boolean remove(Object o) {
		ran = null;
		return wrapped.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return wrapped.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		ran = null;
		return wrapped.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		ran = null;
		return wrapped.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		ran = null;
		return wrapped.removeAll(c);
	}

	@Override
	public void clear() {
		ran = null;
		wrapped.clear();
	}

	@Override
	public boolean equals(Object o) {
		return wrapped.equals(o);
	}

	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}

}
