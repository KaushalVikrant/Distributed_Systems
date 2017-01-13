package pageRankSequential;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.io.FileWriter;

public class SequentialPageRank {
    // adjacency matrix read from file
    private HashMap<Integer, ArrayList<Integer>> adjList = new HashMap<Integer, ArrayList<Integer>>();
    private HashMap<Integer, ArrayList<Integer>> incomingList = new HashMap<Integer, ArrayList<Integer>>();
    // input file name
    private String inputFile = "";
    // output file name
    private String outputFile = "";
    // number of iterations
    private int iterations = 1000;
    // damping factor
    private double df = 0.85;
    // number of URLs
    private int size = 0;
    // calculating rank values
    private HashMap<Integer, Double> rankValues = new HashMap<Integer, Double>();

    /**
     * Parse the command line arguments and update the instance variables. Command line arguments are of the form
     * <input_file_name> <output_file_name> <num_iters> <damp_factor>
     *
     * @param args arguments
     */
    public void parseArgs(String[] args) {
    	
    	inputFile="E:\\Semester 3\\Distributed Systems\\Assignments\\Assignment 1 PageRank\\pagerank.input";
    	//inputFile="E:\\Semester 3\\Distributed Systems\\Assignments\\Assignment 1 PageRank\\pagerank.input.1000.urls.14";
    	outputFile="result.txt";
    	if(args.length>0)
    	{
    		inputFile=args[0];
	    	outputFile=args[1];
	    	iterations= Integer.parseInt(args[2]);
	    	df=Double.parseDouble(args[3]);
    	}
    }

    /**
     * Read the input from the file and populate the adjacency matrix
     *
     * The input is of type
     *
     0
     1 2
     2 1
     3 0 1
     4 1 3 5
     5 1 4
     6 1 4
     7 1 4
     8 1 4
     9 4
     10 4
     * The first value in each line is a URL. Each value after the first value is the URLs referred by the first URL.
     * For example the page represented by the 0 URL doesn't refer any other URL. Page
     * represented by 1 refer the URL 2.
     *
     * @throws java.io.IOException if an error occurs
     */
    public void loadInput() throws IOException {
    	String line = null;
    	try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = 
                new FileReader(inputFile);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
            ArrayList<Integer> NodeList=new ArrayList<>();

            while((line = bufferedReader.readLine()) != null) {
            	ArrayList<Integer> tempAL=new ArrayList<>();
            	String str[]=line.split(" ");
            	int node=Integer.parseInt(str[0]);
            	NodeList.add(node);
            	int i=1;
            	if(!incomingList.containsKey(node))
            	{
            		incomingList.put(node, new ArrayList<Integer>());
            	}
            	while(i<str.length)
            	{
            		int secondNode=Integer.parseInt(str[i]);
            		tempAL.add(secondNode);
            		i++;
            		if(incomingList.containsKey(secondNode))
            		{
            			incomingList.get(secondNode).add(node);
            		}
            		else
            		{
            			incomingList.put(secondNode, new ArrayList<Integer>(Arrays.asList(node)));
            		}
            	}
            	adjList.put(node, tempAL);
            }            
            bufferedReader.close();    
            fileReader.close();
            ArrayList<Integer> danglingNode=new ArrayList<>();
            
            for (Integer key : adjList.keySet()) {
            	if(adjList.get(key).size()==0)
            	{
            		adjList.put(key, NodeList);
            		danglingNode.add(key);
            	}
            }
            
            
            
            for(int key: adjList.keySet())
        	{
        		incomingList.get(key).addAll(danglingNode);
        	}
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                inputFile + "'");                
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + inputFile + "'");                  
             
             ex.printStackTrace();
        }

    }

    /**
     * Do fixed number of iterations and calculate the page rank values. You may keep the
     * intermediate page rank values in a hash table.
     */
    public void calculatePageRank() {
    	
    	double initialProbability=(double)1/adjList.size();
    	size=incomingList.size();
    	for(int key: incomingList.keySet())
    	{
    		rankValues.put(key, initialProbability);
    	}
    	for(int i=0;i<iterations;i++)
    	{
    		HashMap<Integer, Double> tempRank=new HashMap<Integer, Double>();
    		for(int key: rankValues.keySet())
        	{
    			double sumOfProbability=0;
    			for(int incomingNodes: incomingList.get(key))
    			{
    				sumOfProbability+=rankValues.get(incomingNodes)/adjList.get(incomingNodes).size();
    			}
    			double newRankValue=((double)(1-df)/size)+df*sumOfProbability;
    			
    			tempRank.put(key, newRankValue);	
        	}
    		
    		rankValues.clear();
    		rankValues.putAll(tempRank);
    	}

    }

    /**
     * Print the pagerank values. Before printing you should sort them according to decreasing order.
     * Print all the values to the output file. Print only the first 10 values to console.
     *
     * @throws IOException if an error occurs
     */
    public void printValues() throws IOException {
    	List<Entry<Integer,Double>> list=entriesSortedByValues(rankValues);
    	
    	System.out.println("Number of Iterations: "+iterations);
    	System.out.println("\nRanking in decreasing order:\n");
    	for (int i = 0; i < 10; i++) {
    		Entry<Integer,Double> en=list.get(i);
    		int key=en.getKey();
    		double value=en.getValue();
			System.out.println("Rank: "+i+"\t"+key+"\t"+value);
		}
    	FileWriter writer = new FileWriter(outputFile); 
    	for(int i = 0; i < list.size(); i++) {
    		Entry<Integer,Double> en=list.get(i);
    		int key=en.getKey();
    		double value=en.getValue();
    	  writer.write("Rank: "+i+"  "+key+"   "+value+"\n");
    	}
    	writer.close();
    }
    
    /*
     * Reference for Sorting hashmap:
     * http://stackoverflow.com/questions/11647889/sorting-the-mapkey-value-in-descending-order-based-on-the-value
     * 
     */
    
    static <K,V extends Comparable<? super V>> 
    List<Entry<K, V>> entriesSortedByValues(Map<K,V> map) {

    	List<Entry<K,V>> sortedEntries = new ArrayList<Entry<K,V>>(map.entrySet());
		Collections.sort(sortedEntries, 
		    new Comparator<Entry<K,V>>() {
		        @Override
		        public int compare(Entry<K,V> e1, Entry<K,V> e2) {
		            return e2.getValue().compareTo(e1.getValue());
		        }
		    }
		);
		
		return sortedEntries;
    }
    

    public static void main(String[] args) throws IOException {
    	/*
    	if(args.length!=4 )
    	{
    		System.out.println("Error: There should be 4 arguements\n"
    				+ "1. Input File (String)\n"
    				+ "2. Output File (String)\n"
    				+ "3. No. of Iterations (Integer)\n"
    				+ "4. Damping factor (Double)");
    		return;
    	}
    	*/
    	SequentialPageRank sequentialPR = new SequentialPageRank();

        sequentialPR.parseArgs(args);
        sequentialPR.loadInput();
        long startTime = System.currentTimeMillis();
        sequentialPR.calculatePageRank();
        long endTime   = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println(totalTime);
        sequentialPR.printValues();
    }
}

