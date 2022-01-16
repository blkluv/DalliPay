package com.radynamics.CryptoIso20022Interop.cryptoledger.xrpl.api;

import com.google.common.primitives.UnsignedInteger;
import org.apache.logging.log4j.LogManager;
import org.xrpl.xrpl4j.model.client.common.LedgerIndex;

import java.time.LocalDateTime;
import java.util.HashMap;

public class LedgerAtTimeCache {
    private HashMap<UnsignedInteger, LedgerAtTime> items = new HashMap<>();

    public LedgerAtTime add(LocalDateTime pointInTime, LedgerIndex index) {
        var item = new LedgerAtTime(pointInTime, index);
        items.put(index.unsignedIntegerValue(), item);
        return item;
    }

    public LedgerAtTime find(LedgerIndex index) {
        if (items.containsKey(index.unsignedIntegerValue())) {
            var item = items.get(index.unsignedIntegerValue());
            LogManager.getLogger().trace(String.format("CACHE hit %s (%s)", item.getPointInTime(), item.getLedgerIndex().unsignedIntegerValue()));
            return item;
        }
        return null;
    }

    public LedgerAtTime find(LocalDateTime dt) {
        for (var e : items.entrySet()) {
            var item = e.getValue();
            if (item.getPointInTime() == dt) {
                LogManager.getLogger().trace(String.format("CACHE hit %s (%s)", item.getPointInTime(), item.getLedgerIndex().unsignedIntegerValue()));
                return item;
            }
        }
        return null;
    }
}
