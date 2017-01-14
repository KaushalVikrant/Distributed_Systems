/*
 * Copyright 2013-2016 Indiana University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.iu.termproject.minibatch;


import edu.iu.termproject.KMeansConstants;
import edu.iu.harp.partition.PartitionCombiner;
import edu.iu.harp.partition.PartitionStatus;
import edu.iu.termproject.minibatch.MiniBatch;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

public class MiniBatchPlus extends
  PartitionCombiner<MiniBatch> {

  @Override
  public PartitionStatus combine(
    MiniBatch curPar, MiniBatch newPar) {
      
      //System.out.println("Inside Partition Status ");
    
	Int2DoubleOpenHashMap curHM = curPar.getHashMap();
	Int2DoubleOpenHashMap newHM = newPar.getHashMap();
	
	for (Int2DoubleMap.Entry entry : newHM.int2DoubleEntrySet()) {
        int key = entry.getIntKey();
		double value = entry.getDoubleValue();
		
		if( curHM.containsKey(key))
		{
			curHM.put(key,curHM.get(key)+value);
		}
		else
		{
			curHM.put(key,value);
		}
      }
    return PartitionStatus.COMBINED;
  }


}
