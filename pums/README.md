#diqube Data Examples - PUMS#

This example loads data of the [American Community Survey (ACS)][1] [Public Use Microdata Sample (PUMS)][2], the 5 year#
one containing data from 2005-2009. This data is provided by the [US Census Bureau][4].

The 2005-20099 data set was chosen because the 5 year old data set contains more data than 3 or 1 year data sets, but
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
* Tested with Apache Hadoop 2.7.1, it may also work with other Hadoop versions.

[1]:https://www.census.gov/programs-surveys/acs/about.html
[2]:https://www.census.gov/programs-surveys/acs/technical-documentation/pums.html
[3]:https://www.census.gov/programs-surveys/acs/technical-documentation/pums/documentation.2009.html
[4]:https://www.census.gov
[5]:http://hadoop.apache.org