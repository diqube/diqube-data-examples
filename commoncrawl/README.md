#diqube Data Examples - Common Crawl#

This example loads data from the [Common Crawl](http://www.commoncrawl.org) project, which regularly crawls the
internet and freely distributes the results. We concentrate on the "WAT" files here, which contain metadata about the
crawl itself and about the answers of the webservers. An example WAT file can be found 
[on GitHub](https://gist.github.com/Smerity/e750f0ef0ab9aa366558#file-bbc-pretty-wat).

##Result data##
The example Map/Reduce job outputs (a) diqube file(s) which contain exactly the data of the WAT files - the only change
are the field names which are adjusted to be compatible with diqube column names. In addition to that, a small number 
of additional fields are calculated by the example an stored as fields with names starting with "derived.". See the
[CommonCrawlDiqube](diqube-commoncrawl-hadoop/src/main/java/org/diqube/hadoop/CommonCrawlDeriveData.java) class for
details; the derived fields will be available in the diqube table under "derived.*".

##GeoIP data##
The Map/Reduce job will also derive some data for the IP under which a specific server was reachable. This is done
based on the GeoLite CSV files provided by [MaxMind](http://www.maxmind.com). **Before you build** the uber jar of
diqube-commoncrawl-hadoop, you need to manually download the following files and place them into the folder 
**diqube-commoncrawl-hadoop/src/main/geoip**:

* from http://dev.maxmind.com/geoip/legacy/geolite/
  * GeoLite ASN; CSV/ZIP
    * unzip file **GeoIPASNum2.csv**
  * GeoLite ASN IPv6; CSV/ZIP
    * unzip file **GeoIPASNum2v6.csv**
* from http://dev.maxmind.com/geoip/geoip2/geolite2/
  * GeoLite2 City CSV/ZIP
    * unzip files **GeoLite2-City-Blocks-IPv4.csv**, **GeoLite2-City-Blocks-IPv6.csv** and **GeoLite2-City-Locations-en.csv**
  * GeoLite2 Country CSV/ZIP
    * unzip files **GeoLite2-Country-Blocks-IPv4.csv**, **GeoLite2-Country-Blocks-IPv6.csv** and **GeoLite2-Country-Locations-en.csv**
    
##Hadoop settings##
You might want to check the documentation of the [PUMS example](../pums/) to find out what settings might be
helpful to change.