package hex.genmodel;

import com.oracle.tools.packager.IOUtils;
import hex.genmodel.easy.EasyPredictModelWrapper;
import hex.genmodel.easy.RowData;
import hex.genmodel.easy.exception.PredictException;
import hex.genmodel.easy.prediction.RegressionModelPrediction;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.*;

public class MojoPipelineWriterTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testAnything() throws IOException, PredictException {
    LinkedHashMap<String, String> files = new LinkedHashMap<>();
    files.put("clustering", "/Users/mkurka/mojos/kmeans_model.zip");
    files.put("glm", "/Users/mkurka/mojos/glm_model.zip");

    MojoModel kmeans = MojoModel.load(files.get("clustering"));
    MojoModel glm = MojoModel.load(files.get("glm"));

    Map<String, MojoModel> models = new HashMap<>();
    models.put("clustering", kmeans);
    models.put("glm", glm);

    Map<String, String> mapping = Collections.singletonMap("CLUSTER", "clustering:0");

    MojoPipelineWriter w = new MojoPipelineWriter(models, mapping, "glm");

    File mojoZipFile = tmp.newFile("mojo-pipeline.zip");
    try (FileOutputStream fos = new FileOutputStream(mojoZipFile);
         ZipOutputStream zos = new ZipOutputStream(fos)) {
      w.writeTo(zos);
      for (Map.Entry<String, String> mojoFile : files.entrySet()) {
        ZipFile zf = new ZipFile(mojoFile.getValue());
        Enumeration<? extends ZipEntry> entries = zf.entries();
        while (entries.hasMoreElements()) {
          ZipEntry ze = entries.nextElement();

          ZipEntry copy = new ZipEntry("models/" + mojoFile.getKey() + "/" + ze.getName());
          if (copy.getSize() >= 0) {
            copy.setSize(copy.getSize());
          }
          copy.setTime(copy.getTime());
          zos.putNextEntry(copy);
          try (InputStream input = zf.getInputStream(zf.getEntry(ze.getName()))) {
            copyStream(input, zos);
          }
          zos.closeEntry();
        }
      }
    }

    MojoModel mojoPipeline = MojoModel.load(mojoZipFile.getAbsolutePath());
    EasyPredictModelWrapper mojoPipelineWr = new EasyPredictModelWrapper(mojoPipeline);
    RowData rd = new RowData();
    rd.put("AGE", 71.0);
    rd.put("RACE", "1");
    rd.put("DPROS", 3.0);
    rd.put("DCAPS", 2.0);
    rd.put("PSA", 3.3);
    rd.put("VOL", 0.0);
    rd.put("GLEASON", 8.0);

    RegressionModelPrediction p = (RegressionModelPrediction) mojoPipelineWr.predict(rd);
    System.out.println(p.value);

    IOUtils.copyFile(mojoZipFile, new File("/Users/mkurka/mojos/mojo-pipeline.zip"));
  }

  private static void copyStream(InputStream source, OutputStream target) throws IOException {
    byte[] buffer = new byte[8 * 1024];
    while (true) {
      int len = source.read(buffer);
      if (len == -1)
        break;
      target.write(buffer, 0, len);
    }
  }


}