package hex.genmodel.algos.pipeline;

import hex.genmodel.MojoModel;
import hex.genmodel.MultiModelMojoReader;

import java.io.IOException;
import java.util.Map;

public class MojoPipelineReader extends MultiModelMojoReader<MojoPipeline> {

  @Override
  public String getModelName() {
    return "MOJO Pipeline";
  }

  @Override
  protected void readParentModelData() throws IOException {
    String mainModelAlias = readkv("main_model");
    _model._mainModel = getModel(mainModelAlias);
    _model._inputMapping = findIndices(_model._names, _model._mainModel._names);

    _model._models = new MojoPipeline.PipelineSubModel[getSubModels().size() - 1];
    int modelsCnt = 0;
    for (Map.Entry<String, MojoModel> subModel : getSubModels().entrySet()) {
      if (mainModelAlias.equals(subModel.getKey())) {
        continue;
      }
      MojoModel m = subModel.getValue();
      MojoPipeline.PipelineSubModel psm = _model._models[modelsCnt++] = new MojoPipeline.PipelineSubModel();
      psm._inputMapping = findIndices(_model._names, m._names);
      psm._predsSize = m.getPredsSize(m.getModelCategory());
      psm._sourcePredsIndices = new int[0];
      psm._targetRowIndices = new int[0];
      psm._mojoModel = m;
    }
    assert modelsCnt == _model._models.length;
  }

  @Override
  protected MojoPipeline makeModel(String[] columns, String[][] domains, String responseColumn) {
    return new MojoPipeline(columns, domains, responseColumn);
  }

  private static int[] findIndices(String[] strings, String[] subset) {
    int[] idx = new int[subset.length];
    outer: for (int i = 0; i < idx.length; i++) {
      for (int j = 0; j < strings.length; j++) {
        if (subset[i].equals(strings[j])) {
          idx[i] = j;
          continue outer;
        }
      }
      throw new IllegalStateException("Pipeline doesn't have input column '" + subset[i] + "'.");
    }
    return idx;
  }

}
