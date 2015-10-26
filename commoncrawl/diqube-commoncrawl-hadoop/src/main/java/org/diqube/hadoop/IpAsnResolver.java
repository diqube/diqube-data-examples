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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.opencsv.CSVParser;

/**
 * Resolves IPs to ASNs (Autonomous System Numbers) using GeoLite CSV data.
 *
 * @author Bastian Gloeckle
 */
public class IpAsnResolver {
  private NavigableMap<IpSubnet, String> asns = new TreeMap<>();

  public void load(InputStream ip4asn, InputStream ip6asn) throws IOException {
    CSVParser parser = new CSVParser();
    BufferedReader ipv4Reader = new BufferedReader(new InputStreamReader(ip4asn, Charset.forName("UTF-8")));
    String read = ipv4Reader.readLine();
    while (read != null) {
      String[] csvParts = parser.parseLine(read);

      long numFrom = Long.parseLong(csvParts[0]);
      long numTo = Long.parseLong(csvParts[1]);
      String asn = csvParts[2];

      IpSubnet subnet = new IpSubnet(numberTo4Addr(numFrom), numberTo4Addr(numTo));

      asns.put(subnet, asn);

      read = ipv4Reader.readLine();
    }

  }

  private InetAddress numberTo4Addr(long number) throws UnknownHostException {
    // as by documentation of MaxMind.
    byte b1 = (byte) ((number / 16777216) % 256);
    byte b2 = (byte) ((number / 65536) % 256);
    byte b3 = (byte) ((number / 256) % 256);
    byte b4 = (byte) ((number) % 256);
    return InetAddress.getByAddress(new byte[] { b1, b2, b3, b4 });
  }

  public String resolveAsn(String ip) {
    IpSubnet ipSubnet = new IpSubnet(ip);

    Entry<IpSubnet, String> asnEntry = asns.floorEntry(ipSubnet);

    if (asnEntry == null || !asnEntry.getKey().overlap(ipSubnet))
      return null;

    return asnEntry.getValue();
  }
}
