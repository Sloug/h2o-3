package hex.genmodel;

import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class MojoPipelineBuilder {

  private final Map<String, File> _files = new HashMap<>();
  private final Map<String, MojoModel> _models = new HashMap<>();
  private final Map<String, String> _mapping = new HashMap<>();
  private String _mainModelAlias;

  public MojoPipelineBuilder addModel(String modelAlias, File mojoFile) throws IOException {
    MojoModel model = MojoModel.load(mojoFile.getAbsolutePath());
    _files.put(modelAlias, mojoFile);
    _models.put(modelAlias, model);
    return this;
  }

  public MojoPipelineBuilder addMainModel(String modelAlias, File mojoFile) throws IOException {
    addModel(modelAlias, mojoFile);
    _mainModelAlias = modelAlias;
    return this;
  }

  public MojoPipelineBuilder addMapping(String columnName, String sourceModelAlias, int sourceModelPredictionIndex) {
    _mapping.put(columnName, sourceModelAlias + ":" + sourceModelPredictionIndex);
    return this;
  }

  public void buildPipeline(File pipelineFile) throws IOException {
    MojoPipelineWriter w = new MojoPipelineWriter(_models, _mapping, _mainModelAlias);

    try (FileOutputStream fos = new FileOutputStream(pipelineFile);
         ZipOutputStream zos = new ZipOutputStream(fos)) {
      w.writeTo(zos);
      for (Map.Entry<String, File> mojoFile : _files.entrySet()) {
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
