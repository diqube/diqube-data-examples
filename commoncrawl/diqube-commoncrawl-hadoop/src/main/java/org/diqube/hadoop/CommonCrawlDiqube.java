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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.diqube.data.column.ColumnPage;
import org.diqube.data.table.TableShard;
import org.diqube.loader.columnshard.ColumnShardBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

/**
 * Map/Reduce job that reads metadata files (wat files) provided by http://commoncrawl.org/.
 * 
 * <p>
 * It produces .diqube output file(s) which can easily be loaded into diqube for evaluation.
 * 
 * <p>
 * Note that the resulting diqube table will have the same layout as the JSON in the wat files, but field names will be
 * adjusted slightly to be compatible with diqube (see #cleanFieldName(...)).
 * 
 * <p>
 * In addition to that, there will be a few fields added under "derived" which are calculated from the raw data, to make
 * querying easier.
 * 
 *
 * @author Bastian Gloeckle
 */
public class CommonCrawlDiqube {
  private static final Logger logger = LoggerFactory.getLogger(CommonCrawlDiqube.class);

  /**
   * set to <code>true</code> to not include a full list of links in .diqube file. This is meaningful if the MapReduce
   * is executed on machines with not that much main memory. The list of links could be pretty large, which would lead
   * to a lot of columns being created, which means that the memory limit might be reached after only a few rows, not
   * even filling a full {@link ColumnPage} of a new {@link TableShard} (see {@link ColumnShardBuilder#PROPOSAL_ROWS}).
   * 
   * Use <code>false</code> to include all links.
   */
  private static final boolean FILTER_FULL_LINK_LIST = true;

  public static class CommonCrawlMapper extends Mapper<Text, ArchiveReader, BytesWritable, BytesWritable> {
    /**
     * Field names in the input JSON format which will be converted to LONGs, although they are presented as Strings in
     * the input.
     * 
     * It is unfortunate, but the commoncrawl JSON seems to provide only strings as input, but some fields are clearly
     * numbers, we therefore use this list to convert the values of those fields.
     */
    private static final Set<String> LONG_FIELDS = new HashSet<>(Arrays.asList( //
        "Container.Gzip-Metadata.Footer-Length", "Container.Gzip-Metadata.Deflate-Length",
        "Container.Gzip-Metadata.Header-Length", "Container.Gzip-Metadata.Inflated-Length", "Container.Offset", //
        "Envelope.WARC-Header-Length", "Envelope.Actual-Content-Length", "Envelope.WARC-Header-Metadata.Content-Length",
        "Envelope.Payload-Metadata.Trailing-Slop-Length",
        "Envelope.Payload-Metadata.HTTP-Response-Metadata.Headers-Length",
        "Envelope.Payload-Metadata.HTTP-Response-Metadata.Entity-Length",
        "Envelope.Payload-Metadata.HTTP-Response-Metadata.Entity-Trailing-Slop-Bytes"));

    private CommonCrawlDeriveData derive = null;

    @SuppressWarnings("unchecked")
    @Override
    protected void map(Text fileName, ArchiveReader archiveReader, Context ctx)
        throws IOException, InterruptedException {
      if (derive == null)
        derive = new CommonCrawlDeriveData();

      JsonFactory jsonFactory = new JsonFactory();
      ObjectMapper mapper = new ObjectMapper(jsonFactory);
      for (ArchiveRecord archiveRecord : archiveReader) {
        // only work on those records that have actual JSON content and that contains the metadata of the crawl.
        if (archiveRecord.getHeader().getMimetype().equals("application/json") && //
            archiveRecord.getHeader().getHeaderValue("WARC-Type").equals("metadata")) {

          // Filter out entries that do not denote a valid URI - e.g. the data seems to contain results from retrieving
          // the target data file itself (= the .wat file). We only want to work on real internet URIs.
          String targetUriString = (String) archiveRecord.getHeader().getHeaderValue("WARC-Target-URI");
          try {
            URI targetUri = new URI(targetUriString);
            if (targetUri.getScheme() == null) {
              logger.info("Ignoring entry for WARC-Target-URI '{}' as it does not contain a scheme.", targetUriString);
              continue;
            }
          } catch (URISyntaxException e) {
            logger.info("Ignoring entry for WARC-Target-URI '{}' as it is not a well-formed URI.", targetUriString);
            continue;
          }

          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ByteStreams.copy(archiveRecord, baos);
          byte[] jsonInputBytes = baos.toByteArray();

          // logger.info("Working on: {}", new String(jsonInputBytes, Charset.forName("UTF-8")));

          // parse JSON into a map structure
          Map<String, Object> map = mapper.readValue(jsonInputBytes, new TypeReference<HashMap<String, Object>>() {
          });

          // unfortunately, the URI is sometimes (?) not yet contained in the JSON, we therefore add it.
          ((Map<String, Object>) ((Map<String, Object>) map.get("Envelope")).get("WARC-Header-Metadata"))
              .put("WARC-Target-URI", targetUriString);

          // derive some data from the original map so querying in diqube gets easier
          map.put("derived", derive.deriveData(map));

          DiqubeRow newRow = new DiqubeRow();

          if (FILTER_FULL_LINK_LIST)
            CommonCrawlUtil.<Collection<?>, Object> executeOnValue(map,
                "Envelope.Payload-Metadata.HTTP-Response-Metadata.HTML-Metadata.Links", lst -> {
                  lst.clear();
                  return null;
                });

          addToRow(newRow.withData(), "", map);

          baos = new ByteArrayOutputStream();
          try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(newRow);
          }

          // We want to (1) distribute all rows randomly and (2) want to sort the rows of a specific Reducer, so that it
          // creates ColumnPages that most probably can be skipped on "usual queries".
          // For (1): We use the HashPartitionor, so we need to take care that simply each row has a different key, so
          // the hash will be different and the rows will be distributed somewhat randomly.
          // For (2): "Usual queries" might query only rows that are in a specific "bucket" and we simply guess here to
          // build the buckets according to specific field values of the row.
          String bucketId = //
              ((String) CommonCrawlUtil.resolveValue(map, "derived." + CommonCrawlDeriveData.TLD)) + ":" + //
                  ((String) CommonCrawlUtil.resolveValue(map, "derived." + CommonCrawlDeriveData.SERVER)) + ":" + //
                  ((String) CommonCrawlUtil.resolveValue(map, "derived." + CommonCrawlDeriveData.IP_COUNTRY)) + ":" //
                  ;

          // ensure that rows are distributed "randomly", but sorting on single reducers is by bucketId.
          String identifier = bucketId + ":" + map.hashCode();
          ctx.write(new BytesWritable(identifier.getBytes(Charset.forName("UTF-8"))),
              new BytesWritable(baos.toByteArray()));
        }
      }
    }

    @SuppressWarnings("unchecked")
    private void addToRow(DiqubeRow.DiqubeData res, String parentFieldName, Map<String, Object> values)
        throws IOException {
      for (String origFieldName : values.keySet()) {
        Object overallValue = values.get(origFieldName);

        String fullOrigFieldName;
        if ("".equals(parentFieldName))
          fullOrigFieldName = origFieldName;
        else
          fullOrigFieldName = parentFieldName + "." + origFieldName;

        String fieldName = cleanFieldName(origFieldName);

        if (overallValue instanceof Iterable) {
          for (Object value : (Iterable<Object>) overallValue) {
            if (LONG_FIELDS.contains(fullOrigFieldName) && (value instanceof String))
              value = Long.parseLong((String) value);

            if (value == null)
              logger.debug("Ignoring null value of field {}.", fieldName);
            else if (value instanceof String)
              res.addRepeatedData(fieldName, (String) value);
            else if (value instanceof Map && !((Map<String, Object>) value).isEmpty()) {
              addToRow(res.addNewRepeatedDiqubeData(fieldName), fullOrigFieldName, (Map<String, Object>) value);
            } else if (value instanceof Number) {
              if (value instanceof Integer || value instanceof Long)
                res.addRepeatedData(fieldName, ((Number) value).longValue());
              else if (value instanceof Float || value instanceof Double)
                res.addRepeatedData(fieldName, ((Number) value).doubleValue());
              else
                throw new IOException("Unsupported number type: " + value.toString());
            } else if (value instanceof Boolean) {
              res.addRepeatedData(fieldName, ((Boolean) value).booleanValue() ? 1L : 0L);
            }
          }
        } else {
          Object value = overallValue;
          if (LONG_FIELDS.contains(fullOrigFieldName) && (value instanceof String))
            value = Long.parseLong((String) value);

          if (value == null)
            logger.debug("Ignoring null value of field {}.", fieldName);
          else if (value instanceof String)
            res.withData(fieldName, (String) value);
          else if (value instanceof Map && !((Map<String, Object>) value).isEmpty())
            addToRow(res.withNewDiqubeData(fieldName), fullOrigFieldName, (Map<String, Object>) value);
          else if (value instanceof Number) {
            if (value instanceof Integer || value instanceof Long)
              res.withData(fieldName, ((Number) value).longValue());
            else if (value instanceof Float || value instanceof Double)
              res.withData(fieldName, ((Number) value).doubleValue());
            else
              throw new IOException("Unsupported number type: " + value.toString());
          } else if (value instanceof Boolean) {
            res.withData(fieldName, ((Boolean) value).booleanValue() ? 1L : 0L);
          }
        }
      }
    }

    private String cleanFieldName(String origFieldName) {
      return origFieldName.replace("-", "_").replace("#", "_").toLowerCase();
    }

  }

  public static class CommonCrawlReducer extends Reducer<BytesWritable, BytesWritable, NullWritable, DiqubeRow> {
    @Override
    protected void reduce(BytesWritable rowIdentifier, Iterable<BytesWritable> rowsBytes, Context ctx)
        throws IOException, InterruptedException {
      for (BytesWritable rowBytes : rowsBytes) {
        try (ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(rowBytes.getBytes()))) {
          DiqubeRow row = (DiqubeRow) is.readObject();
          ctx.write(NullWritable.get(), row);
        } catch (ClassNotFoundException e) {
          logger.error("Could not deserialize map result!", e);
          throw new IOException("Could not deserialize map result!", e);
        }
      }
    }

  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "CommonCrawl WAT diqube");
    job.setJarByClass(CommonCrawlDiqube.class);

    job.setMapperClass(CommonCrawlMapper.class);
    job.setMapOutputKeyClass(BytesWritable.class);
    job.setMapOutputValueClass(BytesWritable.class);

    job.setReducerClass(CommonCrawlReducer.class);

    job.setInputFormatClass(WatInputFormat.class);

    job.setOutputFormatClass(DiqubeOutputFormat.class);
    job.setOutputKeyClass(NullWritable.class);
    job.setOutputValueClass(DiqubeRow.class);
    job.setNumReduceTasks(1);

    for (int i = 0; i < args.length - 1; i++)
      FileInputFormat.addInputPath(job, new Path(args[i]));
    DiqubeOutputFormat.setOutputPath(job, new Path(args[args.length - 1]));
    DiqubeOutputFormat.setMemoryFlushMb(job, Math.round(12 * 1024L));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
