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


##Result fields of table##

The following is a list of fields that might be of interest in the resulting diqube table. As the fields are derived 
from the fields of the original wat files, see here for more fields: https://gist.github.com/Smerity/e750f0ef0ab9aa366558#file-bbc-pretty-wat

###Fields derived by the MapReduce###

* `derived.ip_city` = "City" information of the IP of a server
* `derived.ip_country` = "Country" information of the IP of a server
* `derived.ip_asn` = "ASN" information of the IP of a server
* `derived.tld` = The top level domain of the requested URL
* `derived.domain` = The domain (= second level domain) of the requested URL or the full IP address, if there was no DNS name
* `derived.subdomain` = The third level domain or the requested URL
* `dervied.domain_scheme` = The scheme of the requested URL
* `derived.cache_seconds` = The number of seconds the server asked commoncrawl to cache the website. Derived from Cache-Control "max-age" and/or the "Expires" HTTP header.
* `derived.server_components` = Array of objects with information about each component returned by the server in the "Server" HTTP header:
  * `component` = name of the component
  * `version` = version of the component
  * `comment` = comment of the component
* `derived.server` = Name of the first server_component, usually the name of the server software serving the website
* `derived.server_version` = Version of the first server_component 
* `derived.server_comment` = Comment of the first server_component
* `derived.top_link_domains` = Array of objects with information about the links that the retrieved document had
  * `domain` = The (second level) domain that was linked to
  * `count` = The number of times the retrieved document linked to the domain

###Fields provided by commoncrawl###

* `envelope.warc_header_metadata.warc_ip_address` = IP address of the server from which the document was retrieved
* `envelope.warc_header_metadata.warc_target_uri` = Full URL of the retrieved document
* `envelope.payload_metadata.http_response_metadata.entity_length` = Size of the document
* `envelope.payload_metadata.http_response_metadata.response_message.version` = HTTP version of the server
* `envelope.payload_metadata.http_response_metadata.headers.content_type` = "Content-Type" header reported by the server
