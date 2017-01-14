package edu.iu.termproject.allreduceSeq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.CollectiveMapper;

import edu.iu.harp.example.DoubleArrPlus;
import edu.iu.harp.partition.Partition;
import edu.iu.harp.partition.Table;
import edu.iu.harp.resource.DoubleArray;
import edu.iu.termproject.KMeansConstants;

import edu.iu.harp.keyval.Long2DoubleKVTable;
//import edu.iu.harp.keyval.Long2DoubleKVPartitionCombiner;
import edu.iu.harp.keyval.Int2LongKVPartition;
import edu.iu.harp.keyval.Long2DoubleKVPartition;
import edu.iu.harp.keyval.TypeDoubleCombiner;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;


import edu.iu.termproject.minibatch.MiniBatchPlus;
import edu.iu.termproject.minibatch.MiniBatch;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.Random;
import java.util.HashMap;

public class KmeansMapper extends CollectiveMapper<String, String, Object, Object> {

	private int numMappers;
	//private int vectorSize;
	private int iteration;
	private int batchSize;
	private int batchSizeSplit;
	private int numMapper;
	private int remainder;
	private int start;
	private int end;
	private int threadNumber;
	private long kMeansStartTime;
	
	private ArrayList<MiniBatch> dataPointsThread;
	private Table<MiniBatch> previousCenTable;
	
	private Table<MiniBatch> parCenTable;
	
	
	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		LOG.info("start setup" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()));
		long startTime = System.currentTimeMillis();
		System.out.println("\n\n\n\n");
		System.out.println("setup: start");
		System.out.println("---------------ALL-REDUCE----------------");
		Configuration configuration = context.getConfiguration();
    	numMappers =configuration.getInt(KMeansConstants.NUM_MAPPERS, 10);
		System.out.println("numMappers : "+ numMappers);
    	/*vectorSize =configuration.getInt(KMeansConstants.VECTOR_SIZE, 20);
		System.out.println("vectorSize : "+vectorSize);*/
    	iteration = configuration.getInt(KMeansConstants.NUM_ITERATONS, 1);
		System.out.println("iteration : "+iteration);
		batchSize = configuration.getInt(KMeansConstants.BATCH_SIZE, 200);
		System.out.println("batchSize : "+batchSize);
		numMapper = configuration.getInt(KMeansConstants.NUM_MAPPERS, 2);
		System.out.println("NUM_CENTROIDS : "+numMappers);
		kMeansStartTime= configuration.getLong(KMeansConstants.START_TIME, -1000);
		System.out.println("START_TIME : "+kMeansStartTime);
		remainder=batchSize%numMapper;
		batchSize=batchSize/numMapper;
    	
		//System.out.println("\n\n\n\n");

	}

	protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
		LOG.info("Start collective mapper.");
		System.out.println("mapCollective: start");
		int id=context.getTaskAttemptID().getTaskID().getId();
		if(remainder+1<id)
		{
		  batchSize+=1;
		}
		System.out.println("\n\n--------Decided Batch Size for this process: "+batchSize+"-------------------");
		System.out.println("Context Id: "+context.getTaskAttemptID().getTaskID().getId());
	    long startTime = System.currentTimeMillis();
	    List<String> pointFiles = new ArrayList<String>();
	    while (reader.nextKeyValue()) {
	    	String key = reader.getCurrentKey();
	    	String value = reader.getCurrentValue();
		System.out.println("Key: " + key + ", Value: " + value);
	    	LOG.info("Key: " + key + ", Value: " + value);
	    	pointFiles.add(value);
	    }
	    Configuration conf = context.getConfiguration();
	    runKmeans(pointFiles, conf, context);
	    LOG.info("Total iterations in master view: " + (System.currentTimeMillis() - startTime));
	  }
	  
	 private void broadcastCentroids( Table<MiniBatch> cenTable) throws IOException{  
		 //broadcast centroids 
		  boolean isSuccess = false;
		  try {
			  isSuccess = broadcast("main", "broadcast-centroids", cenTable, this.getMasterID(),false);
		  } catch (Exception e) {
		      LOG.error("Fail to bcast.", e);
		  }
		  if (!isSuccess) {
		      throw new IOException("Fail to bcast");
		  }
	 }
	
	 private void computation(Table<MiniBatch> cenTable, Table<MiniBatch> previousCenTable,ArrayList<MiniBatch> dataPoints){
		double err=0;
		
		
		 for(MiniBatch dp: dataPoints){
		   
		  MiniBatch obj=dp;
		  
		   
				//for each data point, find the nearest centroid
				double minDist = -1;
				double tempDist = 0;
				
				Int2DoubleOpenHashMap aPoint = dp.getHashMap();
				int nearestPartitionID = -1;
				for(Partition ap: previousCenTable.getPartitions()){
					MiniBatch aCentroidMB = (MiniBatch) ap.get();
					Int2DoubleOpenHashMap aCentroidHM = aCentroidMB.getHashMap();
					
					tempDist = calcEucDistSquare(aCentroidHM , aPoint);
					//System.out.println("\nTemp Distance"+tempDist);
					if(minDist == -1 || tempDist < minDist){
						minDist = tempDist;
						nearestPartitionID = ap.id();
					}						
				}
		    dp.setAllocatedCentroid(nearestPartitionID);
	 
				err+=minDist;
				
				if(cenTable.getPartition(nearestPartitionID) == null){
				  
				  
					Partition<MiniBatch> tmpAp = new Partition<MiniBatch>(nearestPartitionID, dp);
					cenTable.addPartition(tmpAp);
					 
				}else{
				  
				   MiniBatch centroidMB = (MiniBatch)cenTable.getPartition(nearestPartitionID).get();
				   Int2DoubleOpenHashMap centroidHM = centroidMB.getHashMap();
				   
					 for (Int2DoubleMap.Entry entry : aPoint.int2DoubleEntrySet()) {
              int key = entry.getIntKey();
		          double value = entry.getDoubleValue();
		
          		if( centroidHM.containsKey(key))
          		{
          			centroidHM.put(key,centroidHM.get(key)+value);
          		}
          		else
          		{
          			centroidHM.put(key,value);
          		}
            }
            
				}
			  }
			  ////////////////////////////////////////////////////////////////////
			  
			  
			  int newCensize=cenTable.getNumPartitions();
			  int prevCensize=previousCenTable.getNumPartitions();
			  
			  if(newCensize < prevCensize)
			  {
			    for(int cou=0;cou<prevCensize;cou++)
      		{
      		  if(cenTable.getPartition(cou) == null)
      		  {
					    cenTable.addPartition(previousCenTable.getPartition(cou));
      		  }
      		}
			  }
			  
		 System.out.println("Errors: "+err);
	 }
	
	  private void runKmeans(List<String> fileNames, Configuration conf, Context context) throws IOException {
		  // -----------------------------------------------------
		  // Load centroids
		  //for every partition in the centoid table, we will use the last element to store the number of points 
		  // which are clustered to the particular partitionID

		  System.out.println("\n\n\n runKmeans");
		  
		  System.out.println("\n\nIs Master: "+this.isMaster()+"\n\n");

	    //////here
	    Table<MiniBatch> cenTable = new Table(0, new MiniBatchPlus());
		  if (this.isMaster()) {
			  loadCentroids(cenTable,  conf.get(KMeansConstants.CFILE), conf);
		  }

/*
		  int size=cenTable.getNumPartitions();
		
		for(int cou=0;cou<size;cou++)
		{
			MiniBatch centroids = (MiniBatch)cenTable.getPartition(cou).get();
		  int documentId  = centroids.getDocumentId();
		  
		}
	*/	  
		  
		  //broadcast centroids
		  broadcastCentroids(cenTable);
		  
		  //after broadcasting
		
		  HashMap<Integer, Integer> allocatedPointsHM = new HashMap<>();
		  //load data 
		  ArrayList<MiniBatch> dataPoints = loadData(fileNames, conf);
		  //Table<MiniBatch> previousCenTable =  null;
		  previousCenTable =  null;
		  //iterations
		  for(int iter=0; iter < iteration; iter++){
			  previousCenTable =  cenTable;
			  cenTable = new Table<>(0, new MiniBatchPlus());
			 
			  /////////////////////////////////////////////////////////////////////////////
			  
			  int pointsPicked=0;
			  Random random = new Random();
			  int dataPointsSize = dataPoints.size();
			  int pointsLeft = dataPointsSize;
			  ArrayList<MiniBatch> dataPointsBatch= new ArrayList<MiniBatch>();
			  Int2IntOpenHashMap donePoints = new Int2IntOpenHashMap();
			  
			  
			  while(pointsLeft > 0 && pointsPicked < batchSize)
			  {
			    int randNum = random.nextInt(dataPointsSize);
			    //System.out.println("randNum: "+randNum);
			    allocatedPointsHM.put(randNum, 1);
			    dataPointsBatch.add(dataPoints.get(randNum));
  		    pointsPicked++;
  		    pointsLeft--;
			  }
			  
			  
			  
			  //System.out.println("Batch size selected: "+dataPointsBatch.size());
			  
			  if(dataPointsBatch.size()==0)
			  {
			    System.out.println("All points done. No points left for this batch");
			    break;
			  }
			  
			  ////////////////////////////////////////////////////////////////////////////
			  System.out.println("Iteraton No."+iter);
			  
			  //compute new partial centroid table using previousCenTable and data points
		    computation(cenTable,previousCenTable, dataPointsBatch);
			 
		  
			  //****************************************
			  //****************************************
			  //regroup and allgather to synchronized centroids
			  
			  
			  allreduce("main", "allreduce_"+iter, cenTable);
			  
			  //we can calculate new centroids
			  calculateCentroids(cenTable);
			  //printTable(cenTable);
		  }
		  
		  long endTime = System.currentTimeMillis();
		  System.out.println("Start time: "+ kMeansStartTime);
		  System.out.println("End time: "+ endTime);
		  System.out.println("Total K-means Execution Time: "+ (endTime - kMeansStartTime));
		  System.out.println();
		  String output = "\n Total K-means Execution Time: "+ (endTime - kMeansStartTime)+" \n";
		  //output results
		  if(this.isMaster()){
			  output+= " Precision or positive predictive value (PPV) for the overall clusters is "+Calculate_Error( allocatedPointsHM, dataPoints)+"\n\n";
			  outputCentroids(cenTable,  conf,   context,  output);
		  }
		  
		  System.out.println("Done Harp Calculate_Error");
		  //printTable(cenTable);
		  
	 }
	  
	  //output centroids
          private double Calculate_Error(HashMap<Integer, Integer> allocatedPointsHM, ArrayList<MiniBatch> dataTable){
		  HashMap<Integer,HashMap<Integer,Integer>> hm=new HashMap<>();
		  //centroid,label,count
                  
                  
                  for( int point: allocatedPointsHM.keySet()){


              MiniBatch centroids= dataTable.get(point);
                          //MiniBatch centroids = (MiniBatch)ap.get();
						  int centroid=centroids.getAllocatedCentroid();
						  int label=centroids.getDocumentId();
						  
						  if (hm.containsKey(centroid)) {
			            //if centroid exists
					     HashMap<Integer,Integer>  hm1=new HashMap<>();
			        	hm1=hm.get(centroid);
						
			        	if(hm1.containsKey(label))
			        	{
						// check if label exists
					   int count=hm1.get(label)+1;
					   hm1.put(label,count);
					   hm.put(centroid,hm1);
					   }
						else
						{
						// check if label does not exist
						hm1.put(label,1);
			            //key does not exists
			            hm.put(centroid,hm1);
						}
			        } else
			        {//centroid does not exist, add the label with the count 1
			        	    	
			        		HashMap<Integer,Integer>  hm2=new HashMap<>();
							hm2.put(label,1);
			            //key does not exists
			            hm.put(centroid,hm2);
			        	}
			        }
					int no_of_centroids=hm.size();
					//HashMap<Integer,HashMap<Integer,ArrayList<Integer>>> hm3=new HashMap<>();
					HashMap<Integer,ArrayList<Integer>> hm3=new HashMap<>();
					double ppv_total=0.0;
					int overall_positive=0,overall_total=0;
					for(Integer key:hm.keySet())
						{
						  HashMap<Integer,Integer> hm4=new HashMap<>();
						  hm4=hm.get(key);
						  int label=-1,total_count=0,max=0,false_positive=0;
						  for(Integer key1:hm4.keySet())
							{
								total_count=total_count+hm4.get(key1);
								if(hm4.get(key1)>max)
								{
								max=hm4.get(key1);
								label=key1;
								}
								//System.out.println("Total count of records  for the class "+ key1 +" is "+ hm4.get(key1));
								
							}
							false_positive=total_count-max;
							overall_positive=overall_positive+max;
							overall_total=overall_total+total_count;
							System.out.println("Total count of false positive records  for the cluster "+ key +" is "+ false_positive);
							System.out.println("Total count of true positive records  for the dominant CLASS "+ label +" is "+ max);
							System.out.println("Total count of records  for the cluster "+ key +" is "+ total_count);
							double ppv=(double)max/total_count;
							System.out.println("precision or positive predictive value (PPV) for the cluster "+ key +" is "+ppv+ " and the dominant CLASS is "+ label+"\n\n\n\n");
							//ppv_total=ppv_total+ppv;
							ArrayList<Integer> al=new ArrayList<>();
							al.add(label);
							al.add(total_count);
							al.add(false_positive);
							//HashMap<Integer,ArrayList<Integer>> hm5=new HashMap<>();
							//hm5.add(label,al);
							hm3.put(key,al);
							
						}
						ppv_total=(float)overall_positive/overall_total;
						System.out.println("Precision or positive predictive value (PPV) for the overall clusters is "+ (ppv_total)+"\n\n\n\n\n");
					      return ppv_total;
          }
	  
	   //output centroids
	  private void outputCentroids(Table<MiniBatch>  cenTable,Configuration conf, Context context, String str){
		  String output=str;
		  
		  for( Partition<MiniBatch> ap: cenTable.getPartitions()){
			  MiniBatch miniBatchObj = ap.get();
			  output += miniBatchObj.getDocumentId()+" ";
			  output += miniBatchObj.getAllocatedCentroid()+" ";
			  
			  for (Int2DoubleMap.Entry entry : miniBatchObj.getHashMap().int2DoubleEntrySet()) {
		    
		    output += entry.getIntKey()+":"+entry.getDoubleValue()+" ";
		    //System.out.print(" "+entry.getIntKey() +":"+ entry.getDoubleValue()+" ");
		    
      }
			  output+="\n";
		  }
		  
			try {
				context.write(null, new Text(output));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	  }
	  
	  
	  private void calculateCentroids( Table<MiniBatch> cenTable){
	    //System.out.println("Is Master: "+this.isMaster());
		  for( Partition<MiniBatch> partialCenTable: cenTable.getPartitions()){
			  MiniBatch centroid  = partialCenTable.get();
			  Int2DoubleOpenHashMap centroidHM = centroid.getHashMap();
			  
			  double size = centroidHM.get(-100);
			  for (Int2DoubleMap.Entry entry : centroidHM.int2DoubleEntrySet()) {
		    
		    entry.setValue(entry.getDoubleValue()/size);
		    
      }
			centroidHM.put(-100,1);
		  }
		  
	  }
	  
	  
	  //calculate Euclidean distance.
	  private double calcEucDistSquare(Int2DoubleOpenHashMap aPoint, Int2DoubleOpenHashMap otherPoint){
		  
		  double dist=0;
		  Int2DoubleOpenHashMap tempOtherPoint = new Int2DoubleOpenHashMap(otherPoint);
		  for (Int2DoubleMap.Entry entry : aPoint.int2DoubleEntrySet()) {
		    
		    
		    int key = entry.getIntKey();
		    if( tempOtherPoint.containsKey(key))
    		{
    			dist += Math.pow(entry.getDoubleValue()-tempOtherPoint.get(key),2);
    			tempOtherPoint.remove(key);
    		}
    		else
    		{
    		  dist += Math.pow(entry.getDoubleValue(),2);
    		}
		    
      }
		  
		  for (Int2DoubleMap.Entry entry : tempOtherPoint.int2DoubleEntrySet()) {
		    
    		dist += Math.pow(entry.getDoubleValue(),2);
    		
      }
		  //System.out.println("\nDistance: "+dist);
		  
		  
		  return Math.sqrt(dist);
		  
	  }
	  
	  
	 //load centroids from HDFS
	 private void loadCentroids( Table cenTable,   String cFileName, Configuration configuration) throws IOException{
	   //System.out.print("---------------------------------------------Loading Centroids------------------------------");
		 Path cPath = new Path(cFileName);
		 FileSystem fs = FileSystem.get(configuration);
		 FSDataInputStream in = fs.open(cPath);
		 BufferedReader br = new BufferedReader( new InputStreamReader(in));
		 String line="";
		 String[] vector=null;
		 int partitionId=0;
		 while((line = br.readLine()) != null){
			 vector = line.split("\\s+");
			 
				  //double[] aCen = new double[vectorSize+1];
				  MiniBatch obj=new MiniBatch();
				  obj.setDocumentId(Integer.parseInt(vector[0]));
				  obj.setAllocatedCentroid(partitionId);
		      
		      //System.out.println("\n DocumentId: "+vector[0] + "\n PartitionId: "+partitionId);
		      
				  for(int i=1; i<vector.length; i++){
				    String[] keyValuePair= vector[i].split(":");
				    int key = Integer.parseInt(keyValuePair[0]);
				    double value = Double.parseDouble(keyValuePair[1]);
				    
				    
				    obj.putKeyValue(key,value);
					  //System.out.print(" Key: "+keyValuePair[0] + " Value: "+keyValuePair[1]);
					  
					  //aCen[i] = Double.parseDouble(vector[i]);
				  }
				  obj.putKeyValue(-100,1);
				  //aCen[vectorSize]=0;
				  Partition<MiniBatch> ap = new Partition<MiniBatch>(partitionId, obj);
				  cenTable.addPartition(ap);
				  partitionId++;
			  
		 }
	 }
	 
	 
	  //load data form HDFS
	  private ArrayList<MiniBatch>  loadData(List<String> fileNames, Configuration conf) throws IOException{
	    //System.out.print("---------------------------------------------Loading Data------------------------------");
		  ArrayList<MiniBatch> data = new  ArrayList<MiniBatch> ();
		  for(String filename: fileNames){
			  FileSystem fs = FileSystem.get(conf);
			  Path dPath = new Path(filename);
			  FSDataInputStream in = fs.open(dPath);
			  BufferedReader br = new BufferedReader( new InputStreamReader(in));
			  String line="";
			  String[] vector=null;
			  while((line = br.readLine()) != null){
				  vector = line.split("\\s+");
				  MiniBatch obj=new MiniBatch();
				  obj.setDocumentId(Integer.parseInt(vector[0]));
				  obj.setAllocatedCentroid(-1);
				  obj.putKeyValue(-100,1);
				  //System.out.println("\n DocumentId: "+vector[0] + "\n AllocatedCentroid: -1");
				  for(int i=1; i<vector.length; i++){
				    String[] keyValuePair= vector[i].split(":");
				    int key = Integer.parseInt(keyValuePair[0]);
				    double value = Double.parseDouble(keyValuePair[1]);
				    
				    
				    obj.putKeyValue(key,value);
					  //System.out.print(" Key: "+keyValuePair[0] + " Value: "+keyValuePair[1]);
					  
					  //aCen[i] = Double.parseDouble(vector[i]);
				  }
				  
				 data.add(obj);
				  
			  }
		  }
		  return data;
	  }
	  
	  
	  //for testing
	  private void printTable(Table<MiniBatch> dataTable){
		  for( Partition<MiniBatch> ap: dataTable.getPartitions()){
			  
			  MiniBatch centroids = (MiniBatch)ap.get();
			  System.out.print(" DocumentId: "+centroids.getDocumentId() + " Allocated Centroid: "+centroids.getAllocatedCentroid());
			  Int2DoubleOpenHashMap featureValueMap= centroids.getHashMap();
			  
			  
			  for (Int2DoubleMap.Entry entry : featureValueMap.int2DoubleEntrySet()) {
        System.out.print(" "+entry.getIntKey() +":"+ entry.getDoubleValue()+" ");
        
      }
			  System.out.println();
			  
		  }
	  }
	  
}