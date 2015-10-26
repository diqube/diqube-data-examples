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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 * An IP subnet (or a single IP) which can be ordered etc.
 * 
 * The {@link Comparable} orders by startIp.
 *
 * @author Bastian Gloeckle
 */
public class IpSubnet implements Comparable<IpSubnet> {
  private String strValue; // only for toString.
  private BigInteger startIp;
  private BigInteger endIp;
  private boolean isIpv4;

  public IpSubnet(InetAddress start, InetAddress end) throws IllegalArgumentException {
    startIp = new BigInteger(1, start.getAddress());
    endIp = new BigInteger(1, end.getAddress());

    isIpv4 = start.getAddress().length == 4;
  }

  public IpSubnet(String strValue) throws IllegalArgumentException {
    this.strValue = strValue;
    if (strValue.contains("/")) {
      String[] split = strValue.split("/");
      String ipString = split[0];
      int maskInt = Integer.parseInt(split[1]);
      InetAddress addr;
      try {
        addr = InetAddress.getByName(ipString);
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException("Invalid parameter.", e);
      }
      isIpv4 = addr.getAddress().length == 4;

      ByteBuffer maskBuf = ByteBuffer.allocate(addr.getAddress().length);
      for (int i = 0; i < addr.getAddress().length; i++)
        maskBuf.put((byte) 0xff);

      BigInteger mask = new BigInteger(1, maskBuf.array()).shiftRight(maskInt);
      BigInteger ip = new BigInteger(1, addr.getAddress());

      startIp = ip.and(mask.not());
      endIp = startIp.add(mask);
    } else {
      InetAddress addr;
      try {
        addr = InetAddress.getByName(strValue);
      } catch (UnknownHostException e) {
        throw new IllegalArgumentException("Invalid parameter.", e);
      }

      isIpv4 = addr.getAddress().length == 4;
      startIp = new BigInteger(1, addr.getAddress());
      endIp = startIp;
    }
  }

  /**
   * True if this IpSubnet overlaps with the other.
   */
  public boolean overlap(IpSubnet other) {
    if (isIpv4 ^ other.isIpv4)
      return false;

    return (startIp.compareTo(other.startIp) <= 0 && endIp.compareTo(other.startIp) >= 0) || //
        (startIp.compareTo(other.endIp) <= 0 && endIp.compareTo(other.endIp) >= 0) || //
        (startIp.compareTo(other.startIp) <= 0 && endIp.compareTo(other.endIp) >= 0);
  }

  @Override
  public int compareTo(IpSubnet o) {
    if (isIpv4 ^ o.isIpv4) {
      return isIpv4 ? -1 : 1;
    }
    int startCompare = startIp.compareTo(o.startIp);
    if (startCompare != 0)
      return startCompare;
    return endIp.compareTo(o.endIp);
  }

  @Override
  public int hashCode() {
    return startIp.xor(endIp).intValue();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof IpSubnet))
      return false;
    IpSubnet o = (IpSubnet) obj;

    return startIp.equals(o.startIp) && endIp.equals(o.endIp) && isIpv4 == o.isIpv4;
  }

  @Override
  public String toString() {
    return strValue;
  }
}
