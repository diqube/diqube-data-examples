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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    return res;
  }

  private void deriveIp(Map<String, Object> res, Map<String, Object> data) {
    String ip = (String) resolveValue(data, "Envelope.WARC-Header-Metadata.WARC-IP-Address");

    if (ip != null) {
      String city = cityResolver.resolve(ip);
      res.put("ip_city", (city != null) ? city : "");

      String country = countryResolver.resolve(ip);
      res.put("ip_country", (country != null) ? country : "");

      String asn = asnResolver.resolveAsn(ip);
      res.put("ip_asn", (asn != null) ? asn : "");
    } else {
      res.put("ip_city", "");
      res.put("ip_country", "");
      res.put("ip_asn", "");
    }
  }

  private void deriveServer(Map<String, Object> res, Map<String, Object> data) {
    // Parse the string that was returned in Http "Server" response header.
    String httpServer = (String) resolveValue(data, "Envelope.Payload-Metadata.HTTP-Response-Metadata.Headers.Server");

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
        map.put("component", serverComponent);
        if (componentVersion != null)
          map.put("version", componentVersion);
        if (componentComment != null)
          map.put("comment", componentComment);
        foundServerComponents.add(map);
      }

      res.put("server_components", foundServerComponents);
      if (!foundServerComponents.isEmpty()) {
        // the following denotes the "main server information". According to HTTP spec these are the first in the
        // Server response header.
        res.put("server", foundServerComponents.get(0).get("component"));
        res.put("server_version", foundServerComponents.get(0).get("version"));
        res.put("server_comment", foundServerComponents.get(0).get("comment"));
      } else {
        res.put("server", httpServer);
        res.put("server_version", "");
        res.put("server_comment", "");
      }
    } else {
      res.put("server_components", Arrays.asList());
      res.put("server", "");
      res.put("server_version", "");
      res.put("server_comment", "");
    }
  }

  @SuppressWarnings("unchecked")
  private Object resolveValue(Map<String, Object> data, String field) {
    Map<String, Object> curData = data;
    for (String part : field.split("\\.")) {
      if (!curData.containsKey(part))
        return null;

      if (curData.get(part) instanceof Map) {
        curData = (Map<String, Object>) curData.get(part);
        continue;
      } else {
        return curData.get(part);
      }
    }
    return null;
  }
}
