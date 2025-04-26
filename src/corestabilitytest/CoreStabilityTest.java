package corestabilitytest;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CoreStabilityTest
{
    /**
     * Converts an integer, i, to an n-bit binary string
     * @param i     Integer to convert
     * @param n     The number of bits
     * @return  i as an n-bit binary string
     */
    static LinkedList<Integer> bitfield(int i, int n)
    {
        // Direct copy from Python implementation
        String binaryString = Integer.toBinaryString(i);
        LinkedList<Integer> x = new LinkedList<>();
        
        for (char digit : binaryString.toCharArray()) 
            x.add(Character.getNumericValue(digit));
        
        Collections.reverse(x);
        
        while (x.size() < n)
            x.add(0);
        
        return x;
    }
    
    /**
     * Bijectively maps Set to its respective binary string 
     * @param Set   A linked list of integers
     * @param x     An integer between 0 and 2^n, where n is the size of Set
     * @return      result, a linked list of integers mapped to Set
     */
    static LinkedList<Integer> subsetNumber(LinkedList<Integer> Set, int x)
    {
        int n = Set.size();
        
        try
        {
            // Ensure that x is in the valid range (0, 2^n)
            if (x > Math.pow(2, n))
                throw new Exception("subsetNumber called with too large a number for the set.");
            
            LinkedList<Integer> bf = bitfield(x, n); // Get the n-bit binary string for x
            LinkedList<Integer> result = new LinkedList<>(); // Holds the mapping result
            
            // Bijectively map result to Set
            for (int i = 0; i < n; i++)
                if (bf.get(i) == 1)
                    result.add(Set.get(i));
            
            return result;
        }
        catch(Exception ex)
        {
            System.out.println("Generic Exception caught: " + ex.getMessage());
            return null;
        }
    }
    
    /**
     * Determines whether a given coalition strongly blocks a move
     * @param C         The coalition attempting to block
     * @param Seen      A possible set of seen agents
     * @param Win       The Win Matrix 
     * @param utility   The list of utilities
     * @return          True if the coalition blocks, false otherwise
     */
    static boolean strongBlockIfSeen(LinkedList<Integer> C, LinkedList<Integer> Seen, double[][] Win, LinkedList<Double> utility)
    {
        for (int i : C)
        {
            double u = 0; // Utility for the coalition attempting to block
            
            // Calculate utility
            for (int j : Seen)
                u += Win[i][j];

            // If the determined utiliy is not greater than the existing utility,
            //  the coalition does not block
            if (u <= utility.get(i))
                return false;
        }
        
        return true;
    }
    
    /**
     * Calculates the Cartesian Product for two sets
     * @param markers   The list of sets
     * @return          List of all subsets
     */
    static LinkedList<LinkedList<Integer>> findCartesianProduct(LinkedList<LinkedList<Integer>> markers)
    {
        if (markers.isEmpty())
            return null;
        
        LinkedList<LinkedList<Integer>> result = new LinkedList<>();
        if (markers.size() == 1) 
        {
            for (int element : markers.get(0)) 
            {
                LinkedList<Integer> innerList = new LinkedList<>();
                innerList.add(element);
                result.add(innerList);
            }
            return result;
        }
        
        List<LinkedList<Integer>> markersSublist = markers.subList(1, markers.size());
        
        // Convert List<LinkedList<...>> into LinkedList<LinkedList<...>>
        // This is probably a really inefficient way to do this, but we need 
        //  to be working with LinkedLists exclusively
        LinkedList<LinkedList<Integer>> convertedMarkersSublist = new LinkedList<>();
        
        for (LinkedList<Integer> marker : markersSublist)
            convertedMarkersSublist.add(marker);

        LinkedList<LinkedList<Integer>> remainingProduct = findCartesianProduct(convertedMarkersSublist);
        for (Integer element : markers.get(0)) {
            for (LinkedList<Integer> innerList : remainingProduct) {
                LinkedList<Integer> newList = new LinkedList<>();
                newList.add(element);
                newList.addAll(innerList);
                result.add(newList);
            }
        }
        
        return result;
    }
    
    /**
     * Determines if T is core stable
     * @param T     A k-tier list
     * @param Win   The Win matrix on the agents in T
     * @param n     The number of agents in T
     * @param k     The number of tiers in T
     * @return      True if T is core stable, false otherwise
     */
    static boolean isCoreStable(LinkedList<LinkedList<Integer>> T, double[][] Win, int n, int k)
    {
        // Get each agent's current utility and assign an index to each strict subset of each tier
        LinkedList<Double> utility = new LinkedList<>();
        for (int i = 0; i < n; i++)
            utility.add(0.0);
        
        LinkedList<LinkedList<Integer>> subsetCounts = new LinkedList<>();
        
        
        for (int i = 0; i < k; i++)
        {
            for (int a : T.get(i))
            {
                double u = 0; // Utility
                
                // Iterate over tiers from the lowest tier to the current tier
                for (int r = 0; r <= i; r++)
                    for (int b : T.get(r))
                        u += Win[a][b]; // Update agent's utility for each matchup
                
                if (a < utility.size()) // Don't attempt an out-of-bounds assignment
                    utility.set(a, u);
            }
            
            int size = T.get(i).size();
            
            // Shorthand way to add sets from 0 to 2^size
            subsetCounts.add(IntStream.range(0, (int) Math.pow(2, size)).boxed().collect(Collectors.toCollection(LinkedList::new)));
        }
        
        // For each tier, determine if that tier might form part of a blocking coalition
        for (int i = 0; i < k; i++)
        {
            LinkedList<LinkedList<Integer>> nextSubsetMarkers = new LinkedList<>();
            LinkedList<Integer> alreadySeen = new LinkedList<>();
            
            // Find the set of agents seen by agents in tier i
            for (int j = 0; j <= i; j++)
                alreadySeen.addAll(T.get(j));
            
            // Pick one value from each nextSubsetMarkers sublist
            for (int j = 0; j < k; j++)
                if (j == i)
                    nextSubsetMarkers.addLast(new LinkedList<>(Arrays.asList(0)));
                else
                    nextSubsetMarkers.addLast(subsetCounts.get(j));
            
            // Get all subsets from Cartesian Product
            LinkedList<LinkedList<Integer>> subsets = findCartesianProduct(nextSubsetMarkers);
            
            for (LinkedList<Integer> subset : subsets)
            {
                // Form each blocking coaltition
                LinkedList<Integer> C = new LinkedList<>();

                C.addAll(T.get(i));

                // Find the subsets corresponding to the subset indices picked by Cartesian Product
                for (int j = 0; j < k; j++)
                    C.addAll(subsetNumber(T.get(j), subset.get(j)));
                
                // Find each utility of each agent at each level
                LinkedList<Integer> Seen = new LinkedList<>(C);
                
                // Test for block at bottom tier, only if the coalition is not the set of seen agents
                if (! alreadySeen.equals(Seen))
                    if (strongBlockIfSeen(C, Seen, Win, utility))
                    {
                        // The tier blocks by moving the the lowest tier
                        System.out.println("Blocked by " + C + "\nC wants to move to tier 0");
                        return false;
                    }
                
                // Test for blocking at other positions in the list
                for (int j = 0; j < k; j++) // For each tier 
                {
                    boolean changed = false;
                    
                    for (int a : T.get(j)) // For each agent in tier j
                    {
                        if (! Seen.contains(a)) // If a is not seen
                        {
                            changed = true;
                            Seen.add(a);
                        }
                    }
                        
                    if (changed)
                        if (! alreadySeen.equals(Seen))
                            if (strongBlockIfSeen(C, Seen, Win, utility))
                            {
                                System.out.println("Blocked by " + C + "\nC wants to move to tier " + (j + 1));
                                return false;
                            }
                }
            }
        }
        
        // If we got this far, there are no blocking coalitions
        return true;
    }
    
    public static boolean testCase1()
    {
        final int n = 5;
        final int k = 3;
        
        double[][] winMatrix = 
        {
            {0, -1, 0.1, -1, 0.1},
            {1, 0, -1, -1, -1},
            {-0.1, 1, 0, -1, 0.2},
            {1, 1, 1, 0, 0.1},
            {-0.1, 1, -0.2, -0.1, 0}
        };
        
        LinkedList<LinkedList<Integer>> tierList = new LinkedList<>();
        
        // Test case 1.1
        /*
                        Agents
            Tier 0      [0, 1]  
            Tier 1      [2]
            Tier 2      [3, 4]
        
            Expected outcome: false
        */
        tierList.add(new LinkedList<>(Arrays.asList(0, 1)));
        tierList.add(new LinkedList<>(Arrays.asList(2)));
        tierList.add(new LinkedList<>(Arrays.asList(3, 4)));
        
        if (isCoreStable(tierList, winMatrix, n, k))
        {
            System.out.println("Test case 1.1 failed");
            return false;
        }
        
        tierList.clear();
        
        // Test case 1.2
        /*
                        Agents
            Tier 0      [1]  
            Tier 1      [0, 2, 4]
            Tier 2      [3]
        
            Expected outcome: true
        */
        tierList.add(new LinkedList<>(Arrays.asList(1)));
        tierList.add(new LinkedList<>(Arrays.asList(0, 2, 4)));
        tierList.add(new LinkedList<>(Arrays.asList(3)));
        
        if (! isCoreStable(tierList, winMatrix, n, k))
        {
            System.out.println("Test case 1.2 failed");
            return false;
        }
        
        tierList.clear();
        
        // Test case 1.3
        /*
                        Agents
            Tier 0      [0, 1]  
            Tier 1      [2, 4]
            Tier 2      [3]
        
            Expected outcome: true
        */
        
        tierList.add(new LinkedList<>(Arrays.asList(0, 1)));
        tierList.add(new LinkedList<>(Arrays.asList(2, 4)));
        tierList.add(new LinkedList<>(Arrays.asList(3)));
        
        if (! isCoreStable(tierList, winMatrix, n, k))
        {
            System.out.println("Test case 1.3 failed");
            return false;
        }
        
        // All test cases passed
        return true;
    }
    
    public static boolean testCase2()
    {
        final int n = 10;
        final int k = 8;
        
        double[][] winMatrix = {
            {0, -1, -1, -1, 1, 1, 1, 1, 1, 1},
            {1, 0, 1, -1, -1, -1, -1, 1, 1, -1},
            {1, -1, 0, 1, 1, 1, -1, -1, 1, -1},
            {1, 1, -1, 0, -1, -1, -1, -1, 1, 1},
            {-1, 1, -1, 1, 0, 1, -1, 1, 1, -1},
            {-1, 1, -1, 1, -1, 0, -1, 1, -1, 1},
            {-1, 1, 1, 1, 1, 1, 0, 1, 1, -1},
            {-1, -1, 1, 1, -1, -1, -1, 0, 1, 1},
            {-1, -1, -1, -1, -1, 1, -1, -1, 0, 1},
            {-1, 1, 1, -1, 1, -1, 1, -1, -1, 0}
        };

        
        LinkedList<LinkedList<Integer>> tierList = new LinkedList<>();
        
        // Test case 2.1
        /*
                        Agents
            Tier 0      [3, 4, 8]  
            Tier 1      [2]
            Tier 2      [9]
            Tier 3      [7]
            Tier 4      [5]
            Tier 5      [0]
            Tier 6      [1]
            Tier 7      [6]
        
            Expected outcome: false
        */
        tierList.add(new LinkedList<>(Arrays.asList(3, 4, 8)));
        tierList.add(new LinkedList<>(Arrays.asList(2)));
        tierList.add(new LinkedList<>(Arrays.asList(9)));
        tierList.add(new LinkedList<>(Arrays.asList(7)));
        tierList.add(new LinkedList<>(Arrays.asList(5)));
        tierList.add(new LinkedList<>(Arrays.asList(0)));
        tierList.add(new LinkedList<>(Arrays.asList(1)));
        tierList.add(new LinkedList<>(Arrays.asList(6)));
        
        if (isCoreStable(tierList, winMatrix, n, k))
        {
            System.out.println("Test case 2.1 failed");
            return false;
        }
        
        tierList.clear();
        
        // Test case 2.2
        /*
                        Agents
            Tier 0      [1, 8]  
            Tier 1      [3, 4]
            Tier 2      [2]
            Tier 3      [9]
            Tier 4      [7]
            Tier 5      [5]
            Tier 6      [6]
            Tier 7      [0]
        
            Expected outcome: true
        */
        tierList.add(new LinkedList<>(Arrays.asList(1, 8)));
        tierList.add(new LinkedList<>(Arrays.asList(3, 4)));
        tierList.add(new LinkedList<>(Arrays.asList(2)));
        tierList.add(new LinkedList<>(Arrays.asList(9)));
        tierList.add(new LinkedList<>(Arrays.asList(7)));
        tierList.add(new LinkedList<>(Arrays.asList(5)));
        tierList.add(new LinkedList<>(Arrays.asList(6)));
        tierList.add(new LinkedList<>(Arrays.asList(0)));
        
        if (! isCoreStable(tierList, winMatrix, n, k))
        {
            System.out.println("Test case 2.2 failed");
            return false;
        }
        
        
        // All test cases passed
        return true;
    }
    
    public static void main(String[] args)
    {
        boolean testCase1Result = testCase1();
        boolean testCase2Result = testCase2();
        
        if (! testCase1Result)
            System.out.println("\nTest Case 1 failed");
        
        if (! testCase2Result)
            System.out.println("\nTest Case 2 failed");
        
        if (testCase1Result && testCase2Result)
            System.out.println("\nAll test cases passed");
    }
}
