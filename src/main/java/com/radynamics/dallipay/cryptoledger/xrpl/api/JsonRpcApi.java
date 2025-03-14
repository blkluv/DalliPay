package com.radynamics.dallipay.cryptoledger.xrpl.api;

import com.google.common.primitives.UnsignedInteger;
import com.radynamics.dallipay.DateTimeRange;
import com.radynamics.dallipay.cryptoledger.*;
import com.radynamics.dallipay.cryptoledger.generic.Wallet;
import com.radynamics.dallipay.cryptoledger.memo.PayloadConverter;
import com.radynamics.dallipay.cryptoledger.signing.PrivateKeyProvider;
import com.radynamics.dallipay.cryptoledger.signing.TransactionSubmitter;
import com.radynamics.dallipay.cryptoledger.xrpl.Ledger;
import com.radynamics.dallipay.cryptoledger.xrpl.Transaction;
import com.radynamics.dallipay.cryptoledger.xrpl.*;
import com.radynamics.dallipay.cryptoledger.xrpl.api.xrpl4j.ImmutableBookOffersRequestParams;
import com.radynamics.dallipay.cryptoledger.xrpl.api.xrpl4j.ImmutableBookOffersResult;
import com.radynamics.dallipay.cryptoledger.xrpl.api.xrpl4j.ImmutableIssuedCurrency;
import com.radynamics.dallipay.cryptoledger.xrpl.api.xrpl4j.ImmutableXrpCurrency;
import com.radynamics.dallipay.cryptoledger.xrpl.signing.RpcSubmitter;
import com.radynamics.dallipay.exchange.Currency;
import com.radynamics.dallipay.exchange.Money;
import com.radynamics.dallipay.iso20022.Utils;
import okhttp3.HttpUrl;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xrpl.xrpl4j.client.JsonRpcClientErrorException;
import org.xrpl.xrpl4j.client.XrplClient;
import org.xrpl.xrpl4j.client.faucet.FaucetClient;
import org.xrpl.xrpl4j.client.faucet.FundAccountRequest;
import org.xrpl.xrpl4j.model.client.accounts.*;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;
import org.xrpl.xrpl4j.model.client.common.LedgerIndexBound;
import org.xrpl.xrpl4j.model.client.path.RipplePathFindRequestParams;
import org.xrpl.xrpl4j.model.client.path.RipplePathFindResult;
import org.xrpl.xrpl4j.model.client.transactions.ImmutableTransactionRequestParams;
import org.xrpl.xrpl4j.model.ledger.AccountRootObject;
import org.xrpl.xrpl4j.model.transactions.*;
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class JsonRpcApi implements TransactionSource {
    final static Logger log = LogManager.getLogger(JsonRpcApi.class);
    private final Ledger ledger;
    private final NetworkInfo network;
    private final XrplClient xrplClient;
    private final LedgerAtTimeProvider ledgerAtTimeProvider;
    private final Cache<AccountRootObject> accountDataCache;
    private final Cache<AccountLinesResult> accountTrustLineCache;
    private final Cache<RipplePathFindResultEntry> ripplePathFindCache;
    private final Cache<ImmutableBookOffersResult> bookOffersCache;

    private final ResourceBundle res = ResourceBundle.getBundle("i18n." + this.getClass().getSimpleName());

    public JsonRpcApi(Ledger ledger, NetworkInfo network) {
        this.ledger = ledger;
        this.network = network;
        this.xrplClient = new XrplClient(network.getUrl());
        var fallback = new OnchainLookupProvider(xrplClient);
        this.ledgerAtTimeProvider = ledger.isKnownMainnet(network) ? new XrplfDataApi(fallback) : fallback;
        this.accountDataCache = new Cache<>(network.getUrl().toString());
        this.accountTrustLineCache = new Cache<>(network.getUrl().toString());
        this.ripplePathFindCache = new Cache<>(network.getUrl().toString());
        this.bookOffersCache = new Cache<>(network.getUrl().toString());
    }

    @Override
    public TransactionResult listPaymentsSent(Wallet wallet, long sinceDaysAgo, int limit) throws Exception {
        var start = ledgerAtTimeProvider.estimatedDaysAgo(sinceDaysAgo).orElse(estimatedAgoFallback(sinceDaysAgo));
        // Use endOfToday to ensure data until latest ledger is loaded.
        var end = Utils.endOfToday();
        var params = createAccountTransactionsRequestParams(wallet, start.getLedgerIndex(), end, null);
        return listPayments(params, limit, (Payment p) -> StringUtils.equals(p.account().value(), wallet.getPublicKey()));
    }

    private LedgerAtTime estimatedAgoFallback(long sinceDaysAgo) throws LedgerAtTimeException {
        log.trace(String.format("Getting estimated fallback %s days ago.", sinceDaysAgo));

        var deduction = Math.round(sinceDaysAgo * 0.2);
        long remaining = sinceDaysAgo - deduction;
        while (remaining > 0) {
            var candidate = ledgerAtTimeProvider.estimatedDaysAgo(remaining).orElse(null);
            if (candidate != null) {
                return candidate;
            }
            remaining -= deduction;
        }

        return new LedgerAtTime(ZonedDateTime.now(), LedgerIndex.VALIDATED);
    }

    @Override
    public TransactionResult listPaymentsReceived(Wallet wallet, DateTimeRange period) throws Exception {
        var params = createAccountTransactionsRequestParams(wallet, period, null);
        return listPayments(params, 100, (Payment p) -> StringUtils.equals(p.destination().value(), wallet.getPublicKey()));
    }

    private TransactionResult listPayments(ImmutableAccountTransactionsRequestParams.Builder params, int limit, Function<Payment, Boolean> include) throws Exception {
        var tr = new TransactionResult();
        loadTransactions(params, limit, tr, (org.xrpl.xrpl4j.model.transactions.Transaction t, CurrencyAmount deliveredAmount) -> {
            if (t.transactionType() == TransactionType.PAYMENT) {
                var p = (Payment) t;
                if (!include.apply(p)) {
                    return false;
                }

                tr.add(toTransaction(p, deliveredAmount));
                return true;
            }
            return false;
        });
        return tr;
    }

    private void loadTransactions(ImmutableAccountTransactionsRequestParams.Builder params, int limit, TransactionResult tr, BiFunction<org.xrpl.xrpl4j.model.transactions.Transaction, CurrencyAmount, Boolean> include) throws Exception {
        var pageCounter = 0;
        var maxPages = 10;
        var result = xrplClient.accountTransactions(params.build());

        if (result.transactions().size() == 0) {
            tr.setHasNoTransactions(true);
            tr.setExistsWallet(exists(result.account()));
            return;
        }

        while (tr.transactions().length < limit && pageCounter < maxPages && result.transactions().size() > 0) {
            for (var r : result.transactions()) {
                if (tr.transactions().length >= limit) {
                    tr.setHasMarker(true);
                    return;
                }

                if (!r.metadata().isPresent() || !r.metadata().get().transactionResult().equalsIgnoreCase("tesSUCCESS")) {
                    continue;
                }

                var deliveredAmount = r.metadata().get().deliveredAmount().orElse(XrpCurrencyAmount.ofDrops(0));
                if (!include.apply(r.resultTransaction().transaction(), deliveredAmount)) {
                    continue;
                }
            }

            if (pageCounter == 2 && tr.transactions().length == 0) {
                tr.setHasNoTransactions(true);
                return;
            }

            if (!result.marker().isPresent()) {
                tr.setHasMarker(false);
                return;
            }
            params.marker(result.marker().get());
            result = xrplClient.accountTransactions(params.build());
            pageCounter++;
        }

        tr.setHasMaxPageCounterReached(pageCounter >= maxPages);
    }

    private Transaction toTransaction(Payment p, CurrencyAmount deliveredAmount) {
        var future = new CompletableFuture<Transaction>();
        deliveredAmount.handle(xrpCurrencyAmount -> {
            try {
                future.complete(toTransaction(p, xrpCurrencyAmount));
            } catch (DecoderException | UnsupportedEncodingException e) {
                future.completeExceptionally(e);
            }
        }, issuedCurrencyAmount -> {
            try {
                future.complete(toTransaction(p, issuedCurrencyAmount));
            } catch (ExecutionException | InterruptedException | DecoderException | UnsupportedEncodingException e) {
                future.completeExceptionally(e);
            }
        });
        return future.join();
    }

    private Transaction toTransaction(org.xrpl.xrpl4j.model.transactions.Payment p, IssuedCurrencyAmount amount) throws ExecutionException, InterruptedException, DecoderException, UnsupportedEncodingException {
        var ccyCode = Convert.toCurrencyCode(amount.currency());
        var amt = BigDecimal.valueOf(Double.parseDouble(amount.value()));

        var issuer = ledger.createWallet(amount.issuer().value(), "");
        var ccy = new Currency(ccyCode, issuer);
        // When the issuer field of the destination Amount field matches the Destination address, it is treated as a special case meaning "any issuer that the destination accepts." (https://xrpl.org/payment.html)
        if (!issuer.getPublicKey().equals((p.destination().value())) || p.sendMax().isEmpty()) {
            return toTransaction(p, amt, ccy);
        }

        var future = new CompletableFuture<Transaction>();
        p.sendMax().get().handle(xrpCurrencyAmount -> {
                    try {
                        future.complete(toTransaction(p, amt, ccy));
                    } catch (DecoderException | UnsupportedEncodingException e) {
                        future.completeExceptionally(e);
                    }
                },
                issuedCurrencyAmountSendMax -> {
                    try {
                        var issuerSendMax = ledger.createWallet(issuedCurrencyAmountSendMax.issuer().value(), "");
                        future.complete(toTransaction(p, amt, new Currency(ccyCode, issuerSendMax)));
                    } catch (DecoderException | UnsupportedEncodingException e) {
                        future.completeExceptionally(e);
                    }
                });

        return future.join();
    }

    private ImmutableAccountTransactionsRequestParams.Builder createAccountTransactionsRequestParams(Wallet wallet, DateTimeRange period, Marker marker) throws JsonRpcClientErrorException, LedgerException, LedgerAtTimeException {
        var start = ledgerAtTimeProvider.at(period.getStart()).orElse(null);
        if (start == null) {
            throw new LedgerException(String.format(res.getString("ledgerNotFoundAt"), period.getStart()));
        }

        return createAccountTransactionsRequestParams(wallet, start.getLedgerIndex(), period.getEnd(), marker);
    }

    private ImmutableAccountTransactionsRequestParams.Builder createAccountTransactionsRequestParams(Wallet wallet, LedgerIndex start, ZonedDateTime end, Marker marker) throws LedgerAtTimeException, LedgerException {
        var endIndex = LedgerIndex.VALIDATED;
        if (end.isBefore(ZonedDateTime.now())) {
            var endLedger = ledgerAtTimeProvider.at(end).orElse(null);
            if (endLedger == null) {
                throw new LedgerException(String.format(res.getString("ledgerNotFoundAt"), end));
            }
            endIndex = endLedger.getLedgerIndex();
        }

        return createAccountTransactionsRequestParams(wallet, start, endIndex, marker);
    }

    private ImmutableAccountTransactionsRequestParams.Builder createAccountTransactionsRequestParams(Wallet wallet, BlockRange period, Marker marker) throws LedgerAtTimeException, LedgerException {
        var start = Convert.toLedgerBlock(period.getStart()).getLedgerIndex();
        var end = Convert.toLedgerBlock(period.getEnd()).getLedgerIndex();
        return createAccountTransactionsRequestParams(wallet, start, end, marker);
    }

    private ImmutableAccountTransactionsRequestParams.Builder createAccountTransactionsRequestParams(Wallet wallet, LedgerIndex start, LedgerIndex end, Marker marker) throws LedgerAtTimeException, LedgerException {
        var b = AccountTransactionsRequestParams.builder()
                .account(Address.of(wallet.getPublicKey()))
                .ledgerIndexMinimum(LedgerIndexBound.of(start.unsignedIntegerValue().intValue()));
        if (end != LedgerIndex.VALIDATED) {
            b.ledgerIndexMaximum(LedgerIndexBound.of(end.unsignedIntegerValue().intValue()));
        }

        if (marker != null) {
            b.marker(marker);
        }
        return b;
    }

    public com.radynamics.dallipay.cryptoledger.Transaction getTransaction(String transactionId) {
        try {
            var params = ImmutableTransactionRequestParams.builder()
                    .transaction(Hash256.of(transactionId));
            var r = xrplClient.transaction(params.build(), org.xrpl.xrpl4j.model.transactions.Transaction.class);

            if (r.transaction().transactionType() != TransactionType.PAYMENT) {
                log.info("Transaction is not a payment, return null");
                return null;
            }
            var p = (Payment) r.transaction();
            var deliveredAmount = r.metadata().get().deliveredAmount().get();
            return toTransaction(p, deliveredAmount);
        } catch (JsonRpcClientErrorException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public com.radynamics.dallipay.cryptoledger.Transaction[] listTrustlineTransactions(Wallet wallet, DateTimeRange period, Wallet ccyIssuer, String ccy) throws Exception {
        var startLedger = ledgerAtTimeProvider.at(period.getStart()).orElse(null);
        if (startLedger == null) {
            throw new LedgerException(String.format(res.getString("ledgerNotFoundAt"), period.getStart()));
        }

        var end = Block.VALIDATED;
        if (period.getEnd().isBefore(ZonedDateTime.now())) {
            var endLedger = ledgerAtTimeProvider.at(period.getEnd()).orElse(null);
            if (endLedger == null) {
                throw new LedgerException(String.format(res.getString("ledgerNotFoundAt"), period.getEnd()));
            }
            end = new LedgerBlock(endLedger.getLedgerIndex());
        }

        return listTrustlineTransactions(wallet, BlockRange.of(new LedgerBlock(startLedger.getLedgerIndex()), end), ccyIssuer, ccy);
    }

    public com.radynamics.dallipay.cryptoledger.Transaction[] listTrustlineTransactions(Wallet wallet, BlockRange period, Wallet ccyIssuer, String ccy) throws Exception {
        var tr = new TransactionResult();
        var params = createAccountTransactionsRequestParams(wallet, period, null);
        final int limit = 200;
        loadTransactions(params, limit, tr, (org.xrpl.xrpl4j.model.transactions.Transaction t, CurrencyAmount deliveredAmount) -> {
            if (!(t instanceof ImmutableTrustSet)) {
                return false;
            }
            var trustSet = (ImmutableTrustSet) t;
            if (!trustSet.limitAmount().issuer().value().equals(ccyIssuer.getPublicKey()) || !trustSet.limitAmount().currency().equalsIgnoreCase(ccy)) {
                return false;
            }
            try {
                tr.add(toTransaction(t, XrpCurrencyAmount.ofDrops(0)));
            } catch (DecoderException | UnsupportedEncodingException e) {
                log.error(e.getMessage(), e);
            }
            return true;
        });
        return tr.transactions();
    }

    private boolean exists(Address wallet) {
        return exists(WalletConverter.from(wallet));
    }

    public boolean exists(Wallet wallet) {
        return getAccountData(wallet) != null;
    }

    private synchronized AccountRootObject getAccountData(Wallet wallet) {
        accountDataCache.evictOutdated();
        var key = new WalletKey(wallet);
        var data = accountDataCache.get(key);
        // Contained without data means "wallet doesn't exist" (wasn't found previously)
        if (data != null || accountDataCache.isPresent(key)) {
            return data;
        }
        try {
            var requestParams = AccountInfoRequestParams.of(Address.of(wallet.getPublicKey()));
            data = xrplClient.accountInfo(requestParams).accountData();
            accountDataCache.add(key, data);
            return data;
        } catch (Exception e) {
            if (isAccountNotFound(e)) {
                accountDataCache.add(key, null);
            } else {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

    private synchronized AccountLinesResult getAccountLines(Wallet wallet) {
        accountTrustLineCache.evictOutdated();
        var key = new WalletKey(wallet);
        var data = accountTrustLineCache.get(key);
        // Contained without data means "wallet doesn't exist" (wasn't found previously)
        if (data != null || accountTrustLineCache.isPresent(key)) {
            return data;
        }
        try {
            var requestParams = AccountLinesRequestParams.builder().account(Address.of(wallet.getPublicKey())).build();
            data = xrplClient.accountLines(requestParams);
            accountTrustLineCache.add(key, data);
            return data;
        } catch (JsonRpcClientErrorException e) {
            if (isAccountNotFound(e)) {
                accountTrustLineCache.add(key, null);
            } else {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

    public boolean existsPath(Wallet sender, Wallet receiver, Money amount) {
        var data = getRipplePathFindResult(sender, receiver, amount);
        return data != null && data.alternatives().size() > 0;
    }

    private RipplePathFindResult getRipplePathFindResult(Wallet sender, Wallet receiver, Money amount) {
        ripplePathFindCache.evictOutdated();
        var key = new RipplePathFindKey(sender, receiver, amount.getCcy());
        var cached = ripplePathFindCache.list(key, new RipplePathFindResultEntry[0]);
        for (var c : cached) {
            // Contained without data means "wallet doesn't exist" (wasn't found previously)
            if (c.getResult() == null) {
                return null;
            }
            // Assume lower amounts also work if there are known paths for higher amounts.
            if (amount.lessThan(c.getAmount()) || amount.equals(c.getAmount())) {
                log.trace(String.format("CACHE hit %s (%s <= %s)", key.get(), amount, c.getAmount()));
                return c.getResult();
            }
        }
        try {
            var requestParams = RipplePathFindRequestParams.builder()
                    .sourceAccount(Address.of(sender.getPublicKey()))
                    .destinationAccount(Address.of(receiver.getPublicKey()))
                    .destinationAmount(PaymentBuilder.toCurrencyAmount(ledger, amount));
            var data = xrplClient.ripplePathFind(requestParams.build());
            ripplePathFindCache.add(key, new RipplePathFindResultEntry(data, amount));
            return data;
        } catch (Exception e) {
            if (isAccountNotFound(e)) {
                ripplePathFindCache.add(key, new RipplePathFindResultEntry(null, amount));
            } else {
                log.error(e.getMessage(), e);
            }
            return null;
        }
    }

    private boolean isAccountNotFound(Exception e) {
        return e.getMessage().equals("Account not found.") || e.getMessage().equals("Source account not found.");
    }

    public boolean existsSellOffer(Money minimum) {
        var result = getBookOffers(minimum.getCcy());
        if (result == null) {
            return false;
        }

        var available = Money.zero(minimum.getCcy());
        for (var o : result.offers()) {
            available = available.plus(PaymentBuilder.fromCurrencyAmount(ledger, o.takerPays()));
        }

        return minimum.lessThan(available);
    }

    private synchronized ImmutableBookOffersResult getBookOffers(Currency ccy) {
        bookOffersCache.evictOutdated();
        var key = new CurrencyKey(ccy);
        var data = bookOffersCache.get(key);
        // Contained without data means "doesn't exist" (wasn't found previously)
        if (data != null || bookOffersCache.isPresent(key)) {
            return data;
        }
        try {
            var b = ImmutableBookOffersRequestParams.builder()
                    .takerGets(new ImmutableXrpCurrency())
                    .takerPays(ImmutableIssuedCurrency.of(Convert.fromCurrencyCode(ccy.getCode()), Address.of(ccy.getIssuer().getPublicKey())))
                    .limit(10)
                    .build();
            data = xrplClient.getJsonRpcClient().send(b.request(), ImmutableBookOffersResult.class);
            bookOffersCache.add(key, data);
            return data;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public FeeInfo latestFee() {
        try {
            var fee = xrplClient.fee();
            var drops = fee.drops();
            var queuePercentage = fee.currentQueueSize().longValue() / fee.maxQueueSize().orElse(UnsignedInteger.ONE).longValue();
            return new FeeInfo(drops.minimumFee().value().longValue(), drops.openLedgerFee().value().longValue(), drops.medianFee().value().longValue(), queuePercentage);
        } catch (JsonRpcClientErrorException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public boolean requiresDestinationTag(Wallet wallet) {
        var accountData = getAccountData(wallet);
        return accountData != null && accountData.flags().lsfRequireDestTag();
    }

    public double getTransferFee(Wallet wallet) {
        var accountData = getAccountData(wallet);
        if (accountData == null) {
            return 0;
        }

        // "0%" fee (https://xrpl.org/transfer-fees.html)
        final UnsignedInteger zeroFee = UnsignedInteger.valueOf(1000000000);
        var transferRate = accountData.transferRate().orElse(zeroFee);
        // Return fee as value between 0.0 (0%) and 1.0 (100%)
        return transferRate.minus(zeroFee).doubleValue() / zeroFee.doubleValue();
    }

    public boolean isBlackholed(Wallet wallet) {
        var accountData = getAccountData(wallet);
        if (accountData == null) {
            return false;
        }

        var blackholed = new HashSet<>(Arrays.asList("rrrrrrrrrrrrrrrrrrrrrhoLvTp", "rrrrrrrrrrrrrrrrrrrrBZbvji"));
        return accountData.regularKey().isPresent()
                && accountData.flags().lsfDisableMaster() && blackholed.contains(accountData.regularKey().get().value());
    }

    public boolean walletAcceptsXrp(Wallet wallet) {
        var accountData = getAccountData(wallet);
        if (accountData == null) {
            return false;
        }
        return !accountData.flags().lsfDisallowXrp();
    }

    private Transaction toTransaction(org.xrpl.xrpl4j.model.transactions.Payment p, XrpCurrencyAmount deliveredAmount) throws DecoderException, UnsupportedEncodingException {
        return toTransaction((org.xrpl.xrpl4j.model.transactions.Transaction) p, deliveredAmount);
    }

    private Transaction toTransaction(org.xrpl.xrpl4j.model.transactions.Transaction t, XrpCurrencyAmount deliveredAmount) throws DecoderException, UnsupportedEncodingException {
        return toTransaction(t, deliveredAmount.toXrp(), new Currency(ledger.getNativeCcySymbol()));
    }

    private Transaction toTransaction(org.xrpl.xrpl4j.model.transactions.Transaction t, BigDecimal amt, Currency ccy) throws DecoderException, UnsupportedEncodingException {
        var trx = new Transaction(ledger, Money.of(amt.doubleValue(), ccy));
        trx.setId(t.hash().get().value());
        trx.setBooked(t.closeDateHuman().get());
        trx.setBlock(new LedgerBlock(t.ledgerIndex().orElseThrow()));
        trx.setSender(WalletConverter.from(t.account()));
        for (MemoWrapper mw : t.memos()) {
            if (!mw.memo().memoData().isPresent()) {
                continue;
            }
            var unwrappedMemo = PayloadConverter.fromMemo(Utils.hexToString(mw.memo().memoData().get()));
            for (var ft : unwrappedMemo.freeTexts()) {
                trx.addMessage(ft);
            }
        }

        var l = new StructuredReferenceLookup(t);
        for (var r : l.find()) {
            trx.addStructuredReference(r);
        }

        if (t.transactionType() == TransactionType.PAYMENT) {
            var p = (Payment) t;
            trx.setReceiver(WalletConverter.from(p.destination()));
            if (p.destinationTag().isPresent()) {
                trx.setDestinationTag(p.destinationTag().get().toString());
            }
            trx.setInvoiceId(p.invoiceId().isEmpty() ? "" : p.invoiceId().get().value());
        }

        return trx;
    }

    public void refreshBalance(Wallet wallet, boolean useCache) {
        if (!useCache) {
            var key = new WalletKey(wallet);
            accountDataCache.evict(key);
            accountTrustLineCache.evict(key);
        }
        var accountData = getAccountData(wallet);
        if (accountData != null) {
            wallet.getBalances().set(Money.of(accountData.balance().toXrp().doubleValue(), new Currency(ledger.getNativeCcySymbol())));
        }

        for (var t : listTrustlines(wallet)) {
            wallet.getBalances().set(t.getBalance());
        }
    }

    public Trustline[] listTrustlines(Wallet wallet) {
        var list = new ArrayList<Trustline>();
        var result = getAccountLines(wallet);
        if (result == null) {
            return new Trustline[0];
        }
        for (var line : result.lines()) {
            var amt = Double.parseDouble(line.balance());
            var issuer = amt >= 0 ? ledger.createWallet(line.account().value(), "") : wallet;
            var ccy = new Currency(Convert.toCurrencyCode(line.currency()), issuer);
            var lmt = Double.parseDouble(line.limit());
            if (lmt > 0) {
                ccy.setTransferFee(getTransferFee(WalletConverter.from(issuer)));
            } else {
                log.trace("Skipped getTransferFee due trustline.limit is " + lmt);
            }
            var balance = Money.of(amt, ccy);
            var limit = Money.of(lmt, ccy);

            list.add(new Trustline(wallet, balance, limit));
        }
        return list.toArray(new Trustline[0]);
    }

    public String getAccountDomain(Wallet wallet) {
        var accountData = getAccountData(wallet);
        if (accountData == null) {
            return null;
        }
        var hex = accountData.domain().orElse(null);
        try {
            return hex == null ? null : Utils.hexToString(hex);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public TransactionSubmitter createTransactionSubmitter(PrivateKeyProvider privateKeyProvider) {
        var signer = new RpcSubmitter(ledger, privateKeyProvider);
        signer.setVerifier(new OnchainVerifier(ledger));
        return signer;
    }

    public com.radynamics.dallipay.cryptoledger.Wallet createRandomWallet(HttpUrl faucetUrl) {
        var walletFactory = DefaultWalletFactory.getInstance();
        var w = walletFactory.randomWallet(network.isTestnet());

        var faucetClient = FaucetClient.construct(faucetUrl);
        faucetClient.fundAccount(FundAccountRequest.of(w.wallet().classicAddress()));

        var wallet = ledger.createWallet(w.wallet().classicAddress().value(), w.seed());
        ledger.refreshBalance(wallet, false);
        return wallet;
    }

    public EndpointInfo getEndpointInfo(NetworkInfo networkInfo) throws JsonRpcClientErrorException {
        var c = new XrplClient(networkInfo.getUrl());
        var serverInfo = c.serverInformation();

        var future = new CompletableFuture<EndpointInfo>();
        serverInfo.info().handle(rippledServerInfo -> future.complete(EndpointInfo.builder()
                .networkInfo(networkInfo)
                .serverVersion(rippledServerInfo.buildVersion())
                .hostId(rippledServerInfo.hostId())
        ), clioServerInfo -> future.complete(EndpointInfo.builder()
                .networkInfo(networkInfo)
                .serverVersion(clioServerInfo.rippledVersion().orElse(clioServerInfo.clioVersion()))
        ), reportingModeServerInfo -> future.complete(EndpointInfo.builder()
                .networkInfo(networkInfo)
                .serverVersion(reportingModeServerInfo.buildVersion())
                .hostId(reportingModeServerInfo.hostId())
        ));
        return future.join();
    }
}
