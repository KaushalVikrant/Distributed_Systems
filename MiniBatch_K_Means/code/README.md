# Group23-MiniBatch-K-Means
MiniBatch-K Means
Team members:- Vikrant Kaushal(vkaushal) Shalabh(sshalabh)

Here is the sample command

hadoop jar harp3-app-hadoop-2.6.0.jar edu.iu.termproject.KmeansMapCollective 5 300 10 2 /kmeansTermProject /tmp/termproject/ /tmp/termproject/Data

Parameters in the command as args:

number of centroids<br />
batchSize<br />
Number of Iterations<br />
Number of Mappers<br />
Working directory for HDFS<br />
Local Directory from where program will pick Data (Please create same structure and add data file /tmp/termproject/)<br />
Data Directory here program will place files after splitting and put these files to hdfs(Please create same structure /tmp/termproject/Data)<br />
Method Type<br />
            {<br />
            regroup-allgather       ( For running program using multi threading)<br />
            allreduce               ( For running program using multi threading)<br />
            regroup-allgatherSeq    (For running program without multi-threading) You can use this command with one Mapper for sequential simulation<br />
            allreduceSeq            (For running program without multi-threading) You can use this command with one Mapper for sequential simulation<br />
            }<br />
