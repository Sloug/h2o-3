package hex.genmodel;

import hex.ModelCategory;
import sun.awt.image.ImageWatched;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class MojoPipelineWriter extends AbstractMojoWriter {

  private Map<String, MojoModel> _models;
  private Map<String, String> _inputMapping;
  private MojoModel _finalModel;

  public MojoPipelineWriter(Map<String, MojoModel> models, Map<String, String> inputMapping, MojoModel finalModel) {
    super(makePipelineDescriptor(models, inputMapping, finalModel));
    _models = models;
    _inputMapping = inputMapping;
    _finalModel = finalModel;
  }

  @Override
  public String mojoVersion() {
    return "1.00";
  }

  @Override
  protected void writeModelData() throws IOException {
    writekv("submodel_count", _models.size() + 1);
    int modelNum = 0;
    for (Map.Entry<String, MojoModel> model : _models.entrySet()) {
      writekv("submodel_key_" + modelNum, model.getKey());
      writekv("submodel_dir_" + modelNum, "models/" + model.getKey() + "/");
      modelNum++;
    }
    writekv("submodel_key_" + modelNum, "final_model");
    writekv("submodel_dir_" + modelNum, "final_model/");
  }

  private static MojoPipelineDescriptor makePipelineDescriptor(
          Map<String, MojoModel> models, Map<String, String> inputMapping, MojoModel finalModel) {
    LinkedHashMap<String, String[]> schema = deriveInputSchema(models, inputMapping, finalModel);
    return new MojoPipelineDescriptor(schema, finalModel);
  }

  private static LinkedHashMap<String, String[]> deriveInputSchema(
          Map<String, MojoModel> models, Map<String, String> inputMapping, MojoModel finalModel) {
    LinkedHashMap<String, String[]> schema = new LinkedHashMap<>();

    for (MojoModel model : models.values()) {
      for (int i = 0; i < model.nfeatures(); i++) {
        String fName = model._names[i];
        if (schema.containsKey(fName)) { // make sure the domain matches
          // TODO
        } else {
          schema.put(fName, model._domains[i]);
        }
      }
    }

    for (int i = 0; i < finalModel._names.length; i++) { // we include the response of the final model as well
      String fName = finalModel._names[i];
      if (! inputMapping.containsKey(fName)) {
        schema.put(fName, finalModel._domains[i]);
      }
    }

    return schema;
  }

  private static class MojoPipelineDescriptor implements ModelDescriptor {

    private final MojoModel _finalModel;
    private final String[] _names;
    private final String[][] _domains;

    private MojoPipelineDescriptor(LinkedHashMap<String, String[]> schema, MojoModel finalModel) {
      _finalModel = finalModel;
      _names = new String[schema.size()];
      _domains = new String[schema.size()][];
      int i = 0;
      for (Map.Entry<String, String[]> field : schema.entrySet()) {
        _names[i] = field.getKey();
        _domains[i] = field.getValue();
        i++;
      }
    }

    @Override
    public String[][] scoringDomains() {
      return _domains;
    }

    @Override
    public String projectVersion() {
      return "3.22.0.1"; // FIXME
    }

    @Override
    public String algoName() {
      return "pipeline";
    }

    @Override
    public String algoFullName() {
      return "MOJO Pipeline";
    }

    @Override
    public ModelCategory getModelCategory() {
      return _finalModel._category;
    }

    @Override
    public boolean isSupervised() {
      return _finalModel.isSupervised();
    }

    @Override
    public int nfeatures() {
      return _finalModel.nfeatures();
    }

    @Override
    public int nclasses() {
      return _finalModel.nclasses();
    }

    @Override
    public String[] columnNames() {
      return _names;
    }

    @Override
    public boolean balanceClasses() {
      return _finalModel._balanceClasses;
    }

    @Override
    public double defaultThreshold() {
      return _finalModel._defaultThreshold;
    }

    @Override
    public double[] priorClassDist() {
      return _finalModel._priorClassDistrib;
    }

    @Override
    public double[] modelClassDist() {
      return _finalModel._modelClassDistrib;
    }

    @Override
    public long checksum() {
      return -1;
    }

    @Override
    public String timestamp() {
      return String.valueOf(new Date().getTime());
    }
  }

}
