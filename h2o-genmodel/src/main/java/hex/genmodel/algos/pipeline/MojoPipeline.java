package hex.genmodel.algos.pipeline;

import hex.genmodel.MojoModel;

import java.io.Serializable;
import java.util.Arrays;

public class MojoPipeline extends MojoModel {

  MojoModel _mainModel;
  int[] _inputMapping;
  int _outputMappingLength;


  PipelineSubModel[] _models;

  public MojoPipeline(String[] columns, String[][] domains, String responseColumn) {
    super(columns, domains, responseColumn);
  }

  @Override
  public double[] score0(double[] row, double[] preds) {
    double[] mainInputRow = new double[_inputMapping.length + _outputMappingLength];
    for (int i = 0; i < _inputMapping.length; i++) {
      mainInputRow[i] = row[_inputMapping[i]];
    }

    for (PipelineSubModel psm : _models) {
      double[] rowSubset = new double[psm._inputMapping.length];
      for (int i = 0; i < psm._inputMapping.length; i++) {
        rowSubset[i] = row[psm._inputMapping[i]];
      }
      double[] subModelPreds = new double[psm._predsSize];
      subModelPreds = psm._mojoModel.score0(rowSubset, subModelPreds);
      System.out.println(Arrays.toString(subModelPreds));
      for (int j = 0; j < psm._sourcePredsIndices.length; j++) {
        mainInputRow[psm._targetRowIndices[j]] = subModelPreds[psm._sourcePredsIndices[j]];
      }
    }

    return _mainModel.score0(mainInputRow, preds);
  }

  static class PipelineSubModel implements Serializable {
    int[] _inputMapping;
    int _predsSize;
    int[] _sourcePredsIndices;
    int[] _targetRowIndices;
    MojoModel _mojoModel;
  }

}
