package edu.iu.rnd;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import edu.iu.harp.keyval.Int2LongKVTable;
import edu.iu.harp.keyval.Int2LongKVPartition;
import edu.iu.harp.keyval.Long2DoubleKVPartition;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;

import java.util.*;

//import edu.iu.kmexample.bcastreduce.KmeansMapper;

public class KmeansMapCollective  extends Configured implements Tool {

	public static void main(String[] argv) throws Exception {
		int res = ToolRunner.run(new Configuration(), new KmeansMapCollective(), argv);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 0) {
			System.err.println("Usage: KmeansMapCollective <numOfDataPoints> <num of Centroids> "
					+ "<size of vector> <number of map tasks> <number of iteration> <workDir> <localDir> <communication operation>\n"
					+ "<communication operation> includes:\n  "
				 	+  "[allreduce]: use allreduce operation to synchronize centroids \n"
					+  "[regroup-allgather]: use regroup and allgather operation to synchronize centroids \n"
					+  "[broadcast-reduce]: use broadcast and reduce operation to synchronize centroids \n"
					+  "[push-pull]: use push and pull operation to synchronize centroids\n");			
			ToolRunner.printGenericCommandUsage(System.err);
				return -1;
		}
		
		int numCentroids = Integer.parseInt(args[0]);
		int batchSize = Integer.parseInt(args[1]);
		int numIterations = Integer.parseInt(args[2]);	
		
		int numMapTasks = Integer.parseInt(args[3]);
		//int numIteration = Integer.parseInt(args[4]);
		String workDir = args[4];
		String localDir = args[5];
		String localDirData = args[6];
		//String operation = args[7];

		System.out.println( "No of initial centroids = "	+ numCentroids);
		System.out.println("Iterations : " + numIterations);
		System.out.println("Args=:");
		for(String arg: args){
			System.out.print(arg+";");
		}
		System.out.println();
		
		Long2DoubleKVPartition obj=new Long2DoubleKVPartition();
		obj.initialize();
		
		obj.putKeyVal(1,2.45,null);
		obj.putKeyVal(2,3.009,null);
		
		System.out.print("Value of 1: "+obj.getVal(1));
		System.out.print("Value of 2: "+obj.getVal(2));
		Long2DoubleOpenHashMap hm=obj.getKVMap();
		
		//Iterator it = obj.entrySet().iterator();
		
		for (Long key : hm.keySet()) {
    System.out.print("Value of "+key+": "+obj.getVal(key));
}
		
		//launch(numCentroids,batchSize, numIterations,numMapTasks , workDir, localDir,localDirData);
		System.out.println("HarpKmeans Completed"); 
		return 0;
	}
	
	
	


}
