package jp.hotbrain.db.migration;

import scala.collection.Seq;

public interface MultiRule extends SingleRule {
    Seq<String> schemas(String schemaName);
}
