package io.cockroachdb.pool.proxy.sandbox.util;

import org.springframework.shell.jline.tui.table.BorderStyle;
import org.springframework.shell.jline.tui.table.TableBuilder;
import org.springframework.shell.jline.tui.table.TableModel;

public abstract class TableUtils {
    private TableUtils() {
    }

    public static String prettyPrint(TableModel model) {
        TableBuilder tableBuilder = new TableBuilder(model);
        tableBuilder.addInnerBorder(BorderStyle.fancy_light);
        tableBuilder.addHeaderBorder(BorderStyle.fancy_double);
        return tableBuilder
                .build().render(120);
    }
}
