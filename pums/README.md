#diqube Data Examples - PUMS#

This example loads data of the [American Community Survey (ACS)][1] [Public Use Microdata Sample (PUMS)][2], the 5 year#
one containing data from 2005-2009. This data is provided by the [US Census Bureau][4].

The 2005-2009 data set was chosen because the 5 year old data set contains more data than 3 or 1 year data sets, but
the newest 5 year dataset (2009-2013) contains a change in how the data is split geographically somewhere in that period -
some of the data is available in an older format, some in a newer - which would have needed more effort for preparing
the data.   

##Download the raw data##

The raw data is provided as CSV files here: https://www.census.gov/programs-surveys/acs/data/pums.html. Download all the
CSV files for both "person" and "housing" records for the "2005-2009 5 year PUMS" containing data about all the United
States.

You should end up having following files:

* Housing records
  * ss09husa.csv
  * ss09husb.csv
  * ss09husc.csv
  * ss09husd.csv
* Person records
  * ss09pusa.csv
  * ss09pusb.csv
  * ss09pusc.csv
  * ss09pusd.csv
  
This data totals to approx 10955 MB.

For more information on PUMS and its data can be found on the [Technical Documentation][3] site of the US Census Bureau.

##Prepare the data using Hadoop##

In [diqube-pums-hadoop](diqube-pums-hadoop/) is an example Map/Reduce job for [Apache Hadoop][5] which reads the CSV 
file data, combines and adjusts the data to be easier readable and then provides a .diqube file which can be easily
imported into diqube-server (the resulting file(s) are just like the ones created by diqube-transpose).

###Adjustments done by Map/Reduce job###

The Map/Reduce job "adjusts" the data of the CSV files. It (1) renames the columns to be more easily readable and (2)
also maps the values of the columns to be more easily readable.

Additionally it combines the housing records and the person records: Each housing record gets a "persons" field which
contains a list of person records that belong to the same house. Each row in the resulting table will then contain one
"housing" record.

###Step-by-step###

1. Download CSV files
2. Remove first line of each CSV file, as it contains the "CSV header" and the Map/Reduce job cannot dynamically handle
   this. Do this on Linux for example using `tail -n +2 ss09husa.csv > ss09husa_new.csv`
3. Build the uber jar of the example
4. Start Hadoop: `bin/hadoop jar path/to/diqube-pums-hadoop.jar org.diqube.hadoop.PumsDiqube ss09hus*.csv ss09pus*.csv /path/to/output`

###Pitfalls###

* You might want to adjust the Hadoop job and specify the number of reducers used: Each reducer produces one output file
  and if you want to serve the data in a cluster, each diqube-server needs a dedicated data file.
* The Hadoop job needs quite some amount of memory. Be prepared to increase that amount to quite a few GB.
* Tested with Apache Hadoop 2.7.1 (and 2.5.1 which is bundled with MapR community edition), it may also work with other Hadoop versions.
* Be aware that diqube needs Java 8, Hadoop usually runs with Java 7. I though had no problems running Hadoop with Java 8.

##Execute on Amazon WebServices##

I did execute this Map/Reduce successfully on AWS EC2 using the MapR community edition. I took the following steps:

* Create an AWS account, setup a key pair for N. Virginia.
* Accept license agreement of MapR Community edition: https://aws.amazon.com/marketplace/pp/B010GJS5WO
* Install a cluster of MapR Community edition. Start this using the form available here: https://www.mapr.com/how-launch-mapr-aws-marketplace
  * Use the following parameters for your cluster (install in N. Virginia or same cluster that has the key pair).
    * ClusterInstanceCount = 4
    * InstanceType = i2.xlarge
    * KeyName = name of your key pair
    * MapREdition = M3
  * Wait for your cluster to be fully available.
* Login to each node of the cluster and execute the following commands:
    ```
    wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" "http://download.oracle.com/otn-pub/java/jdk/8u60-b27/jdk-8u60-linux-x64.tar.gz"
    tar xf jdk-8u60-linux-x64.tar.gz
    sudo mv jdk1.8.0_60/ /opt
    sudo chmod -R 0777 /opt/jdk1.8.0_60/
    cd /usr/java/
    sudo rm latest default
    sudo ln -s /opt/jdk1.8.0_60/ latest
    sudo ln -s /opt/jdk1.8.0_60/ default
    sudo alternatives --install /usr/bin/java java /opt/jdk1.8.0_60/bin/java 2
    sudo sed -i 's/<\/configuration>/<property><name>mapreduce.reduce.java.opts<\/name><value>-Xmx26112m<\/value><\/property><property><name>mapreduce.reduce.memory.mb<\/name><value>26624<\/value><\/property><property><name>mapreduce.job.maps<\/name><value>4<\/value><\/property><\/configuration>/' /opt/mapr/hadoop/hadoop-2.5.1/etc/hadoop/mapred-site.xml
    sudo sed -i 's/<\/configuration>/<property><name>yarn.scheduler.maximum-allocation-mb<\/name><value>28000<\/value><\/property><property><name>yarn.nodemanager.resource.memory-mb<\/name><value>28672<\/value><\/property><\/configuration>/' /opt/mapr/hadoop/hadoop-2.5.1/etc/hadoop/yarn-site.xml
    sudo reboot
    ```
* Wait until all nodes have restarted and the cluster is fully available again (check by querying HDFS from one node for example or check the MapR UI).
* Create an EBS volume that will hold the source CSV files (20 GB should be enough, take care to create the volume in the same Availability Zone as the cluster instances!). Attach it to one of the clusters nodes, format and mount it.
* Download and extract the CSV files to that volume, remove the first line (=header) of the files (`tail` command, see above).
* Copy the prepared CSV files to HDFS: 
  ```
  hadoop fs -mkdir /data/pums
  hadoop fs -copyFromLocal /your/mountpoint/*.csv /data/pums
  ```
  (This might take a while, for me about 45 mins).
* Copy the uber jar of `diqube-pums-hadoop` to one of the cluster nodes.
* As soon as all CSVs are available in HDFS, start the Map/Reduce: `hadoop jar /path/to/jar/diqube-pums-hadoop-1-SNAPSHOT.jar org.diqube.hadoop.PumsDiqube /data/pums/ss09husa.csv /data/pums/ss09husb.csv /data/pums/ss09husc.csv  /data/pums/ss09husd.csv /data/pums/ss09pusa.csv /data/pums/ss09pusb.csv /data/pums/ss09pusc.csv /data/pums/ss09pusd.csv /data/pums-out`
* After the Map/Reduce is done (took about 1 hour for me), you'll find the result files in HDFS in `/data/pums-out`. Copy them to the local machine, download them and delete the MapR cluster (do that using the "CloudFormation" UI in AWS). Delete the EBS volume, too.

Note that the "i2.xlarge" instances are not part of the Free Tier of AWS, so you'll most probably have to pay. Additionally, the EBS volumes will take up more space than the Free Tier allows.

[1]:https://www.census.gov/programs-surveys/acs/about.html
[2]:https://www.census.gov/programs-surveys/acs/technical-documentation/pums.html
[3]:https://www.census.gov/programs-surveys/acs/technical-documentation/pums/documentation.2009.html
[4]:https://www.census.gov
[5]:http://hadoop.apache.org