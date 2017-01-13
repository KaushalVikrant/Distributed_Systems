package edu.iu.km;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

/*
 * generate data and initial centroids
 */
public class Utils { 

	static void generateData( int numOfDataPoints, int vectorSize, int numMapTasks, 
			FileSystem fs,  String localDirStr, Path dataDir) throws IOException, InterruptedException, ExecutionException {
	
	}
	
	
	
	 static void generateInitialCentroids(int numCentroids, int vectorSize, Configuration configuration,
			  Path cDir, FileSystem fs, int JobID) throws IOException {
		 
	 }
}
