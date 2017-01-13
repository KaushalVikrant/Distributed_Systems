package edu.iu.km;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import edu.iu.fileformat.MultiFileInputFormat;

public class KmeansMapCollective  extends Configured implements Tool {

	public static void main(String[] argv) throws Exception {
		int res = ToolRunner.run(new Configuration(), new KmeansMapCollective(), argv);
		System.exit(res);
	}

	
	@Override
	public int run(String[] args) throws Exception {
		//keep this unchanged.
		if (args.length < 6) {
			System.err.println("Usage: KmeansMapCollective <numOfDataPoints> <num of Centroids> "
					+ "<size of vector> <number of map tasks> <number of iteration> <workDir> <localDir>");			
			ToolRunner.printGenericCommandUsage(System.err);
				return -1;
		}
		
		/*
		 * Parse arguments
		 * Generate data randomly
		 * Generate initial centroids
		 * Configure jobs 
		 *   **for inputFormatClass: use job.setInputFormatClass(MultiFileInputFormat.class);
		 * Launch jobs
		 */
		
		return 0;
	}
}
