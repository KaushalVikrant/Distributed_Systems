package edu.iu.termproject.minibatch;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import edu.iu.harp.resource.Writable;


public class MiniBatch extends Writable {

  private int documentId;
  private int allocatedCentroid;  
  private Int2DoubleOpenHashMap featureValueMap;

  public MiniBatch() {
	documentId=-1;
    allocatedCentroid = -1;
    featureValueMap = new Int2DoubleOpenHashMap();
  }

public MiniBatch(MiniBatch obj) {
	this.documentId=obj.documentId;
	this.allocatedCentroid=obj.allocatedCentroid;
    
    this.featureValueMap = new Int2DoubleOpenHashMap(obj.featureValueMap);
  }

  public void setDocumentId(int docId) {
    this.documentId = docId;
  }
  public void setAllocatedCentroid(int allocCentroid) {
    this.allocatedCentroid = allocCentroid;
  }
  
  public void putKeyValue(int key, double value) {
    this.featureValueMap.put(key,value);
  }

  public Int2DoubleOpenHashMap getHashMap() {
    return featureValueMap;
  }
  
  public int getDocumentId() {
    return documentId;
  }
  public int getAllocatedCentroid() {
    return allocatedCentroid;
  }

  @Override
  public int getNumWriteBytes() {
    int size = 12;
    
    if (featureValueMap != null) {
      size += (featureValueMap.size() * 12);
    }
    return size;
  }

  @Override
  public void write(DataOutput out)
    throws IOException {
	out.writeInt(documentId);
	out.writeInt(allocatedCentroid);
    if (featureValueMap != null) {
      out.writeInt(featureValueMap.size());
      for (Int2DoubleMap.Entry entry : this.featureValueMap
        .int2DoubleEntrySet()) {
        out.writeInt(entry.getIntKey());
        out.writeDouble(entry.getDoubleValue());
      }
    } else {
      out.writeInt(0);
    }
    
  }

  @Override
  public void read(DataInput in)
    throws IOException {
    documentId = in.readInt();
	allocatedCentroid = in.readInt();
	int mapSize = in.readInt();
    if (mapSize > 0) {
      if (featureValueMap == null) {
        featureValueMap =
          new Int2DoubleOpenHashMap(mapSize);
      }
    }
    for (int i = 0; i < mapSize; i++) {
      featureValueMap.put(in.readInt(),
        in.readDouble());
        }
  }

  @Override
  public void clear() {
	documentId=-1;
    allocatedCentroid = -1;
    if (featureValueMap != null) {
      featureValueMap.clear();
    }
  }


}
