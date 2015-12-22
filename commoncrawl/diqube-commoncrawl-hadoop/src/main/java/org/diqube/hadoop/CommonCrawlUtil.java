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

import java.util.Map;
import java.util.function.Function;

/**
 *
 * @author Bastian Gloeckle
 */
public class CommonCrawlUtil {
  public static Object resolveValue(Map<String, Object> data, String field) {
    return executeOnValue(data, field, v -> v);
  }

  @SuppressWarnings("unchecked")
  public static <P, T> T executeOnValue(Map<String, Object> data, String field, Function<P, T> fn) {
    Map<String, Object> curData = data;
    for (String part : field.split("\\.")) {
      if (!curData.containsKey(part))
        return null;

      if (curData.get(part) instanceof Map) {
        curData = (Map<String, Object>) curData.get(part);
        continue;
      } else {
        return fn.apply((P) curData.get(part));
      }
    }
    return null;
  }
}
