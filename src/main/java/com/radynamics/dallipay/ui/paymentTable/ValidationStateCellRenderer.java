package com.radynamics.dallipay.ui.paymentTable;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.radynamics.dallipay.cryptoledger.transaction.ValidationResult;
import com.radynamics.dallipay.cryptoledger.transaction.ValidationResultUtils;
import com.radynamics.dallipay.cryptoledger.transaction.ValidationState;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ResourceBundle;

public class ValidationStateCellRenderer extends JLabel implements TableCellRenderer {
    private TableColumn validationResultsColumn;

    private final static int WIDTH = 16;
    private final static int HEIGHT = 16;

    private final static FlatSVGIcon ok = null;
    private final static FlatSVGIcon info = new FlatSVGIcon("svg/informationDialog.svg", WIDTH, HEIGHT);
    private final static FlatSVGIcon warning = new FlatSVGIcon("svg/warningDialog.svg", WIDTH, HEIGHT);
    private final static FlatSVGIcon error = new FlatSVGIcon("svg/errorDialog.svg", WIDTH, HEIGHT);

    private final ResourceBundle res = ResourceBundle.getBundle("i18n.Various");

    public ValidationStateCellRenderer(TableColumn validationResultsColumn) {
        this.validationResultsColumn = validationResultsColumn;
        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        setHorizontalAlignment(JLabel.CENTER);

        if (value == null) {
            setText("...");
            setIcon(null);
            setToolTipText(res.getString("validating"));
            return this;
        }

        setText(null);
        var status = (ValidationState) value;
        var obj = (ValidationResult[]) table.getModel().getValueAt(row, validationResultsColumn.getModelIndex());
        ValidationResultUtils.sortDescending(obj);
        setIcon(getIconOrNull(status));
        setToolTipText(createToolTipText(obj));
        return this;
    }

    private String createToolTipText(ValidationResult[] results) {
        var sb = new StringBuilder();
        for (var i = 0; i < results.length; i++) {
            sb.append(String.format("- %s", results[i].getMessage()));
            if (i < results.length - 1) {
                sb.append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private static Icon getIconOrNull(ValidationState status) {
        switch (status) {
            case Ok -> {
                return ok;
            }
            case Info -> {
                return info;
            }
            case Warning -> {
                return warning;
            }
            case Error -> {
                return error;
            }

            default -> throw new IllegalStateException("Unexpected value: " + status);
        }
    }
}
