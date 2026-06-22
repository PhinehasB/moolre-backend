package com.project.klare_server.moolre;

import java.util.Set;

public final class GhanaMobileMoney {

    private static final Set<String> MTN_PREFIXES = Set.of("024", "025", "053", "054", "055", "059");
    private static final Set<String> TELECEL_PREFIXES = Set.of("020", "050");
    private static final Set<String> AT_PREFIXES = Set.of("026", "027", "056", "057");

    private GhanaMobileMoney() {
    }

    public enum Network {
        MTN("1", "13"),
        TELECEL("6", "6"),
        AT("7", "7");

        private final String transferChannel;
        private final String paymentChannel;

        Network(String transferChannel, String paymentChannel) {
            this.transferChannel = transferChannel;
            this.paymentChannel = paymentChannel;
        }

        public String transferChannel() {
            return transferChannel;
        }

        public String paymentChannel() {
            return paymentChannel;
        }
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

    public static Network networkOf(String phone) {
        String local = normalize(phone);
        if (local.length() < 3) {
            throw new MoolreException("UNSUPPORTED_NETWORK", "Unrecognised mobile money number");
        }
        String prefix = local.substring(0, 3);
        if (MTN_PREFIXES.contains(prefix)) {
            return Network.MTN;
        }
        if (TELECEL_PREFIXES.contains(prefix)) {
            return Network.TELECEL;
        }
        if (AT_PREFIXES.contains(prefix)) {
            return Network.AT;
        }
        throw new MoolreException("UNSUPPORTED_NETWORK", "Unsupported mobile money network for " + local);
    }

    public static String toInternational(String phone) {
        String local = normalize(phone);
        if (local.startsWith("0") && local.length() == 10) {
            return "+233" + local.substring(1);
        }
        String digits = phone == null ? "" : phone.replaceAll("\\D", "");
        if (digits.startsWith("233")) {
            return "+" + digits;
        }
        return "+233" + digits;
    }

    public static String resolveChannel(String phone) {
        return networkOf(phone).transferChannel();
    }

    public static String resolvePaymentChannel(String phone) {
        return networkOf(phone).paymentChannel();
    }
}
