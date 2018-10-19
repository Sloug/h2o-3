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

import static org.junit.Assert.*;

public class MojoPipelineWriterTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testAnything() throws IOException, PredictException {
    File mojoZipFile = tmp.newFile("mojo-pipeline.zip");

    MojoPipelineBuilder builder = new MojoPipelineBuilder();
    builder
            .addModel("clustering", new File("/Users/mkurka/mojos/kmeans_model.zip"))
            .addMapping("CLUSTER", "clustering", 0)
            .addMainModel("regression", new File("/Users/mkurka/mojos/glm_model.zip"))
            .buildPipeline(mojoZipFile);

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
    assertEquals(0.7812266, p.value, 1e-7);

    IOUtils.copyFile(mojoZipFile, new File("/Users/mkurka/mojos/mojo-pipeline.zip"));
  }

}