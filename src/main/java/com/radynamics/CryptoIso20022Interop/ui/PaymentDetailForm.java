package com.radynamics.CryptoIso20022Interop.ui;

import com.alexandriasoftware.swing.JSplitButton;
import com.alexandriasoftware.swing.action.SplitButtonClickedActionListener;
import com.radynamics.CryptoIso20022Interop.DateTimeConvert;
import com.radynamics.CryptoIso20022Interop.MoneyFormatter;
import com.radynamics.CryptoIso20022Interop.cryptoledger.LookupProviderException;
import com.radynamics.CryptoIso20022Interop.cryptoledger.LookupProviderFactory;
import com.radynamics.CryptoIso20022Interop.cryptoledger.PaymentPath;
import com.radynamics.CryptoIso20022Interop.cryptoledger.WalletInfoAggregator;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.ValidationResultUtils;
import com.radynamics.CryptoIso20022Interop.exchange.CurrencyConverter;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRate;
import com.radynamics.CryptoIso20022Interop.exchange.ExchangeRateProvider;
import com.radynamics.CryptoIso20022Interop.exchange.Money;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;
import com.radynamics.CryptoIso20022Interop.iso20022.PaymentValidator;
import com.radynamics.CryptoIso20022Interop.ui.paymentTable.Actor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class PaymentDetailForm extends JDialog {
    private Payment payment;
    private PaymentValidator validator;
    private ExchangeRateProvider exchangeRateProvider;
    private final WalletInfoAggregator walletInfoAggregator;
    private final CurrencyConverter currencyConverter;
    private final Actor actor;

    private SpringLayout panel1Layout;
    private JPanel pnlContent;
    private Component anchorComponentTopLeft;
    private boolean paymentChanged;
    private JLabel lblLedgerAmount;
    private MoneyLabel lblAmountText;
    private JLabel lblEditExchangeRate;
    private JSplitButton cmdPaymentPath;

    public PaymentDetailForm(Payment payment, PaymentValidator validator, ExchangeRateProvider exchangeRateProvider, CurrencyConverter currencyConverter, Actor actor) {
        if (payment == null) throw new IllegalArgumentException("Parameter 'payment' cannot be null");
        if (validator == null) throw new IllegalArgumentException("Parameter 'validator' cannot be null");
        if (exchangeRateProvider == null) throw new IllegalArgumentException("Parameter 'exchangeRateProvider' cannot be null");
        if (currencyConverter == null) throw new IllegalArgumentException("Parameter 'currencyConverter' cannot be null");
        this.payment = payment;
        this.validator = validator;
        this.exchangeRateProvider = exchangeRateProvider;
        this.walletInfoAggregator = new WalletInfoAggregator(payment.getLedger().getInfoProvider());
        this.currencyConverter = currencyConverter;
        this.actor = actor;

        setupUI();
    }

    private void setupUI() {
        setTitle("Payment detail");
        setIconImage(Utils.getProductIcon());

        var al = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        };
        getRootPane().registerKeyboardAction(al, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        var pnlMain = new JPanel();
        pnlMain.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(pnlMain);

        pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.Y_AXIS));

        var innerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        JPanel panel0 = new JPanel();
        panel0.setBorder(innerBorder);
        panel0.setLayout(new BoxLayout(panel0, BoxLayout.X_AXIS));
        var panel1 = new JPanel();
        panel1.setBorder(innerBorder);
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
        panel1Layout = new SpringLayout();
        pnlContent = new JPanel();
        pnlContent.setLayout(panel1Layout);
        JPanel panel3 = new JPanel();
        var panel3Layout = new SpringLayout();
        panel3.setLayout(panel3Layout);

        panel1.add(pnlContent);

        pnlMain.add(panel0);
        pnlMain.add(panel1);
        pnlMain.add(panel3);

        panel0.setMinimumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel0.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel0.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));
        panel3.setMinimumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        panel3.setPreferredSize(new Dimension(Integer.MAX_VALUE, 50));

        {
            var lbl = new JLabel();
            lbl.setText(getTitle());
            lbl.putClientProperty("FlatLaf.style", "font: 200% $semibold.font");
            lbl.setOpaque(true);
            panel0.add(lbl);
        }

        {
            int row = 0;
            {
                var secondLine = new JPanel();
                secondLine.setLayout(new BoxLayout(secondLine, BoxLayout.X_AXIS));
                var pnlFirstLine = new JPanel();
                pnlFirstLine.setLayout(new BoxLayout(pnlFirstLine, BoxLayout.LINE_AXIS));
                lblAmountText = new MoneyLabel(payment.getLedger());
                pnlFirstLine.add(lblAmountText);
                pnlFirstLine.add(Box.createRigidArea(new Dimension(10, 0)));
                {
                    JMenuItem selected = null;
                    var popupMenu = new JPopupMenu();
                    var availablePaths = payment.getLedger().createPaymentPathFinder().find(currencyConverter, payment);
                    for (var path : availablePaths) {
                        var item = new JMenuItem(path.getDisplayText());
                        selected = path.isSet(payment) ? item : selected;
                        popupMenu.add(item);
                        item.addActionListener((SplitButtonClickedActionListener) e -> apply(path));
                    }
                    cmdPaymentPath = new JSplitButton();
                    if (selected != null) {
                        popupMenu.setSelected(selected);
                        refreshPaymentPathText(selected.getText());
                    }
                    cmdPaymentPath.setVisible(actor == Actor.Sender);
                    cmdPaymentPath.setEnabled(availablePaths.length > 1 && payment.getBooked() == null);
                    cmdPaymentPath.setAlwaysPopup(true);
                    cmdPaymentPath.setPopupMenu(popupMenu);
                    cmdPaymentPath.setPreferredSize(new Dimension(170, 21));
                    pnlFirstLine.add(cmdPaymentPath);
                }
                lblLedgerAmount = Utils.formatSecondaryInfo(new JLabel());

                var enabled = payment.getExchangeRate() == null || !payment.getExchangeRate().isNone();
                lblEditExchangeRate = formatSecondLineLinkLabel(Utils.createLinkLabel(pnlContent, "edit...", enabled));
                refreshAmountsText();
                secondLine.add(lblLedgerAmount);
                {
                    lblEditExchangeRate.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (enabled && e.getClickCount() == 1) {
                                showExchangeRateEdit();
                            }
                        }
                    });
                    lblEditExchangeRate.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
                    secondLine.add(lblEditExchangeRate);
                }
                anchorComponentTopLeft = createRow(row++, "Amount:", pnlFirstLine, secondLine, false, 7);
            }
            {
                var lbl = new WalletLabel();
                lbl.setWallet(payment.getSenderWallet())
                        .setLedger(payment.getLedger())
                        .setAccount(payment.getSenderAccount())
                        .setAddress(payment.getSenderAddress())
                        .setWalletInfoAggregator(walletInfoAggregator);
                createRow(row++, "Sender:", lbl, null, false);
            }
            {
                var lbl = new WalletLabel();
                lbl.setWallet(payment.getReceiverWallet())
                        .setLedger(payment.getLedger())
                        .setAccount(payment.getReceiverAccount())
                        .setAddress(payment.getReceiverAddress())
                        .setWalletInfoAggregator(walletInfoAggregator);
                createRow(row++, "Receiver:", lbl, null, false);
            }
            {
                JLabel secondLine = null;
                if (payment.getId() != null) {
                    secondLine = formatSecondLineLinkLabel(Utils.createLinkLabel(pnlContent, "show ledger transaction..."));
                    secondLine.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            if (e.getClickCount() == 1) {
                                showLedgerTransaction();
                            }
                        }
                    });
                }
                createRow(row++, "Booked:", payment.getBooked() == null ? "unknown" : Utils.createFormatDate().format(DateTimeConvert.toUserTimeZone(payment.getBooked())), secondLine);
            }
            {
                var sb = new StringBuilder();
                for (var ref : payment.getStructuredReferences()) {
                    sb.append(String.format("%s\n", ref.getUnformatted()));
                }
                var txt = createTextArea(1, Utils.removeEndingLineSeparator(sb.toString()));
                createRow(row++, "References:", txt, null);
            }
            {
                var sb = new StringBuilder();
                for (var m : payment.getMessages()) {
                    sb.append(String.format("%s\n", m));
                }
                var txt = createTextArea(3, Utils.removeEndingLineSeparator(sb.toString()));
                createRow(row++, "Messages:", txt, null);
                row++;
            }
            {
                var sb = new StringBuilder();
                if (payment.getTransmissionError() != null) {
                    sb.append(String.format("%s\n", payment.getTransmissionError().getMessage()));
                }
                var validations = validator.validate(payment);
                ValidationResultUtils.sortDescending(validations);
                for (var vr : validations) {
                    sb.append(String.format("- [%s] %s\n", vr.getStatus().name(), vr.getMessage()));
                }
                var pnl = new JPanel();
                pnl.setLayout(new BorderLayout());
                pnl.add(createTextArea(3, sb.length() == 0 ? "" : Utils.removeEndingLineSeparator(sb.toString())));
                createRow(row++, "Issues:", pnl, null, true);
            }
        }
        {
            var cmd = new JButton("Close");
            cmd.setPreferredSize(new Dimension(150, 35));
            cmd.addActionListener(e -> {
                close();
            });
            panel3Layout.putConstraint(SpringLayout.EAST, cmd, 0, SpringLayout.EAST, panel3);
            panel3Layout.putConstraint(SpringLayout.SOUTH, cmd, 0, SpringLayout.SOUTH, panel3);
            panel3.add(cmd);
        }
    }

    private void apply(PaymentPath path) {
        path.apply(payment);
        refreshPaymentPathText(path.getDisplayText());
        refreshAmountsText();
        setPaymentChanged(true);
    }

    private void refreshPaymentPathText(String selectedText) {
        cmdPaymentPath.setText(String.format("send using %s", selectedText));
    }

    private void refreshAmountsText() {
        lblAmountText.setAmount(Money.of(payment.getAmount(), payment.getUserCcy()));

        lblLedgerAmount.setVisible(!payment.isUserCcyEqualTransactionCcy());
        lblEditExchangeRate.setVisible(!payment.isUserCcyEqualTransactionCcy());

        var amtLedgerText = MoneyFormatter.formatLedger(payment.getAmountTransaction());
        if (payment.getExchangeRate() == null) {
            lblLedgerAmount.setText(String.format("%s, missing exchange rate", amtLedgerText));
            return;
        }

        var fxRateText = "unknown";
        var fxRateAtText = "unknown";
        if (!payment.isAmountUnknown()) {
            fxRateText = Utils.createFormatLedger().format(payment.getExchangeRate().getRate());
            fxRateAtText = Utils.createFormatDate().format(DateTimeConvert.toUserTimeZone(payment.getExchangeRate().getPointInTime()));
        }
        lblLedgerAmount.setText(String.format("%s with exchange rate %s at %s", amtLedgerText, fxRateText, fxRateAtText));
    }

    private void showExchangeRateEdit() {
        var rate = payment.getExchangeRate() == null ? ExchangeRate.Undefined(payment.createCcyPair()) : payment.getExchangeRate();

        var frm = new ExchangeRatesForm(exchangeRateProvider, new ExchangeRate[]{rate}, rate.getPointInTime());
        frm.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frm.setSize(400, 300);
        frm.setModal(true);
        frm.setLocationRelativeTo(this);
        frm.setVisible(true);

        if (!frm.isDialogAccepted()) {
            return;
        }

        payment.setExchangeRate(rate.isUndefined() ? null : rate);
        payment.refreshAmounts();
        refreshAmountsText();
        setPaymentChanged(true);
    }

    private void showLedgerTransaction() {
        try {
            var lp = LookupProviderFactory.createTransactionLookupProvider(payment.getLedger());
            lp.open(payment.getId());
        } catch (LookupProviderException ex) {
            ExceptionDialog.show(this, ex);
        }
    }

    private JScrollPane createTextArea(int rows, String text) {
        var txt = new JTextArea(rows, 39);
        txt.setLineWrap(true);
        txt.setEditable(false);
        txt.setText(text);
        txt.setCaretPosition(0);
        return new JScrollPane(txt);
    }

    private void close() {
        dispose();
    }

    private Component createRow(int row, String labelText, Component firstLine, String contentSecondLine) {
        JLabel secondLine = null;
        if (contentSecondLine != null) {
            secondLine = new JLabel(contentSecondLine);
            Utils.formatSecondaryInfo(secondLine);
        }
        return createRow(row, labelText, firstLine, secondLine, false);
    }

    private Component createRow(int row, String labelText, String contentFirstLine, Component secondLine) {
        return createRow(row, labelText, new JLabel(contentFirstLine), secondLine, false);
    }

    private Component createRow(int row, String labelText, Component firstLine, Component secondLine, boolean growBottomRight) {
        return createRow(row, labelText, firstLine, secondLine, growBottomRight, 0);
    }

    private Component createRow(int row, String labelText, Component firstLine, Component secondLine, boolean growBottomRight, int secondLineNorthOffset) {
        var lbl = new JLabel(labelText);
        panel1Layout.putConstraint(SpringLayout.WEST, lbl, 0, SpringLayout.WEST, pnlContent);
        panel1Layout.putConstraint(SpringLayout.NORTH, lbl, getNorthPad(row), SpringLayout.NORTH, pnlContent);
        lbl.setOpaque(true);
        pnlContent.add(lbl);

        panel1Layout.putConstraint(SpringLayout.WEST, firstLine, 50, SpringLayout.EAST, anchorComponentTopLeft == null ? lbl : anchorComponentTopLeft);
        panel1Layout.putConstraint(SpringLayout.NORTH, firstLine, getNorthPad(row), SpringLayout.NORTH, pnlContent);
        if (growBottomRight) {
            panel1Layout.putConstraint(SpringLayout.EAST, pnlContent, 0, SpringLayout.EAST, firstLine);
            panel1Layout.putConstraint(SpringLayout.SOUTH, pnlContent, 0, SpringLayout.SOUTH, firstLine);
        }
        pnlContent.add(firstLine);

        if (secondLine != null) {
            panel1Layout.putConstraint(SpringLayout.WEST, secondLine, 50, SpringLayout.EAST, anchorComponentTopLeft == null ? lbl : anchorComponentTopLeft);
            panel1Layout.putConstraint(SpringLayout.NORTH, secondLine, getNorthPad(row) + 13 + secondLineNorthOffset, SpringLayout.NORTH, pnlContent);
            pnlContent.add(secondLine);
        }

        return lbl;
    }

    private JLabel formatSecondLineLinkLabel(JLabel lbl) {
        lbl.putClientProperty("FlatLaf.styleClass", "small");
        return lbl;
    }

    private static int getNorthPad(int line) {
        final var lineHeight = 30;
        return line * lineHeight;
    }

    private void setPaymentChanged(boolean changed) {
        this.paymentChanged = changed;
    }

    public boolean getPaymentChanged() {
        return paymentChanged;
    }
}
