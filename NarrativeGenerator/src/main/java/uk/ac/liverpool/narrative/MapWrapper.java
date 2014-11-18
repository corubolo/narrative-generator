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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javaff.JavaFF_mod;
import javolution.util.FastMap;

public class MapWrapper<K, V> implements Map<K, V>, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7337466057238804210L;

	private Map<K, V> wrapped;

	private static Class<? extends Map> generatingClass = FastMap.class;
	private static boolean random = false;
	private static boolean randomizeEach = false;

	public static boolean isRandomizeEach() {
		return randomizeEach;
	}

	public static void setRandomizeEach(boolean randomizeEach) {
		MapWrapper.randomizeEach = randomizeEach;
	}

	private List<V> ranv;
	private Set<K> rank;
	private Set<Entry<K, V>> rane;

	public MapWrapper() {
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

	public MapWrapper(int k) {
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

	public MapWrapper(Map<? extends K, ? extends V> s) {
		try {
			wrapped = generatingClass.newInstance();
			wrapped.putAll(s);
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
	public boolean containsKey(Object key) {
		return wrapped.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return wrapped.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return wrapped.get(key);
	}

	@Override
	public V put(K key, V value) {
		rank = null;
		rane = null;
		ranv = null;
		return wrapped.put(key, value);
	}

	@Override
	public V remove(Object key) {
		rank = null;
		rane = null;
		ranv = null;
		return wrapped.remove(key);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		rank = null;
		rane = null;
		ranv = null;
		wrapped.putAll(m);
	}

	@Override
	public void clear() {
		rank = null;
		rane = null;
		ranv = null;
		wrapped.clear();
	}

	@Override
	public Set<K> keySet() {
		if (random) {
			if (rank != null && !randomizeEach)
				return rank;
			ArrayList<K> ran = new ArrayList<K>(wrapped.keySet());// .toArray());
			Collections.shuffle(ran, JavaFF_mod.generator);
			rank = new HashSet<K>(ran);
			return rank;// .toArray();
		} else

			return wrapped.keySet();
	}

	@Override
	public Collection<V> values() {
		if (random) {
			if (ranv != null && !randomizeEach)
				return ranv;
			ranv = new ArrayList<V>(wrapped.values());// .toArray());
			Collections.shuffle(ranv, JavaFF_mod.generator);
			return ranv;// .toArray();
		} else
			return wrapped.values();
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		if (random) {
			if (rane != null && !randomizeEach)
				return rane;
			ArrayList<Entry<K, V>> ran = new ArrayList<Entry<K, V>>(
					wrapped.entrySet());// .toArray());
			Collections.shuffle(ran, JavaFF_mod.generator);
			rane = new HashSet<Entry<K, V>>(ran);
			return rane;// .toArray();
		} else
			return wrapped.entrySet();
	}

	@Override
	public boolean equals(Object o) {
		return wrapped.equals(o);
	}

	@Override
	public int hashCode() {
		return wrapped.hashCode();
	}

	public static Class<? extends Map> getGeneratingClass() {
		return generatingClass;
	}

	public static void setGeneratingClass(Class<? extends Map> generatingClass) {
		MapWrapper.generatingClass = generatingClass;
	}

	public static boolean isRandom() {
		return random;
	}

	public static void setRandom(boolean randomise) {
		MapWrapper.random = randomise;
	}

}
