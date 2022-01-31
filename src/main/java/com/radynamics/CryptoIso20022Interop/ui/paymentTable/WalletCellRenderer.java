package com.radynamics.CryptoIso20022Interop.ui.paymentTable;

import com.radynamics.CryptoIso20022Interop.ui.Consts;
import com.radynamics.CryptoIso20022Interop.ui.Utils;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class WalletCellRenderer extends JLabel implements TableCellRenderer {
    private final JPanel pnl;
    private final JLabel wallet;
    private final JLabel desc;

    public WalletCellRenderer() {
        pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        wallet = new JLabel();
        pnl.add(wallet);

        desc = Utils.formatSecondaryInfo(new JLabel());
        pnl.add(desc);

        setOpaque(true);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        pnl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        wallet.setForeground(isSelected ? table.getSelectionForeground() : table.getForeground());
        desc.setForeground(isSelected ? table.getSelectionForeground() : Consts.ColorSmallInfo);

        if (value instanceof WalletCellValue) {
            var cellValue = (WalletCellValue) value;
            wallet.setText(cellValue.getWallet() == null ? "" : cellValue.getWallet().getPublicKey());
            desc.setText(cellValue.getWalletInfo() == null ? "" : String.format("%s %s", cellValue.getWalletInfo().getText(), cellValue.getWalletInfo().getValue()));
        } else {
            wallet.setText((String) value);
            desc.setText("");
        }
        return pnl;
    }
}
