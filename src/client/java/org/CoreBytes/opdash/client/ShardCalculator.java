package org.CoreBytes.opdash.client;

public class ShardCalculator {

    // Berechnet Shards für eine Menge eines Items
    public static double calculateShards(String itemKey, double amount) {
        double rate = OPShardAPI.getRate(itemKey);
        return rate * amount;
    }

    // Berechnet die Menge, die man für eine Ziel-Shardzahl braucht
    public static int calculateAmountForShards(String itemKey, double targetShards) {
        double rate = OPShardAPI.getRate(itemKey);
        if (rate <= 0) return 0;
        return (int) Math.ceil(targetShards / rate);
    }
}