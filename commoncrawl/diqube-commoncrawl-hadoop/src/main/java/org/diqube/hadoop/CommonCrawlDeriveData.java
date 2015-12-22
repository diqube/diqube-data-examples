/**
 * diqube: Distributed Query Base.
 *
 * Copyright (C) 2015 Bastian Gloeckle
 *
 * This file is part of diqube data examples.
 *
 * diqube data examples are free software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.diqube.hadoop;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.diqube.util.Triple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Derives some data from the input which is read from WAT files.
 *
 * @author Bastian Gloeckle
 */
public class CommonCrawlDeriveData {
  private static final Logger logger = LoggerFactory.getLogger(CommonCrawlDeriveData.class);

  private static final String GEOLITE_CITY_4 = "/GeoLite2-City-Blocks-IPv4.csv";
  private static final String GEOLITE_CITY_6 = "/GeoLite2-City-Blocks-IPv6.csv";
  private static final String GEOLITE_CITY_DESCRIPTION = "/GeoLite2-City-Locations-en.csv";
  private static final String GEOLITE_COUNTRY_4 = "/GeoLite2-Country-Blocks-IPv4.csv";
  private static final String GEOLITE_COUNTRY_6 = "/GeoLite2-Country-Blocks-IPv6.csv";
  private static final String GEOLITE_COUNTRY_DESCRIPTION = "/GeoLite2-Country-Locations-en.csv";
  private static final String GEOLITE_ASN_4 = "/GeoIPASNum2.csv";
  private static final String GEOLITE_ASN_6 = "/GeoIPASNum2v6.csv";

  public static final String IP_CITY = "ip_city";
  public static final String IP_COUNTRY = "ip_country";
  public static final String IP_ASN = "ip_asn";

  public static final String TLD = "tld";
  public static final String DOMAIN = "domain";
  public static final String SUBDOMAIN = "subdomain";
  public static final String DOMAIN_SCHEME = "domain_scheme";

  public static final String CACHE_SECONDS = "cache_seconds";

  public static final String SERVER_COMPONENTS = "server_components";
  public static final String SERVER_COMPONENTS_COMPONENT = "component";
  public static final String SERVER_COMPONENTS_VERSION = "version";
  public static final String SERVER_COMPONENTS_COMMENT = "comment";

  public static final String SERVER = "server";
  public static final String SERVER_VERSION = "server_version";
  public static final String SERVER_COMMENT = "server_comment";

  public static final String TOP_LINK_DOMAINS = "top_link_domains";
  public static final String TOP_LINK_DOMAINS_DOMAIN = "domain";
  public static final String TOP_LINK_DOMAINS_COUNT = "count";

  private IpValueResolver cityResolver;
  private IpValueResolver countryResolver;
  private IpAsnResolver asnResolver;

  public CommonCrawlDeriveData() throws IOException {
    logger.info("Loading GeoIP data...");

    cityResolver = new IpValueResolver();
    cityResolver.load(CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_CITY_4), //
        CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_CITY_6), //
        CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_CITY_DESCRIPTION), //
        "city_name");

    countryResolver = new IpValueResolver();
    countryResolver.load(CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_COUNTRY_4), //
        CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_COUNTRY_6), //
        CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_COUNTRY_DESCRIPTION), //
        "country_name");

    asnResolver = new IpAsnResolver();
    asnResolver.load(CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_ASN_4), //
        CommonCrawlDeriveData.class.getResourceAsStream(GEOLITE_ASN_6));

    logger.info("GeoIP data loaded.");
  }

  public Map<String, Object> deriveData(Map<String, Object> data) {
    Map<String, Object> res = new HashMap<>();

    deriveServer(res, data);
    deriveIp(res, data);
    deriveDomain(res, data);
    deriveCache(res, data);
    deriveTopLinkDomains(res, data);

    return res;
  }

  private void deriveIp(Map<String, Object> res, Map<String, Object> data) {
    String ip = (String) CommonCrawlUtil.resolveValue(data, "Envelope.WARC-Header-Metadata.WARC-IP-Address");

    if (ip != null) {
      String city = cityResolver.resolve(ip);
      res.put(IP_CITY, (city != null) ? city : "");

      String country = countryResolver.resolve(ip);
      res.put(IP_COUNTRY, (country != null) ? country : "");

      String asn = asnResolver.resolveAsn(ip);
      res.put(IP_ASN, (asn != null) ? asn : "");
    } else {
      res.put(IP_CITY, "");
      res.put(IP_COUNTRY, "");
      res.put(IP_ASN, "");
    }
  }

  private void deriveDomain(Map<String, Object> res, Map<String, Object> data) {
    String url = (String) CommonCrawlUtil.resolveValue(data, "Envelope.WARC-Header-Metadata.WARC-Target-URI");

    res.put(TLD, "");
    res.put(DOMAIN, "");
    res.put(SUBDOMAIN, "");
    res.put(DOMAIN_SCHEME, "");

    if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
      try {
        URI uri = new URI(url);
        Triple<String, String, String> t = getDomainInfo(uri);
        res.put(TLD, t.getLeft());
        res.put(DOMAIN, t.getMiddle());
        res.put(SUBDOMAIN, t.getRight());

        if (uri.getScheme() != null)
          res.put(DOMAIN_SCHEME, uri.getScheme());
      } catch (URISyntaxException | IndexOutOfBoundsException e) {
        // swallow, accept entries done up until here.
      }
    }
  }

  private void deriveCache(Map<String, Object> res, Map<String, Object> data) {
    String cacheControl = (String) CommonCrawlUtil.resolveValue(data,
        "Envelope.Payload-Metadata.HTTP-Response-Metadata.Headers.Cache-Control");

    Long cacheTimeSeconds = null;
    if (cacheControl != null) {
      String[] singleCacheControls = cacheControl.split(",");
      for (String s : singleCacheControls) {
        s = s.trim();
        if (s.startsWith("max-age")) {
          String[] values = s.split("=");
          if (values.length > 1) {
            try {
              cacheTimeSeconds = Long.parseLong(values[1]);
            } catch (NumberFormatException e) {
              // no valid number, ignore.
            }
          }
          break;
        }
      }
    }

    if (cacheTimeSeconds == null) {
      String expires = (String) CommonCrawlUtil.resolveValue(data,
          "Envelope.Payload-Metadata.HTTP-Response-Metadata.Headers.Expires");
      if (expires != null) {
        LocalDateTime startDateTime = null;

        DateTimeFormatter httpDateFormatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

        try {
          String sourceDate = (String) CommonCrawlUtil.resolveValue(data,
              "Envelope.Payload-Metadata.HTTP-Response-Metadata.Headers.Date");
          if (sourceDate != null) {
            startDateTime = LocalDateTime.parse(sourceDate, httpDateFormatter);
          } else {
            // use crawl date as start.
            String crawlDate = (String) CommonCrawlUtil.resolveValue(data, "Envelope.WARC-Header-Metadata.WARC-Date");
            if (crawlDate != null) {
              startDateTime = LocalDateTime.parse(crawlDate, DateTimeFormatter.ISO_INSTANT);
            }
          }

          LocalDateTime expiresDateTime = LocalDateTime.parse(expires, httpDateFormatter);

          if (startDateTime != null)
            cacheTimeSeconds = startDateTime.until(expiresDateTime, ChronoUnit.SECONDS);
        } catch (DateTimeParseException e) {
          // error parsing a date, ignore.
        }
      }
    }

    res.put(CACHE_SECONDS, (cacheTimeSeconds != null) ? cacheTimeSeconds : Long.valueOf(-1L));
  }

  private void deriveServer(Map<String, Object> res, Map<String, Object> data) {
    // Parse the string that was returned in Http "Server" response header.
    String httpServer =
        (String) CommonCrawlUtil.resolveValue(data, "Envelope.Payload-Metadata.HTTP-Response-Metadata.Headers.Server");

    if (httpServer != null) {
      List<Map<String, Object>> foundServerComponents = new ArrayList<>();

      Pattern p = Pattern.compile("[^-_a-zA-Z0-9]*([-_a-zA-Z0-9]+)(/[-a-zA-Z0-9\\.]+)? *(\\(.*?\\))?");
      Matcher m = p.matcher(httpServer);
      while (m.find()) {
        String serverComponent = m.group(1);
        String componentVersion = null;
        String componentComment = null;
        if (m.groupCount() > 1 & m.group(2) != null) {
          if (m.group(2).startsWith("/"))
            componentVersion = m.group(2).substring(1);
          else
            componentComment = m.group(2).substring(1, m.group(2).length() - 1);
        }
        if (m.groupCount() > 2 && m.group(3) != null)
          if (m.group(3).startsWith("("))
            componentComment = m.group(3).substring(1, m.group(3).length() - 1);

        Map<String, Object> map = new HashMap<>();
        map.put(SERVER_COMPONENTS_COMPONENT, serverComponent);
        if (componentVersion != null)
          map.put(SERVER_COMPONENTS_VERSION, componentVersion);
        if (componentComment != null)
          map.put(SERVER_COMPONENTS_COMMENT, componentComment);
        foundServerComponents.add(map);
      }

      res.put(SERVER_COMPONENTS, foundServerComponents);
      if (!foundServerComponents.isEmpty()) {
        // the following denotes the "main server information". According to HTTP spec these are the first in the
        // Server response header.
        String server = (String) foundServerComponents.get(0).get(SERVER_COMPONENTS_COMPONENT);
        res.put(SERVER, (server != null) ? server : "");
        String version = (String) foundServerComponents.get(0).get(SERVER_COMPONENTS_VERSION);
        res.put(SERVER_VERSION, (version != null) ? version : "");
        String comment = (String) foundServerComponents.get(0).get(SERVER_COMPONENTS_COMMENT);
        res.put(SERVER_COMMENT, (comment != null) ? comment : "");
      } else {
        res.put(SERVER, httpServer);
        res.put(SERVER_VERSION, "");
        res.put(SERVER_COMMENT, "");
      }
    } else {
      res.put(SERVER_COMPONENTS, Arrays.asList());
      res.put(SERVER, "");
      res.put(SERVER_VERSION, "");
      res.put(SERVER_COMMENT, "");
    }
  }

  private void deriveTopLinkDomains(Map<String, Object> res, Map<String, Object> data) {
    @SuppressWarnings("unchecked")
    List<Map<String, String>> links = (List<Map<String, String>>) CommonCrawlUtil.resolveValue(data,
        "Envelope.Payload-Metadata.HTTP-Response-Metadata.HTML-Metadata.Links");

    if (links == null) {
      res.put(TOP_LINK_DOMAINS, new ArrayList<>());
      return;
    }

    // use this domain for relative links.
    String ourUriString = (String) CommonCrawlUtil.resolveValue(data, "Envelope.WARC-Header-Metadata.WARC-Target-URI");
    String ourDomain;
    try {
      ourDomain = getDomainInfo(new URI(ourUriString)).getMiddle();
    } catch (URISyntaxException e1) {
      ourDomain = "";
    }

    Map<String, Integer> countMap = new HashMap<>();

    for (Map<String, String> m : links) {
      String targetUrl = m.get("url");
      if (targetUrl == null)
        continue;
      try {
        String domain;
        URI uri = new URI(targetUrl);
        if (uri.getHost() != null && (uri.getHost().startsWith("http://") || uri.getHost().startsWith("https://")))
          domain = getDomainInfo(uri).getMiddle();
        else
          domain = ourDomain;

        countMap.merge(domain, 1, (a, b) -> a + b);
      } catch (URISyntaxException e) {
        // ignore, as we cannot read URI of link.
      }
    }

    List<String> topList = new ArrayList<>(countMap.keySet());
    // sort decreasing by counts, cut off after 5 elements.
    topList = topList.stream().sorted((a, b) -> countMap.get(b).compareTo(countMap.get(a))).limit(5)
        .collect(Collectors.toList());

    List<Map<String, Object>> topMap = new ArrayList<>();
    for (String topDomain : topList) {
      Map<String, Object> m = new HashMap<>();
      m.put(TOP_LINK_DOMAINS_DOMAIN, topDomain);
      m.put(TOP_LINK_DOMAINS_COUNT, countMap.get(topDomain));
      topMap.add(m);
    }

    res.put(TOP_LINK_DOMAINS, topMap);
  }

  /**
   * @return Triple with TLD, domain and subdomain from the URI.
   */
  private Triple<String, String, String> getDomainInfo(URI uri) {
    String tld = "";
    String domain = "";
    String subdomain = "";

    if (!uri.getHost().contains(":")) { // ":" could be a lead for a IPv6 as host, ignore.
      String[] hostSplit = uri.getHost().split("\\.");
      boolean lastHostPartIsNumber;
      try {
        Integer.valueOf(hostSplit[hostSplit.length - 1]);
        lastHostPartIsNumber = true;
      } catch (NumberFormatException e) {
        lastHostPartIsNumber = false;
      }

      if (!lastHostPartIsNumber) { // if last part of host is a number, it might be an IPv4, ignore.
        if (hostSplit.length >= 1)
          tld = hostSplit[hostSplit.length - 1];
        if (hostSplit.length >= 2)
          domain = hostSplit[hostSplit.length - 2] + "." + hostSplit[hostSplit.length - 1];
        if (hostSplit.length >= 3)
          subdomain = hostSplit[hostSplit.length - 3] + "." + hostSplit[hostSplit.length - 2] + "."
              + hostSplit[hostSplit.length - 1];
      } else
        // use IPv4 as "domain"
        domain = uri.getHost();
    } else
      // use IPv6 as "domain".
      domain = uri.getHost();
    return new Triple<>(tld, domain, subdomain);
  }
}
