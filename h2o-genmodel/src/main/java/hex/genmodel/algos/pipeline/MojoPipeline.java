package hex.genmodel.algos.pipeline;

import hex.genmodel.MojoModel;

public class MojoPipeline extends MojoModel {

  public MojoPipeline(String[] columns, String[][] domains, String responseColumn) {
    super(columns, domains, responseColumn);
  }

  @Override
  public double[] score0(double[] row, double[] preds) {
    return preds; // FIXME
  }

}
