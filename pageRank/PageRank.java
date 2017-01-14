

import mpi.*;
import java.io.*;
import java.util.*;
import java.util.Map.Entry;

//Sample command
//mpjrun.sh -np 2 PageRank "./pagerank.input" "./output.txt" .85 1000

public class PageRank {
	
	// adjacency matrix read from file
    private static HashMap<Integer, ArrayList<Integer>> adjList = new HashMap<Integer, ArrayList<Integer>>();
    private static  HashMap<Integer, ArrayList<Integer>> incomingList = new HashMap<Integer, ArrayList<Integer>>();
    // input file name
    private static String inputFile = "";
    // output file name
    private static String outputFile = "";
    // number of iterations
    private static int iterations = 400;
    // damping factor
    private static double df = 0.85;
    // number of URLs
    private static int size = 0;
    // calculating rank values
    private static HashMap<Integer, Double> rankValues = new HashMap<Integer, Double>();
    //An arrayList having all the nodes
    private static ArrayList<Integer> NodeList=new ArrayList<>();
    //The initial probability
    private static double initialProbability=0;
    /**
     * Parse the command line arguments and update the instance variables. Command line arguments are of the form
     * <input_file_name> <output_file_name> <num_iters> <damp_factor>
     *
     * @param args arguments
     */
    public static  void parseArgs(String[] args) {
    	
    	
    		inputFile=args[3];
    		//inputFile="E:\\Semester 3\\Distributed Systems\\Assignments\\Assignment 1 PageRank\\pagerank.input";
    		outputFile=args[4];
    		//outputFile="E:\\Semester 3\\Distributed Systems\\Assignments\\Assignment 1 PageRank\\pagerank.output";
	    	iterations= Integer.parseInt(args[6]);
    		//iterations=400;
    		df=Double.parseDouble(args[5]);
    		//df=0.85;
    	
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
    public static void loadInput() throws IOException {
    	String line = null;
    	try {
            // FileReader reads text files in the default encoding.
            FileReader fileReader = 
                new FileReader(inputFile);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
            //ArrayList<Integer> NodeList=new ArrayList<>();

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
            initialProbability=(double)1/adjList.size();
            for(int key: incomingList.keySet())
        	{
        		rankValues.put(key, initialProbability);
        	}
            
    		
            //MPI.COMM_WORLD.Bcast(incomingList, 0, NodeList.size(), MPI.OBJECT, 0);
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
    public static void calculatePageRank(int []num, int rank, int npSize) {
    	
    	
    	//double initialProbability=(double)1/adjList.size();
    	size=incomingList.size();
    	
    	
    	for(int i=0;i<iterations;i++)
    	{
    		HashMap<Integer, Double> tempRank=new HashMap<Integer, Double>();
    		Map<Integer,Double>[] arrOfObjects=new Map[1];
    		
    		
            //System.out.println("rankValues="+rankValues);	
    		for(int key=0;key<num.length;key++)
        	{
    			double sumOfProbability=0;
    			for(int incomingNodes: incomingList.get(num[key]))
    			{
    				sumOfProbability+=rankValues.get(incomingNodes)/adjList.get(incomingNodes).size();
    			}
    			double newRankValue=((double)(1-df)/size)+df*sumOfProbability;
    			tempRank.put(num[key], newRankValue);	
        	}
    	
    		
    		//System.out.println("temprankValues="+tempRank);
    		
    		/*for(int key: rankValues.keySet())
        	{
    			double sumOfProbability=0;
    			for(int incomingNodes: incomingList.get(key))
    			{
    				sumOfProbability+=rankValues.get(incomingNodes)/adjList.get(incomingNodes).size();
    			}
    			double newRankValue=((double)(1-df)/size)+df*sumOfProbability;
    			
    			tempRank.put(key, newRankValue);	
        	}
    		//updating the old rankvalues of the node from tempRank
    		for(int key: tempRank.keySet())
        	{
    			rankValues.put(key, tempRank.get(key));
        	}
    		*/
        	///MPI.COMM_WORLD.Barrier();
    		//rankValues.clear();
    		//rankValues.putAll(tempRank);
    		//System.out.println(MPI.COMM_WORLD.Rank());
    		//System.out.println("rankValues="+rankValues);
    		if(rank!=0)
    		{
	    		arrOfObjects[0]=tempRank;
	        	MPI.COMM_WORLD.Send(arrOfObjects, 0, 1, MPI.OBJECT, 0 , 2);
    		}
        	if(rank==0)
    		{
        		
    			for(int x=1;x<npSize;x++)
    			{
    				MPI.COMM_WORLD.Recv(arrOfObjects, 0, 1, MPI.OBJECT, x, 2);
    				for(int inner: arrOfObjects[0].keySet())
    				{
    					rankValues.put(inner , arrOfObjects[0].get(inner));
    				}
    			}
    			for(int key: tempRank.keySet())
            	{
        			rankValues.put(key, tempRank.get(key));
            	}
    			
    		}
        	Map<Integer,Double>[] rankAgain=new HashMap[1];
            rankAgain[0]=rankValues;
            MPI.COMM_WORLD.Bcast(rankAgain, 0, 1, MPI.OBJECT, 0);
            if(rank!=0)
            {
            	rankValues=(HashMap)rankAgain[0];
            }
    	}
    	
    	

    }

    /**
     * Print the pagerank values. Before printing you should sort them according to decreasing order.
     * Print all the values to the output file. Print only the first 10 values to console.
     *
     * @throws IOException if an error occurs
     */
    public  static void printValues() throws IOException {
    	List<Entry<Integer,Double>> list=entriesSortedByValues(rankValues);
    	
    	//System.out.println("Number of Iterations: "+iterations);
    	System.out.println("\nRanking in decreasing order:\n");
    	//System.out.println("Rank: "+"\t"+"Node"+"\t"+"Probability Value");
    	System.out.println("Node"+"\t"+"Probability Value");
    	for (int i = 0; i < 10; i++) {
    		Entry<Integer,Double> en=list.get(i);
    		int key=en.getKey();
    		double value=en.getValue();
			//System.out.println(i+"\t"+key+"\t"+value);
			System.out.println(key+"\t"+value);
		}
    	FileWriter writer = new FileWriter(outputFile); 
    	writer.write("Node"+"\t"+"Probability Value");
		for(int i = 0; i < list.size(); i++) {
    		Entry<Integer,Double> en=list.get(i);
    		int key=en.getKey();
    		double value=en.getValue();
    	  writer.write(key+"\t"+value+"\n");
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
    
    
    
    public static void mpiFunction(String[] args) throws IOException
    {
    	MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        //MPJPageRank.calculatePageRank();
        //mpj.printValues();
        
        
        
        int values[]=null;

        // generate an array with some random values
        Map<Integer,ArrayList<Integer>>[] arrOfObjects=new Map[2];
        if (rank == 0) {
        	PageRank.parseArgs(args);
        	PageRank.loadInput();
        	arrOfObjects[0] = incomingList;
        	arrOfObjects[1] = adjList;
        	/*
        	for(int i=1;i<size;i++)
        	{
        		MPI.COMM_WORLD.Send(arrOfObjects, 0, 1, MPI.OBJECT, i , 2);
        	}
        	*/
        	values = new int[NodeList.size()];
            for (int i = 0; i < NodeList.size(); i++) {
                values[i] = NodeList.get(i);
            }
        }
        int sizeBuf[] = new int[1];
        int localSize = 0;
        int[] nodeListVar=new int[1];
        nodeListVar[0]=NodeList.size();
        MPI.COMM_WORLD.Bcast(nodeListVar, 0, 1, MPI.INT, 0);
        int nodeListSize=NodeList.size();
        if(rank!=0)
        {
        	nodeListSize=nodeListVar[0];
        }
        

        MPI.COMM_WORLD.Bcast(arrOfObjects, 0, 2, MPI.OBJECT, 0);
        if(rank!=0)
        {
        	incomingList=(HashMap)arrOfObjects[0];
        	adjList=(HashMap)arrOfObjects[1];
        }
        
        // divide the array in to equal chunks of number of processes.
        // This assumes size of array is divisible by the number of processes.
        Map<Integer,ArrayList<Integer>> localMap=new HashMap<Integer, ArrayList<Integer>>();
        //int chunkSize = NodeList.size() / size;
        int chunkSize = nodeListSize / size;
        //System.out.println(NodeList);
        //System.out.println(chunkSize);
        
        
        if (rank == 0) {
            // first send each process the size that it should expect
            for (int i = 1; i < size; i++) {
                sizeBuf[0] = chunkSize;
                MPI.COMM_WORLD.Send(sizeBuf, 0, 1, MPI.INT, i, 1);
                
            }
            localSize = chunkSize;
        } else {
            // receive the size of the array to expect
        	//MPI.COMM_WORLD.Recv(arrOfObjects, 0, 1, MPI.OBJECT, 0 , 2);
        	localMap=arrOfObjects[0];
            MPI.COMM_WORLD.Recv(sizeBuf, 0, 1, MPI.INT, 0, 1);
            localSize = sizeBuf[0];
            
        }
        
        
        Map<Integer,Double>[] rankAgain=new HashMap[1];
        rankAgain[0]=rankValues;
        MPI.COMM_WORLD.Bcast(rankAgain, 0, 1, MPI.OBJECT, 0);
        if(rank!=0)
        {
        	rankValues=(HashMap)rankAgain[0];
        }
        //Map<Integer,Double>[] rankAgain=new Map[1];

        //System.out.println(hm);

        int localValues[] = new int[localSize];
        if (rank == 0) {
            // send the actual array parts to processes
            for (int i = 1; i < size; i++) {
            	int diff=0;
            boolean	var= i == size-1;
            	if(  var )
                {	
                	diff=nodeListSize-(chunkSize*size);
                	chunkSize+=diff;
                	
                }
            	
                MPI.COMM_WORLD.Send(values, i * (NodeList.size() / size), chunkSize, MPI.INT, i, 3);
                if(var)
                {
                	chunkSize-=diff;
                }
                	
            }
            // for process 0 we can get the local from the data itself
            
            for (int i = 0; i < chunkSize; i++) {
              localValues[i] = values[i];
              
           }
        } 
        else {
            // receive the local array
        	
        	boolean	var= rank == size-1;
        	if(  var )
            {	
            	int diff=nodeListSize-(chunkSize*size);
            	chunkSize+=diff;
            	localValues = new int[chunkSize];
            }
            MPI.COMM_WORLD.Recv(localValues, 0, chunkSize, MPI.INT, 0, 3);
        }

        // everyone calculates their local means
        //double localMean = MPIMean.culculateMean(localValues);
        
        PageRank.calculatePageRank(localValues,rank,size);
        //double localMeanBuf[] = new double[1];
        //localMeanBuf[0] = localMean;

        //double sum[] = new double[1];
        // now sum up all the means
        // this has to be done only in one machine, lets do it in rank 0 machine
        // every process other than 0 should send its local mean to process 0
        // 0th process should sum up these values and divide that sum by size

        // you can do all the above using one MPI call, allreduce, have a look at that method and try to do it
        // if your sum is correct, the following two should be same
        if (rank == 0) {
            // display the mean calculated using parallel processing
           // System.out.println("Mean calculated using MPI: " + sum[0]/size);
            // display the mean calculated using all the values
            //System.out.println("Real Mean calculated using the global values: " + MPIMean.culculateMean(values));
        	
        	
        	///////////////////////////////////////////////////////////
        	PageRank.printValues();
        }

        MPI.Finalize();
    }
    
    
    
	public static final int ARRAY_SIZE = NodeList.size();

    public static void main(String args[]) throws Exception {
    	/*System.out.println("MPJPageRank");
    	System.out.println(args.length);
    	for(int i=0;i<args.length;i++)
		{
			System.out.println(i+1+":"+args[i]);
		}*/
		
		if(args.length<4 )
    	{
    		System.out.println("Error: There should be 4 arguements\n"
    				+ "1. Input File (String)\n"
    				+ "2. Output File (String)\n"
    				+ "3. No. of Iterations (Integer)\n"
    				+ "4. Damping factor (Double)");
    		return;
    	}
    	
    	
    	PageRank.mpiFunction(args);
        

        
    	
        
    }

}
