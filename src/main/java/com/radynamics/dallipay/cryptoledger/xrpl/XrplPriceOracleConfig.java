package com.radynamics.dallipay.cryptoledger.xrpl;

import com.radynamics.dallipay.cryptoledger.LedgerId;
import com.radynamics.dallipay.cryptoledger.generic.Wallet;
import com.radynamics.dallipay.db.ConfigRepo;
import com.radynamics.dallipay.exchange.CurrencyPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class XrplPriceOracleConfig {
    final static Logger log = LogManager.getLogger(XrplPriceOracleConfig.class);
    private final HashSet<IssuedCurrency> issuedCurrencies = new HashSet<>();

    private final static ResourceBundle res = ResourceBundle.getBundle("i18n.Various");

    public final static String AsReceived = res.getString("asReceived");

    public void load() {
        try (var repo = new ConfigRepo()) {
            load(repo);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void load(ConfigRepo repo) throws Exception {
        issuedCurrencies.clear();
        issuedCurrencies.addAll(Arrays.asList(fromJson(repo.getXrplPriceOracleConfig())));
    }

    public void save() throws Exception {
        try (var repo = new ConfigRepo()) {
            save(repo);
            repo.commit();
        } catch (Exception e) {
            throw e;
        }
    }

    public void save(ConfigRepo repo) throws Exception {
        repo.setXrplPriceOracleConfig(toJson(issuedCurrencies));
    }

    private JSONObject toJson(Collection<IssuedCurrency> ccys) {
        var o = new JSONObject();

        o.put("version", 1);
        var ccyPairs = new JSONArray();
        o.put("ccyPairs", ccyPairs);
        for (var ccy : ccys) {
            var k = new JSONObject();
            ccyPairs.put(k);
            k.put("first", ccy.getPair().getFirstCode());
            k.put("second", ccy.getPair().getSecondCode());
            k.put("issuer", ccy.getIssuer().getPublicKey());
            k.put("receiver", ccy.getReceiver().getPublicKey());
        }

        return o;
    }

    private IssuedCurrency[] fromJson(JSONObject json) {
        var list = new ArrayList<IssuedCurrency>();

        var ccyPairs = json.getJSONArray("ccyPairs");
        for (var i = 0; i < ccyPairs.length(); i++) {
            var k = ccyPairs.getJSONObject(i);
            var pair = new CurrencyPair(k.getString("first"), k.getString("second"));
            list.add(new IssuedCurrency(pair, new Wallet(LedgerId.Xrpl, k.getString("issuer")), new Wallet(LedgerId.Xrpl, k.getString("receiver"))));
        }

        return list.toArray(new IssuedCurrency[0]);
    }

    public static List<IssuedCurrency> defaultsXumm() {
        var list = new ArrayList<IssuedCurrency>();
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "USD"), new Wallet(LedgerId.Xrpl, "r9PfV3sQpKLWxccdg3HL2FXKxGW2orAcLE"), new Wallet(LedgerId.Xrpl, "rXUMMaPpZqPutoRszR29jtC8amWq3APkx")));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "JPY"), new Wallet(LedgerId.Xrpl, "r9PfV3sQpKLWxccdg3HL2FXKxGW2orAcLE"), new Wallet(LedgerId.Xrpl, "rrJPYwVRyWFcwfaNMm83QEaCexEpKnkEg")));
        return list;
    }

    public static List<IssuedCurrency> defaultsRadyamics() {
        var issuer = new Wallet(LedgerId.Xrpl, "rDLx56UDgChRy3HqwkFSDBpX4hL6sEgmtx");
        var receiver = new Wallet(LedgerId.Xrpl, "rpXCfDds782Bd6eK9Hsn15RDnGMtxf752m");
        var list = new ArrayList<IssuedCurrency>();
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "USD"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "EUR"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "JPY"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "KRW"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "TRY"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "GBP"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "THB"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "RUB"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "BRL"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "AUD"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "MXN"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "ZAR"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "MYR"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "IDR"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "SGD"), issuer, receiver));
        list.add(new IssuedCurrency(new CurrencyPair("XRP", "CHF"), issuer, receiver));
        return list;
    }

    public IssuedCurrency[] issuedCurrencies() {
        return issuedCurrencies.toArray(new IssuedCurrency[0]);
    }

    public void set(List<IssuedCurrency> issuedCurrencies) {
        this.issuedCurrencies.clear();
        this.issuedCurrencies.addAll(issuedCurrencies);
    }
}
