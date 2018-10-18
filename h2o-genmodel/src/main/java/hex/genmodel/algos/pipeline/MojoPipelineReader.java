package hex.genmodel.algos.pipeline;

import hex.genmodel.MultiModelMojoReader;

import java.io.IOException;

public class MojoPipelineReader extends MultiModelMojoReader<MojoPipeline> {

  @Override
  public String getModelName() {
    return "MOJO Pipeline";
  }

  @Override
  protected void readParentModelData() throws IOException {
    // FIXME
  }

  @Override
  protected MojoPipeline makeModel(String[] columns, String[][] domains, String responseColumn) {
    return new MojoPipeline(columns, domains, responseColumn);
  }

}
