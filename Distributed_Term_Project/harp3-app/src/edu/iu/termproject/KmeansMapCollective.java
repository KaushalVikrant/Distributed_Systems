package edu.iu.termproject;

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
import edu.iu.fileformat.MultiFileInputFormat;
//import edu.iu.kmexample.bcastreduce.KmeansMapper;

public class KmeansMapCollective  extends Configured implements Tool {

	public static void main(String[] argv) throws Exception {
		int res = ToolRunner.run(new Configuration(), new KmeansMapCollective(), argv);
		System.exit(res);
	}

	@Override
	public int run(String[] args) throws Exception {
		if (args.length < 3) {
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
		String operation = "";
		if(args.length == 8)
		{
		  operation = args[7];
		}

		System.out.println( "No of initial centroids = "	+ numCentroids);
		System.out.println("Iterations : " + numIterations);
		System.out.println("Args=:");
		for(String arg: args){
			System.out.print(arg+";");
		}
		System.out.println();
		
		launch(numCentroids,batchSize, numIterations,numMapTasks , workDir, localDir,localDirData, operation);
		System.out.println("HarpKmeans Completed"); 
		return 0;
	}
	
	void launch(int numCentroids, int batchSize , int numIterations,  int numMapTasks, String workDir, String localDir,String localDirDataStr,String operation)
			throws IOException, URISyntaxException, InterruptedException, ExecutionException, ClassNotFoundException {
		
		Configuration configuration = getConf();
		Path workDirPath = new Path(workDir);
		FileSystem fs = FileSystem.get(configuration);
		Path dataDir = new Path(workDirPath, "data");
		Path cenDir = new Path(workDirPath, "centroids");
		Path outDir = new Path(workDirPath, "outTermProject");
		if (fs.exists(outDir)) {
			fs.delete(outDir, true);
		}
		fs.mkdirs(outDir);
		
		
		int numOfDataPoints=Utils.generateData(numMapTasks, fs, localDir, localDirDataStr,dataDir);
		
		int JobID = 0;
		Utils.generateInitialCentroids(numCentroids, localDir,configuration, cenDir, fs, JobID);
		
		long startTime = System.currentTimeMillis();
		System.out.println("Start time: "+startTime);
		runKMeans(numOfDataPoints, batchSize , numCentroids,  numIterations,
				JobID,  numMapTasks, configuration, workDirPath,
				dataDir, cenDir, outDir, operation,startTime);
				
		long endTime = System.currentTimeMillis();
		System.out.println("Total K-means Execution including evaluation Time: "+ (endTime - startTime));
	}
	
	
	private void runKMeans(int numOfDataPoints, int batchSize, int numCentroids, int numIterations, 
				int JobID, int numMapTasks, Configuration configuration, 
			Path workDirPath, Path dataDir, Path cDir, Path outDir, String operation, long startTime)
			throws IOException,URISyntaxException, InterruptedException,ClassNotFoundException {
			
		System.out.println("Starting Job");
		long jobSubmitTime;
		boolean jobSuccess = true;
		int jobRetryCount = 0;
		
		do {
			// ----------------------------------------------------------------------
			jobSubmitTime = System.currentTimeMillis();
			System.out.println("Start Job#" + JobID + " "+ new SimpleDateFormat("HH:mm:ss.SSS").format(Calendar.getInstance().getTime()));
			
			Job kmeansJob = configureKMeansJob(numOfDataPoints,	batchSize ,numCentroids, numMapTasks,
					configuration, workDirPath, dataDir,cDir, outDir, JobID, numIterations, operation, startTime);
			
			System.out.println("| Job#"+ JobID+ " configure in "+ (System.currentTimeMillis() - jobSubmitTime)+ " miliseconds |");
			
			// ----------------------------------------------------------
			jobSuccess =kmeansJob.waitForCompletion(true);
			
			System.out.println("end Jod#" + JobID + " "
						+ new SimpleDateFormat("HH:mm:ss.SSS")
						.format(Calendar.getInstance().getTime()));
			System.out.println("| Job#"+ JobID + " Finished in "
					+ (System.currentTimeMillis() - jobSubmitTime)
					+ " miliseconds |");
		
			// ---------------------------------------------------------
			if (!jobSuccess) {
				System.out.println("KMeans Job failed. Job ID:"+ JobID);
				jobRetryCount++;
				if (jobRetryCount == 3) {
					break;
				}
			}else{
				break;
			}
		} while (true);
	}
	
	private Job configureKMeansJob(int numOfDataPoints, int batchSize, int numCentroids, 
			int numMapTasks, Configuration configuration,Path workDirPath, Path dataDir, Path cDir,
			Path outDir, int jobID, int numIterations, String operation, long startTime) throws IOException, URISyntaxException {
			
		Job job = Job.getInstance(configuration, "kmeans_job_"+ jobID);
		Configuration jobConfig = job.getConfiguration();
		Path jobOutDir = new Path(outDir, "kmeans_out_" + jobID);
		FileSystem fs = FileSystem.get(configuration);
		if (fs.exists(jobOutDir)) {
			fs.delete(jobOutDir, true);
		}
		FileInputFormat.setInputPaths(job, dataDir);
		FileOutputFormat.setOutputPath(job, jobOutDir);

		Path cFile = new Path(cDir,KMeansConstants.CENTROID_FILE_PREFIX + jobID);
		System.out.println("Centroid File Path: "+ cFile.toString());
		jobConfig.set(KMeansConstants.CFILE,cFile.toString());
		jobConfig.setInt(KMeansConstants.JOB_ID, jobID);
		jobConfig.setInt(KMeansConstants.NUM_ITERATONS, numIterations);
		jobConfig.setInt(KMeansConstants.BATCH_SIZE, batchSize);
		//jobConfig.setInt(KMeansConstants.MAP_TASKS, numMapTasks);
		job.setInputFormatClass(MultiFileInputFormat.class);
		job.setJarByClass(KmeansMapCollective.class);
		
		
		//use different kinds of mappers
		//job.setMapperClass(edu.iu.termproject.regroupallgather.KmeansMapper.class);
		//job.setMapperClass(edu.iu.termproject.allreduce.KmeansMapper.class);

    

    if(operation.equalsIgnoreCase("allreduce")){
			job.setMapperClass(edu.iu.termproject.allreduce.KmeansMapper.class);
		}else if (operation.equalsIgnoreCase("regroup-allgather")){
			job.setMapperClass(edu.iu.termproject.regroupallgather.KmeansMapper.class);
		}else if (operation.equalsIgnoreCase("allreduceSeq")){
			job.setMapperClass(edu.iu.termproject.allreduceSeq.KmeansMapper.class);
		}else if (operation.equalsIgnoreCase("regroup-allgatherSeq")){
			job.setMapperClass(edu.iu.termproject.regroupallgatherSeq.KmeansMapper.class);
		}else{//by default, 
			job.setMapperClass(edu.iu.termproject.regroupallgather.KmeansMapper.class);
		}


    if(operation.equals(""))
    {
      operation="regroup-allgather";
    }
    System.out.println("Operation: "+operation);
		
		org.apache.hadoop.mapred.JobConf jobConf = (JobConf) job.getConfiguration();
		jobConf.set("mapreduce.framework.name", "map-collective");
		jobConf.setNumMapTasks(numMapTasks);
		jobConf.setInt("mapreduce.job.max.split.locations", 10000);
		job.setNumReduceTasks(0);
		//jobConfig.setInt(KMeansConstants.VECTOR_SIZE,vectorSize);
		jobConfig.setInt(KMeansConstants.NUM_CENTROIDS, numCentroids);
		jobConfig.set(KMeansConstants.WORK_DIR,workDirPath.toString());
		jobConfig.setInt(KMeansConstants.NUM_MAPPERS, numMapTasks);
		jobConfig.setLong(KMeansConstants.START_TIME, startTime);
		return job;
	}
	


}
