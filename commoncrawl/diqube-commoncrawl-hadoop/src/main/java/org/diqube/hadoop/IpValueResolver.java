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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.opencsv.CSVParser;

/**
 * Resolves IPs to either cities or countries using GeoLite2 CSV data.
 *
 * @author Bastian Gloeckle
 */
public class IpValueResolver {

  private NavigableMap<IpSubnet, Integer> map = new TreeMap<>();
  private HashMap<Integer, String> values = new HashMap<>();

  public void load(InputStream ip4input, InputStream ip6input, InputStream descriptionInput,
      String descriptionInputColName) throws IOException {
    map.clear();
    values.clear();

    CSVParser parser = new CSVParser();

    BufferedReader ip4Reader = new BufferedReader(new InputStreamReader(ip4input, Charset.forName("UTF-8")));
    BufferedReader ip6Reader = new BufferedReader(new InputStreamReader(ip6input, Charset.forName("UTF-8")));

    for (BufferedReader reader : Arrays.asList(ip4Reader, ip6Reader)) {
      String read = reader.readLine(); // remove header line.
      read = reader.readLine();
      while (read != null) {
        String[] csvParts = parser.parseLine(read);
        String ipSubnet = csvParts[0];
        String geonameIdString = csvParts[1];

        int geonameId;
        try {
          geonameId = Integer.parseInt(geonameIdString);
        } catch (NumberFormatException e) {
          // ignore.
          read = reader.readLine();
          continue;
        }

        map.put(new IpSubnet(ipSubnet), geonameId);

        read = reader.readLine();
      }
    }

    BufferedReader descriptionReader =
        new BufferedReader(new InputStreamReader(descriptionInput, Charset.forName("UTF-8")));
    // Search "value" field in header
    String read = descriptionReader.readLine();
    String[] csvParts = parser.parseLine(read);
    int valueIdx = 0;
    while (!csvParts[valueIdx].equals(descriptionInputColName))
      valueIdx++;

    read = descriptionReader.readLine();
    while (read != null) {
      csvParts = parser.parseLine(read);
      String geonameId = csvParts[0];
      String value = csvParts[valueIdx];
      values.put(Integer.parseInt(geonameId), value);

      read = descriptionReader.readLine();
    }
  }

  public String resolve(String ip) {
    IpSubnet ipSubnet = new IpSubnet(ip);

    Entry<IpSubnet, Integer> entry = map.floorEntry(ipSubnet);
    if (entry == null || !entry.getKey().overlap(ipSubnet))
      return null;

    return values.get(entry.getValue());
  }
}
