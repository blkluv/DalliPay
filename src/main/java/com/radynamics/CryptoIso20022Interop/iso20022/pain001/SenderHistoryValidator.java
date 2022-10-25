package com.radynamics.CryptoIso20022Interop.iso20022.pain001;

import com.radynamics.CryptoIso20022Interop.DateTimeConvert;
import com.radynamics.CryptoIso20022Interop.cryptoledger.*;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.ValidationResult;
import com.radynamics.CryptoIso20022Interop.cryptoledger.transaction.ValidationState;
import com.radynamics.CryptoIso20022Interop.iso20022.Payment;
import com.radynamics.CryptoIso20022Interop.iso20022.Utils;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;

public class SenderHistoryValidator implements WalletHistoryValidator {
    private final Cache<PaymentHistoryProvider> cache;
    private final DateTimeFormatter df = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM);

    public SenderHistoryValidator(NetworkInfo network) {
        cache = new Cache<>(network.getUrl().toString(), ChronoUnit.FOREVER.getDuration());
    }

    public ValidationResult[] validate(Payment[] payments) {
        var list = new ArrayList<ValidationResult>();

        for (var senderWallet : PaymentUtils.distinctSendingWallets(payments)) {
            for (var p : PaymentUtils.fromSender(senderWallet, payments)) {
                list.addAll(Arrays.asList(validate(p)));
            }
        }

        return list.toArray(new ValidationResult[0]);
    }

    public ValidationResult[] validate(Payment p) {
        if (p.getSenderWallet() == null) {
            return new ValidationResult[0];
        }

        var list = new ArrayList<ValidationResult>();
        var ledger = p.getLedger();
        if (!ledger.isValidPublicKey(p.getSenderWallet().getPublicKey())) {
            return new ValidationResult[0];
        }

        synchronized (this) {
            if (!cache.isPresent(p.getSenderWallet())) {
                loadHistory(ledger, p.getSenderWallet());
            }
        }

        var paymentHistory = cache.get(p.getSenderWallet());
        var similar = paymentHistory.oldestSimilarOrDefault(p);
        if (similar != null) {
            list.add(new ValidationResult(ValidationState.Warning, String.format("Similar payment sent to same receiver at %s.", df.format(DateTimeConvert.toUserTimeZone(similar.getBooked())))));
        }

        return list.toArray(new ValidationResult[0]);
    }

    public void clearCache() {
        cache.clear();
    }

    public void loadHistory(Ledger ledger, Wallet wallet) {
        if (wallet == null) {
            return;
        }

        if (!cache.isPresent(wallet)) {
            var paymentHistory = ledger.getPaymentHistoryProvider();

            var desired = ZonedDateTime.now().minusDays(40);
            var availableSince = ledger.getNetwork().historyAvailableSince();
            var since = desired.isBefore(availableSince) ? availableSince : desired;

            // Use endOfDay to ensure data until latest ledger is loaded. Ignoring time improves cache hits.
            var sinceDaysAgo = Duration.between(Utils.endOfDay(since), ZonedDateTime.now()).toDays();
            paymentHistory.load(ledger, wallet, sinceDaysAgo);
            cache.add(wallet, paymentHistory);
        }
    }
}