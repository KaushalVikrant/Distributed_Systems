package edu.iu.termproject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.util.*;

public class Utils {
	private final static int DATA_RANGE = 10;
	
	public static int generateData(  int numMapTasks, 
			FileSystem fs,  String localDirStr,String localDirDataStr,Path dataDir) throws IOException, InterruptedException, ExecutionException {
    
    int numOfDataPoints=0;
	  File folder = new File(localDirStr);
	  File[] listOfFiles = folder.listFiles();
	  ArrayList<String> fileName=new ArrayList<>();
	  for (int i = 0; i < listOfFiles.length; i++) 
		{
			if (listOfFiles[i].isFile()&& listOfFiles[i].getName().endsWith(".dat")) 
			{
				fileName.add(listOfFiles[i].getName());
			}
		}
		
		
		for (int i = 0; i < fileName.size(); i++) 
		{
			//System.out.println(fileName.get(i));
			try{
				String Path=localDirStr+"//"+fileName.get(i);
				//System.out.println(Path);
				LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(Path)));
				lnr.skip(Long.MAX_VALUE);
				numOfDataPoints=numOfDataPoints+lnr.getLineNumber() + 1; //Add 1 because line index starts at 0
				// Finally, the LineNumberReader object should be closed to prevent resource leak
				lnr.close();
				}
						catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		System.out.println("Total points: "+numOfDataPoints);
		
		
		
		int numOfpointFiles = numMapTasks;
		int pointsPerFile = numOfDataPoints / numOfpointFiles;
		int pointsRemainder = numOfDataPoints % numOfpointFiles;
	    System.out.println("Writing " + numOfDataPoints + " vectors to "+ numMapTasks +" file evenly");
	    
	    // Check data directory
	    if (fs.exists(dataDir)) {
	    	fs.delete(dataDir, true);
	    }
	    // Check local directory
	    File localDir = new File(localDirStr);
		
	    
	    
	    double point;
	    int hasRemainder=0;
	    Random random = new Random();
	    String line=null;
	    String Path="";
	    int fileCounter=0;
	    
      if(fileName.size()>0)
      {
        
  			FileReader dataPointReader=new FileReader(localDirStr+"//"+fileName.get(0)); 
  	    BufferedReader bufferedReader= new BufferedReader(dataPointReader);
  	    
        for (int k = 0; k < numOfpointFiles; k++) 
        {
          //System.out.println("value of k: "+ k);
            try {
                String filename =Integer.toString(k);
                File file = new File( localDirDataStr + File.separator + "data_" + filename);
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);

                if(pointsRemainder > 0){
                    hasRemainder = 1;
                    pointsRemainder--;
                }else{
                    hasRemainder = 0;
                }
                int pointsForThisFile =  pointsPerFile + hasRemainder;
                for (int i = 0; i < pointsForThisFile; ) {
                    //System.out.println("value of i: "+ i);
                    if(line==null)
                    {
                    	if(fileName.size()>fileCounter)
                    	{
	                        Path=localDirStr+"//"+fileName.get(fileCounter);                            
	                        dataPointReader =  new FileReader(Path);
	                        bufferedReader = new BufferedReader(dataPointReader);
	                        fileCounter++;
                    	}
                    	 else
                        	i++;
                    }
                    
                    while( (line = bufferedReader.readLine()) != null)
                    {  
                        bw.write(line);
                        bw.newLine();
                        i++;
                        //System.out.println("value of line: "+ line);
                        if(i>=pointsForThisFile)
                        {
                            break;
                        }
                        
                      
                    }    
                    }
                bw.close();
                System.out.println("Done written"+ pointsForThisFile + "points" +"to file "+ filename);
                }catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
              }
              
          } 
        
	    }
	
	    Path localPath = new Path(localDirDataStr);
	    fs.copyFromLocalFile(localPath, dataDir);
	    return numOfDataPoints;
	  }
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	 public static void generateInitialCentroids(int numCentroids, String inputDir,Configuration configuration,
			  Path cDir, FileSystem fs, int JobID) throws IOException {
	    Random random = new Random();
	    double[] data = null;
	    if (fs.exists(cDir))
	    	fs.delete(cDir, true);
	    if (!fs.mkdirs(cDir)) {
	    	throw new IOException("Mkdirs failed to create " + cDir.toString());
	    }
	    
	    
	    
	    int size=0;
	/*	
		try{
			LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(Path)));
			lnr.skip(Long.MAX_VALUE);
			 size=lnr.getLineNumber() + 1; //Add 1 because line index starts at 0
			// Finally, the LineNumberReader object should be closed to prevent resource leak
			lnr.close();
			}
					catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
			
			File folder = new File(inputDir);
      File[] listOfFiles = folder.listFiles();
      ArrayList<String> fileName=new ArrayList<>();
      for (int i = 0; i < listOfFiles.length; i++) 
        {
            if (listOfFiles[i].isFile()&& listOfFiles[i].getName().endsWith(".dat")) 
            {
                fileName.add(listOfFiles[i].getName());
            }
        }
        
        //int numOfDataPoints=0;
        for (int i = 0; i < fileName.size(); i++) 
        {
            //System.out.println(fileName.get(i));
            try{
                String Path=inputDir+"//"+fileName.get(i);
                //System.out.println(Path);
                LineNumberReader  lnr = new LineNumberReader(new FileReader(new File(Path)));
                lnr.skip(Long.MAX_VALUE);
                size=size+lnr.getLineNumber()+1; //Add 1 because line index starts at 0
                // Finally, the LineNumberReader object should be closed to prevent resource leak
                lnr.close();
                }
                        catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
        }
        System.out.println("Total points: "+size);
	    
		ArrayList<Integer> duplicateRand=new ArrayList<>();
		int numberOfClusters=numCentroids;
		if(size<numberOfClusters)
		{
			System.err.println("invalid cluster size ! Please Reduce cluster size");
			return ;
		}
		
		for(int i=0;i<numberOfClusters;i++)
		{
			int randNum=random.nextInt(size)+1;
			//to ensure that a random number is not generated twice and hence the 
			//same record is not assigned to the cluster
			while(duplicateRand.contains(randNum))
			{
				randNum=random.nextInt(size)+1;
			}
			duplicateRand.add(randNum);
			
		}
		
		ArrayList<Integer> cluster=new ArrayList<>();
      
      cluster=duplicateRand;
      Collections.sort(cluster);
      //System.out.println(cluster);
		

			
			Path initClustersFile = new Path(cDir, KMeansConstants.CENTROID_FILE_PREFIX+JobID);
		    //System.out.println("Generated centroid data." + initClustersFile.toString());
		    
		    FSDataOutputStream out = fs.create(initClustersFile, true);
		    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
		        	  int counter=0;
		        	  
			/////////////////////
			for (int i = 0; i < fileName.size(); i++) 
            {
                //System.out.println(fileName.get(i));
                try{
                    String Path=inputDir+"//"+fileName.get(i);
                    String line=null;
                    FileReader dataPointReader=new FileReader(Path); 
                    BufferedReader bufferedReader= new BufferedReader(dataPointReader);
                    while( (line = bufferedReader.readLine()) != null)
                    {  
                        counter++;
                      //System.out.println("counter:"+counter);
                        if(cluster.size()==0)
                        {
                        	i=fileName.size()+2;
                        	break;
                        }
                        else
                        {
                        	if(cluster.contains(counter))
                        	{
                        		//System.out.println("counter:"+counter);
                        		//System.out.println(line);
                        		bw.write(line);
	    		                  bw.newLine();
                        		int index=cluster.indexOf(counter);
                        		cluster.remove(index);
                        	}
                        }
                        
                        
                      
                    }
                    
                    bufferedReader.close();
                    
                    }
                            catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
            }
            ////////////////////////
			
			
			
			
			
		/*	
			for(int j = 0; j< duplicateRand.size(); ++j)
			{
			  System.out.println(duplicateRand.get(j));
				FileReader dataPointReader =  new FileReader(Path);
				 BufferedReader br = new BufferedReader(dataPointReader);
				 String lineIWant=null;
				 for(int i = 1; i <= duplicateRand.get(j); i++)
				 {
				lineIWant = br.readLine();
				bw.write(lineIWant + "");
	    		bw.newLine();
				 }
				 System.out.println(lineIWant+"\n\n");
				 br.close();
			}
			
		}
		catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	    bw.flush();
	    bw.close();
	    // out.flush();
	    // out.sync();
	    // out.close();
	    System.out.println("Wrote centroids data to file");
	  }
	  
	
}
