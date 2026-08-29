package com.nandinirai.customers.customer.validation;

public final class NameRules {


    public static final String PATTERN = "^\\p{L}[\\p{L}\\p{M}'\u2019\\-. ]*$";

    public static final String MESSAGE =
            "may only contain letters, spaces, hyphens, apostrophes and full stops";

    public static final int MAX_LENGTH = 100;

    private NameRules() {
    }
}
