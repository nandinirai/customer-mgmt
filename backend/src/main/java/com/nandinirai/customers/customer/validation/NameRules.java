package com.nandinirai.customers.customer.validation;

public final class NameRules {

    /**
     * Unicode letters, so "Zoë", "李", "O'Connor", "Anne-Marie" and "Jr." all
     * pass. An ASCII-only rule would quietly exclude a large share of real
     * names, which is a correctness bug dressed up as validation.
     */
    public static final String PATTERN = "^\\p{L}[\\p{L}\\p{M}'\u2019\\-. ]*$";

    public static final String MESSAGE =
            "may only contain letters, spaces, hyphens, apostrophes and full stops";

    public static final int MAX_LENGTH = 100;

    private NameRules() {
    }
}
