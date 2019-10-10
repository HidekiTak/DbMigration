package jp.hotbrain.db.migration;

import java.sql.Connection;

public interface SingleRule {
    Connection connectionFor(String schemaName);
}
