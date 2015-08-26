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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.diqube.hadoop.DiqubeRow.DiqubeData;

import com.opencsv.CSVParser;

/**
 *
 * @author Bastian Gloeckle
 */
public class PumsDiqube {
  public static class ToDiqubeRowMapper extends Mapper<Object, Text, LongWritable, BytesWritable> {

    private static final String[] HOUSE_HEADER = new String[] { "DIVISION", "PUMA", "REGION", "ST", "ADJHSG", "ADJINC",
        "WGTP", "NP", "TYPE", "ACR", "AGS", "BDS", "BLD", "BUS", "CONP", "ELEP", "FS", "FULP", "GASP", "HFL", "INSP",
        "MHP", "MRGI", "MRGP", "MRGT", "MRGX", "RMS", "RNTM", "RNTP", "SMP", "TEL", "TEN", "VACS", "VAL", "VEH", "WATP",
        "YBL", "FES", "FINCP", "FPARC", "GRNTP", "GRPIP", "HHL", "HHT", "HINCP", "HUGCL", "HUPAC", "HUPAOC", "HUPARC",
        "KIT", "LNGI", "MV", "NOC", "NPF", "NPP", "NR", "NRC", "OCPIP", "PARTNER", "PLM", "PSF", "R18", "R60", "R65",
        "RESMODE", "SMOCP", "SMX", "SRNT", "SVAL", "TAXP", "WIF", "WKEXREL", "WORKSTAT", "FACRP", "FAGSP", "FBDSP",
        "FBLDP", "FBUSP", "FCONP", "FELEP", "FFSP", "FFULP", "FGASP", "FHFLP", "FINSP", "FKITP", "FMHP", "FMRGIP",
        "FMRGP", "FMRGTP", "FMRGXP", "FMVP", "FPLMP", "FRMSP", "FRNTMP", "FRNTP", "FSMP", "FSMXHP", "FSMXSP", "FTAXP",
        "FTELP", "FTENP", "FVACSP", "FVALP", "FVEHP", "FWATP", "FYBLP", "WGTPR" }; /* last field is repeated */

    private static final String[] PERSON_HEADER = new String[] { "SPORDER", "PUMA", "ST", "ADJINC", "PWGTP", "AGEP",
        "CIT", "COW", "ENG", "FER", "GCL", "GCM", "GCR", "INTP", "JWMNP", "JWRIP", "JWTR", "LANX", "MAR", "MIG", "MIL",
        "MLPA", "MLPB", "MLPC", "MLPD", "MLPE", "MLPF", "MLPG", "MLPH", "MLPI", "MLPJ", "MLPK", "NWAB", "NWAV", "NWLA",
        "NWLK", "NWRE", "OIP", "PAP", "REL", "RETP", "SCH", "SCHG", "SCHL", "SEMP", "SEX", "SSIP", "SSP", "WAGP",
        "WKHP", "WKL", "WKW", "YOEP", "ANC", "ANC1P", "ANC2P", "DECADE", "DRIVESP", "ESP", "ESR", "HISP", "INDP",
        "JWAP", "JWDP", "LANP", "MIGPUMA", "MIGSP", "MSP", "NAICSP", "NATIVITY", "OC", "OCCP", "PAOC", "PERNP", "PINCP",
        "POBP", "POVPIP", "POWPUMA", "POWSP", "QTRBIR", "RAC1P", "RAC2P", "RAC3P", "RACAIAN", "RACASN", "RACBLK",
        "RACNHPI", "RACNUM", "RACSOR", "RACWHT", "RC", "SFN", "SFR", "SOCP", "VPS", "WAOB", "FAGEP", "FANCP", "FCITP",
        "FCOWP", "FENGP", "FESRP", "FFERP", "FGCLP", "FGCMP", "FGCRP", "FHISP", "FINDP", "FINTP", "FJWDP", "FJWMNP",
        "FJWRIP", "FJWTRP", "FLANP", "FLANXP", "FMARP", "FMIGP", "FMIGSP", "FMILPP", "FMILSP", "FOCCP", "FOIP", "FPAP",
        "FPOBP", "FPOWSP", "FRACP", "FRELP", "FRETP", "FSCHGP", "FSCHLP", "FSCHP", "FSEMP", "FSEXP", "FSSIP", "FSSP",
        "FWAGP", "FWKHP", "FWKLP", "FWKWP", "FYOEP", "PWGTPR" }; /* last field is repeated */

    @Override
    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
      String[] values = new CSVParser().parseLine(value.toString());

      long serialNo = Long.parseLong(values[0]);
      String type = values[1];

      try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

        if (type.equals("P")) {
          // person tag
          baos.write(new byte[] { 'P' });

          DiqubeRow row = new DiqubeRow();
          DiqubeData data = row.withData();
          writeValueArrayToDiqubeData(PERSON_HEADER, values, data);

          try (ObjectOutputStream objOutStream = new ObjectOutputStream(baos)) {
            objOutStream.writeObject(row);
          }
        } else if (type.equals("H")) {
          // housing tag
          baos.write(new byte[] { 'H' });

          DiqubeRow row = new DiqubeRow();
          DiqubeData data = row.withData();
          writeValueArrayToDiqubeData(HOUSE_HEADER, values, data);
          data.withData("serialNo", serialNo);

          try (ObjectOutputStream objOutStream = new ObjectOutputStream(baos)) {
            objOutStream.writeObject(row);
          }
        } else
          throw new IOException("Invalid type: " + type);

        context.write(new LongWritable(serialNo), new BytesWritable(baos.toByteArray()));
      }
    }

    private void writeValueArrayToDiqubeData(String[] header, String[] values, DiqubeData data) throws IOException {
      for (int valueIdx = 2; valueIdx < values.length; valueIdx++) {
        int headerIdx = valueIdx - 2;
        String colName;
        boolean isRepeated = false;
        if (headerIdx >= header.length - 1) { // last field is repeated
          colName = header[header.length - 1];
          isRepeated = true;
        } else
          colName = header[headerIdx];

        Function<String, Object> valueAdjustFunc = PumsAdjust.getAdjustFunc(colName);
        colName = PumsAdjust.getNiceColName(colName);

        String valueStr = (values[valueIdx] == null) ? "" : values[valueIdx];
        Object valObject = valueAdjustFunc.apply(valueStr);

        if (isRepeated)
          data.addRepeatedData(colName, valObject);
        else
          data.withData(colName, valObject);
      }
    }
  }

  public static class CombineRowsReducer extends Reducer<LongWritable, BytesWritable, NullWritable, DiqubeRow> {

    @Override
    public void reduce(LongWritable serialNo, Iterable<BytesWritable> values, Context context)
        throws IOException, InterruptedException {
      List<DiqubeRow> persons = new ArrayList<>();
      DiqubeRow house = null;

      for (BytesWritable value : values) {
        ByteArrayInputStream bais = new ByteArrayInputStream(value.copyBytes());
        byte[] firstByte = new byte[1];
        while (bais.read(firstByte) != 1)
          ;
        ObjectInputStream ois = new ObjectInputStream(bais);

        try {
          if (firstByte[0] == 'P') {
            DiqubeRow personRow = (DiqubeRow) ois.readObject();
            persons.add(personRow);
          } else if (firstByte[0] == 'H') {
            if (house != null)
              throw new IOException("Multiple houses available for serialNo " + serialNo.get());
            house = (DiqubeRow) ois.readObject();
          } else
            throw new IOException("Unknown value for serialNo " + serialNo.get() + ": " + firstByte[0]);
        } catch (ClassNotFoundException e) {
          throw new IOException("Could not deserialize data from mappers", e);
        }
      }

      if (house == null) {
        System.out.println("Skipping serialNo " + serialNo.get() + " as there is no house record.");
        return;
      }

      for (DiqubeRow personRow : persons)
        house.getData().addRepeatedData("persons", personRow.getData());

      context.write(NullWritable.get(), house);
    }
  }

  public static void main(String[] args) throws Exception {
    Configuration conf = new Configuration();
    Job job = Job.getInstance(conf, "Pums Diqube");
    job.setJarByClass(PumsDiqube.class);

    job.setMapperClass(ToDiqubeRowMapper.class);
    job.setMapOutputKeyClass(LongWritable.class);
    job.setMapOutputValueClass(BytesWritable.class);

    job.setReducerClass(CombineRowsReducer.class);
    job.setOutputFormatClass(DiqubeOutputFormat.class);
    job.setOutputKeyClass(NullWritable.class);
    job.setOutputValueClass(DiqubeRow.class);

    for (int i = 0; i < args.length - 1; i++)
      FileInputFormat.addInputPath(job, new Path(args[i]));
    DiqubeOutputFormat.setOutputPath(job, new Path(args[args.length - 1]));
    System.exit(job.waitForCompletion(true) ? 0 : 1);
  }
}
