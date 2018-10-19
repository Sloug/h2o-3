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
    _model._inputMapping = findIndices(_model._names, _model._mainModel._names, readGeneratedColumns());

    _model._models = new MojoPipeline.PipelineSubModel[getSubModels().size() - 1];
    int modelsCnt = 0;
    for (Map.Entry<String, MojoModel> subModel : getSubModels().entrySet()) {
      if (mainModelAlias.equals(subModel.getKey())) {
        continue;
      }
      MojoModel m = subModel.getValue();
      MojoPipeline.PipelineSubModel psm = _model._models[modelsCnt++] = new MojoPipeline.PipelineSubModel();
      psm._inputMapping = findIndices(_model._names, m._names, new String[0]);
      psm._predsSize = m.getPredsSize(m.getModelCategory());
      psm._sourcePredsIndices = new int[0]; // FIXME
      psm._targetRowIndices = new int[0];
      psm._mojoModel = m;
    }
    assert modelsCnt == _model._models.length;
  }

  private String[] readGeneratedColumns() {
    final int cnt = readkv("generated_column_count", 0);
    final String[] names = new String[cnt];
    for (int i = 0; i < names.length; i++) {
      names[i] = readkv("generated_column_name_" + i, "");
    }
    return names;
  }

  @Override
  protected MojoPipeline makeModel(String[] columns, String[][] domains, String responseColumn) {
    return new MojoPipeline(columns, domains, responseColumn);
  }

  private static int[] findIndices(String[] strings, String[] subset, String[] ignored) {
    final int[] idx = new int[subset.length - ignored.length];
    int cnt = 0;
    outer: for (int i = 0; i < idx.length; i++) {
      final String s = subset[i];
      assert s != null;
      for (String si : ignored) {
        if (s.equals(si)) {
          continue outer;
        }
      }
      for (int j = 0; j < strings.length; j++) {
        if (s.equals(strings[j])) {
          idx[cnt++] = j;
          continue outer;
        }
      }
      throw new IllegalStateException("Pipeline doesn't have input column '" + subset[i] + "'.");
    }
    return idx;
  }

}
