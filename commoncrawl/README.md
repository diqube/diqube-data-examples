#diqube Data Examples - Common Crawl#

This example loads data from the [Common Crawl](http://www.commoncrawl.org) project, which regularly crawls the
internet and freely distributes the results. We concentrate on the "WAT" files here, which contain metadata about the
crawl itself and about the answers of the webservers. An example WAT file can be found 
[on GitHub](https://gist.github.com/Smerity/e750f0ef0ab9aa366558#file-bbc-pretty-wat).

##Result data##
The example Map/Reduce job outputs (a) diqube file(s) whcih contain exactly the data of the WAT files - the only change
are the field names which are adjusted to be compatible with diqube column names. In addition to that, a small number 
of additional fields is calculated by the example an stored as fields with names starting with "derived.". See the
[CommonCrawlDiqube](diqube-commoncrawl-hadoop/src/main/java/org/diqube/hadoop/CommonCrawlDiqube.java) class for details.