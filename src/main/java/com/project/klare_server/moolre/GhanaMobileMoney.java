package com.project.klare_server.moolre;

import java.util.Set;

public final class GhanaMobileMoney {

    public static final String CHANNEL_MTN = "1";
    public static final String CHANNEL_TELECEL = "6";
    public static final String CHANNEL_AT = "7";

    private static final Set<String> MTN_PREFIXES = Set.of("024", "025", "053", "054", "055", "059");
    private static final Set<String> TELECEL_PREFIXES = Set.of("020", "050");
    private static final Set<String> AT_PREFIXES = Set.of("026", "027", "056", "057");

    private GhanaMobileMoney() {
    }

    public static String normalize(String phone) {
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.startsWith("233") && digits.length() == 12) {
            return "0" + digits.substring(3);
        }
        if (digits.length() == 9) {
            return "0" + digits;
        }
        return digits;
    }

    public static String resolveChannel(String phone) {
        String local = normalize(phone);
        if (local.length() < 3) {
            throw new MoolreException("UNSUPPORTED_NETWORK", "Unrecognised mobile money number");
        }
        String prefix = local.substring(0, 3);
        if (MTN_PREFIXES.contains(prefix)) {
            return CHANNEL_MTN;
        }
        if (TELECEL_PREFIXES.contains(prefix)) {
            return CHANNEL_TELECEL;
        }
        if (AT_PREFIXES.contains(prefix)) {
            return CHANNEL_AT;
        }
        throw new MoolreException("UNSUPPORTED_NETWORK", "Unsupported mobile money network for " + local);
    }
}
