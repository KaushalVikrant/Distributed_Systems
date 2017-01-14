package edu.iu.km;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapred.CollectiveMapper;


public class KmeansMapper  extends CollectiveMapper<String, String, Object, Object> {

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		Configuration configuration = context.getConfiguration();
		/*
		 * initialization
		 */
	}

	protected void mapCollective( KeyValReader reader, Context context) throws IOException, InterruptedException {
		/*
		 * vals in the keyval pairs from reader are data file paths.
		 * read data from file paths.
		 * load initial centroids
		 * do{
		 * 	computations
		 *  generate new centroids
		 * }while(<maxIteration)
		 * 
		 */	
	}
}