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
package javaff.search;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import uk.ac.liverpool.narrative.SetWrapper;


public  class BranchingUtils {


//	public static double euclideanDistance(float[] x, float[] y) {
//		double sumXY2 = 0.0;
//		for(int i = 0, n = x.length; i < n; i++) {
//			sumXY2 += Math.pow(x[i] - y[i], 2);
//		}
//		return Math.sqrt(sumXY2);
//	}

	public static double euclideanDistanceNorm(float[] x, float[] y) {
		double sumXY2 = 0.0;
		for(int i = 0, n = x.length; i < n; i++) {
			if (Float.isNaN(x[i])|| Float.isNaN(y[i]))
				sumXY2 +=1;
			else
				sumXY2 += Math.pow(x[i] - y[i], 2);
		}
		return Math.sqrt(sumXY2)/ Math.sqrt(x.length);
	}
	public static double euclideanDistance(float[] x, float[] y) {
		double sumXY2 = 0.0;
		for(int i = 0, n = x.length; i < n; i++) {
			if (Float.isNaN(x[i])|| Float.isNaN(y[i]))
				sumXY2 +=1;
			else
				sumXY2 += Math.pow(x[i] - y[i], 2);
		}
		return Math.sqrt(sumXY2);
	}

	public static double productDistanceNorm(float[] v1, float[] v2) {
		double distance = 0.0f;
		if (v1.length != v2.length)
			return Float.MAX_VALUE;

		for (int i = 0 ; i < v1.length ; i++) 
			distance += v1[i] * v2[i];

		return distance / v1.length;
	}	

	public static void main(String[] args) {
		String a = "",a1="ACGGTGTCGTGCTATGCTGATGCTGACTTATATGCTA";
		String b = "",b1="CGTTCGGCTATCGTACGTTCTATTCTATGATTTCTAA";
		for (int f=0;f<300; f++) {
			a+=a1;
			b+=b1;
		}
		long bf;
		bf =System.currentTimeMillis();
		//		System.out.println(lcs(a, b));
		//		System.out.println(System.currentTimeMillis()-bf);
		//		bf =System.currentTimeMillis();
		//		System.out.println(lcs2(a, b));
		//		System.out.println(System.currentTimeMillis()-bf);
		//		bf =System.currentTimeMillis();
		//		System.out.println(longestSubstring(a, b));
		//		System.out.println(System.currentTimeMillis()-bf);
		//		bf =System.currentTimeMillis();
		System.out.println(longestCommonSubstring(a, b));
		System.out.println(System.currentTimeMillis()-bf);
		bf =System.currentTimeMillis();
		//		System.out.println(longestSubstring(a, b));
		//		System.out.println(System.currentTimeMillis()-bf);
		//		bf =System.currentTimeMillis();

		System.out.println(longestCommonSubsequence(getArray (a),getArray (b)));
		System.out.println(System.currentTimeMillis()-bf);
		bf =System.currentTimeMillis();

	}

	public static <T> double jacardSimilarity(List<T> x, List<T> y) {

		if( x.size() == 0 || y.size() == 0 ) {
			return 0.0;
		}
		//		int inter = 0;
		//		for (T a:x) {
		//			if  (y.contains(a))
		//				inter++;
		//		}
		//		int union = x.size() + y.size() - inter;
		//		double j = (double)inter / (double) union;
		//		
		Set<T> unionXY = new HashSet<T>(x);
		unionXY.addAll(y);

		
		Set<T> intersectionXY = new HashSet<T>(x);
		intersectionXY.retainAll(y);
		//		if (j != ((double) intersectionXY.size() / (double) unionXY.size()))
		//			throw new IllegalArgumentException("KJJ");
		return (double) intersectionXY.size() / (double) unionXY.size(); 

	}
	public static <T> double jacardSimilarityGuava(List<T> x, List<T> y) {

		if( x.size() == 0 || y.size() == 0 ) {
			return 0.0;
		}
		//		int inter = 0;
		//		for (T a:x) {
		//			if  (y.contains(a))
		//				inter++;
		//		}
		//		int union = x.size() + y.size() - inter;
		//		double j = (double)inter / (double) union;
		//	
		Set<T> unionXY = new HashSet<T>(x);
		unionXY.addAll(y);

		
		Set<T> intersectionXY = new HashSet<T>(x);
		intersectionXY.retainAll(y);
		//		if (j != ((double) intersectionXY.size() / (double) unionXY.size()))
		//			throw new IllegalArgumentException("KJJ");
		return (double) intersectionXY.size() / (double) unionXY.size(); 

	}

	public static <T> double diceSimilarity(List<T> x, List<T> y) {
		Set<T> intersectionXY = new HashSet<T>(x);
		intersectionXY.retainAll(y);
		return (2.0* (double)intersectionXY.size()) / (double)(x.size() + y.size());
	}
	public static int levenshtein(String s, String t){
		/* if either string is empty, difference is inserting all chars 
		 * from the other
		 */
		if(s.length() == 0) return t.length();
		if(t.length() == 0) return s.length();

		/* if first letters are the same, the difference is whatever is
		 * required to edit the rest of the strings
		 */
		if(s.charAt(0) == t.charAt(0))
			return levenshtein(s.substring(1), t.substring(1));

		/* else try:
		 *      changing first letter of s to that of t,
		 *      remove first letter of s, or
		 *      remove first letter of t
		 */
		int a = levenshtein(s.substring(1), t.substring(1));
		int b = levenshtein(s, t.substring(1));
		int c = levenshtein(s.substring(1), t);

		if(a > b) a = b;
		if(a > c) a = c;

		//any of which is 1 edit plus editing the rest of the strings
		return a + 1;
	}
	private static int minimum(int a, int b, int c) {
		return Math.min(Math.min(a, b), c);
	}

	public static int computeLevenshteinDistance(CharSequence str1,
			CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 1; j <= str2.length(); j++)
			distance[0][j] = j;

		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j] = minimum(
						distance[i - 1][j] + 1,
						distance[i][j - 1] + 1,
						distance[i - 1][j - 1]
								+ ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
										: 1));

		return distance[str1.length()][str2.length()];
	}

	public static <E> int[] simHash(List<E> set, int k) {
		int [] hashes = new int[set.size()];

		for (int i=0;i<set.size();i++) {
			int hash = set.get(i).hashCode();
			hashes[i] = hash;
		}
		Arrays.sort(hashes);
		return Arrays.copyOf(hashes, k);
	}

	public static double similarityMinHash(int[] a, int[]b) {
		int k = a.length;
		Set<Integer> A = new SetWrapper<Integer>();
		Set<Integer> B = new SetWrapper<Integer>();

		for (int i=0;i<k;i++) {
			A.add(a[i]);
			B.add(b[i]);
		}
		Set<Integer> unionAB = new SetWrapper<Integer>(A);
		unionAB.addAll(B);

		Set<Integer> intersectionXY = new SetWrapper<Integer>(A);
		intersectionXY.retainAll(B);
		intersectionXY.retainAll(unionAB);

		//		if (j != ((double) intersectionXY.size() / (double) unionXY.size()))
		//			throw new IllegalArgumentException("KJJ");
		return (double) intersectionXY.size() / (double) k; 

	}

	public static int computeLevenshteinDistance(String s1, String s2) {
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++) {
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++) {
				if (i == 0)
					costs[j] = j;
				else {
					if (j > 0) {
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
				costs[s2.length()] = lastValue;
		}
		return costs[s2.length()];
	}
	private static Character[] getArray(String aa) {
		Character [] cc = new Character[aa.length()];
		for (int a=0; a<cc.length;a++) { 
			cc[a] = aa.charAt(a);
		}
		return cc;
	}
	public static String lcs(String a, String b) {
		int[][] lengths = new int[a.length()+1][b.length()+1];

		// row 0 and column 0 are initialized to 0 already

		for (int i = 0; i < a.length(); i++)
			for (int j = 0; j < b.length(); j++)
				if (a.charAt(i) == b.charAt(j))
					lengths[i+1][j+1] = lengths[i][j] + 1;
				else
					lengths[i+1][j+1] =
					Math.max(lengths[i+1][j], lengths[i][j+1]);

		// read the substring out from the matrix
		StringBuffer sb = new StringBuffer();
		for (int x = a.length(), y = b.length();
				x != 0 && y != 0; ) {
			if (lengths[x][y] == lengths[x-1][y])
				x--;
			else if (lengths[x][y] == lengths[x][y-1])
				y--;
			else {
				assert a.charAt(x-1) == b.charAt(y-1);
				sb.append(a.charAt(x-1));
				x--;
				y--;
			}
		}

		return sb.reverse().toString();
	}

	//	public static String lcs2(String a, String b){
	//	    int aLen = a.length();
	//	    int bLen = b.length();
	//	    if(aLen == 0 || bLen == 0){
	//	        return "";
	//	    }else if(a.charAt(aLen-1) == b.charAt(bLen-1)){
	//	        return lcs(a.substring(0,aLen-1),b.substring(0,bLen-1))
	//	            + a.charAt(aLen-1);
	//	    }else{
	//	        String x = lcs(a, b.substring(0,bLen-1));
	//	        String y = lcs(a.substring(0,aLen-1), b);
	//	        return (x.length() > y.length()) ? x : y;
	//	    }
	//	}
	//	
	//	public static String longestSubstring(String str1, String str2) {
	//
	//		StringBuilder sb = new StringBuilder();
	//		if (str1 == null || str1.isEmpty() || str2 == null || str2.isEmpty())
	//		  return "";
	//
	//		// ignore case
	//		str1 = str1.toLowerCase();
	//		str2 = str2.toLowerCase();
	//
	//		// java initializes them already with 0
	//		int[][] num = new int[str1.length()][str2.length()];
	//		int maxlen = 0;
	//		int lastSubsBegin = 0;
	//
	//		for (int i = 0; i < str1.length(); i++) {
	//		for (int j = 0; j < str2.length(); j++) {
	//		  if (str1.charAt(i) == str2.charAt(j)) {
	//		    if ((i == 0) || (j == 0))
	//		       num[i][j] = 1;
	//		    else
	//		       num[i][j] = 1 + num[i - 1][j - 1];
	//
	//		    if (num[i][j] > maxlen) {
	//		      maxlen = num[i][j];
	//		      // generate substring from str1 => i
	//		      int thisSubsBegin = i - num[i][j] + 1;
	//		      if (lastSubsBegin == thisSubsBegin) {
	//		         //if the current LCS is the same as the last time this block ran
	//		         sb.append(str1.charAt(i));
	//		      } else {
	//		         //this block resets the string builder if a different LCS is found
	//		         lastSubsBegin = thisSubsBegin;
	//		         sb = new StringBuilder();
	//		         sb.append(str1.substring(lastSubsBegin, i + 1));
	//		      }
	//		   }
	//		}
	//		}}
	//
	//		return sb.toString();
	//		}


	public static <E> List<E> longestCommonSubsequence(E[] s1, E[] s2)
	{
		int[][] num = new int[s1.length+1][s2.length+1];  //2D array, initialized to 0

		//Actual algorithm
		for (int i = 1; i <= s1.length; i++)
			for (int j = 1; j <= s2.length; j++)
				if (s1[i-1].equals(s2[j-1]))
					num[i][j] = 1 + num[i-1][j-1];
				else
					num[i][j] = Math.max(num[i-1][j], num[i][j-1]);

		// System.out.println("length of LCS = " + num[s1.length][s2.length]);

		int s1position = s1.length, s2position = s2.length;
		List<E> result = new LinkedList<E>();

		while (s1position != 0 && s2position != 0)
		{
			if (s1[s1position - 1].equals(s2[s2position - 1]))
			{
				result.add(s1[s1position - 1]);
				s1position--;
				s2position--;
			}
			else if (num[s1position][s2position - 1] >= num[s1position - 1][s2position])
			{
				s2position--;
			}
			else
			{
				s1position--;
			}
		}
		Collections.reverse(result);
		return result;
	}


	// returns sequence
	public static String longestCommonSubstring(String S1, String S2)
	{
		int Start = 0;
		int Max = 0;
		for (int i = 0; i < S1.length(); i++)
		{
			for (int j = 0; j < S2.length(); j++)
			{
				int x = 0;
				while (S1.charAt(i + x) == S2.charAt(j + x))
				{
					x++;
					if (((i + x) >= S1.length()) || ((j + x) >= S2.length())) break;
				}
				if (x > Max)
				{
					Max = x;
					Start = i;
				}
			}
		}
		return S1.substring(Start, (Start + Max));
	}


}
