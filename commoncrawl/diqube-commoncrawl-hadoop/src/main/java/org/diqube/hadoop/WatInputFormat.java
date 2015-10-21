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

import java.io.BufferedInputStream;
import java.io.IOException;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveRecord;
import org.archive.io.warc.WARCReaderFactory;

/**
 * Input format for reading .wat or .wat.gz files.
 * 
 * <p>
 * The {@link RecordReader} which is returned will provide only a single {@link ArchiveReader}, which can then be used
 * in the mapper to iterate over the entries in the .wat file. Unfortunately, we cannot return {@link ArchiveRecord}s
 * directly in this input format, as the current {@link ArchiveRecord} will be {@link ArchiveRecord#close()}ed when
 * iterating through the {@link ArchiveReader}.
 * 
 *
 * @author Bastian Gloeckle
 */
public class WatInputFormat extends FileInputFormat<Text, ArchiveReader> {

  @Override
  public RecordReader<Text, ArchiveReader> createRecordReader(InputSplit split, TaskAttemptContext context)
      throws IOException, InterruptedException {
    return new WatRecordReader();
  }

  @Override
  protected boolean isSplitable(JobContext context, Path filename) {
    // not easily splittable.
    return false;
  }

  /**
   * Simple {@link RecordReader} which returns a single {@link ArchiveReader} for a whole input file.
   * 
   * Only works on {@link FileSplit} inputs.
   */
  public static class WatRecordReader extends RecordReader<Text, ArchiveReader> {
    private FSDataInputStream inStream;
    private ArchiveReader archiveReader;
    private boolean singleEntryRead;
    private Path sourcePath;

    @Override
    public void initialize(InputSplit split, TaskAttemptContext context) throws IOException, InterruptedException {
      sourcePath = ((FileSplit) split).getPath();
      inStream = sourcePath.getFileSystem(context.getConfiguration()).open(sourcePath);
      archiveReader = WARCReaderFactory.get(sourcePath.getName(), new BufferedInputStream(inStream), true);
      singleEntryRead = false;
    }

    @Override
    public boolean nextKeyValue() throws IOException, InterruptedException {
      if (singleEntryRead)
        return false;
      return (singleEntryRead = true);
    }

    @Override
    public Text getCurrentKey() throws IOException, InterruptedException {
      return new Text(sourcePath.toString());
    }

    @Override
    public ArchiveReader getCurrentValue() throws IOException, InterruptedException {
      return archiveReader;
    }

    @Override
    public float getProgress() throws IOException, InterruptedException {
      if (singleEntryRead)
        return 1;
      return 0;
    }

    @Override
    public void close() throws IOException {
      archiveReader.close();
      inStream.close();
    }

  }

}
