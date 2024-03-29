package org.snomed.aag.config;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;;
import static org.junit.jupiter.api.Assertions.assertFalse;;

class URLMappingConfigTest {
    private static final String[] PATTERN_STRINGS = new URLMappingConfig().getPatternStrings();
    private static final String [][] TEST_DATA = {
                    // Pattern,
                    // Input,
                    // Expected result
                    {
                            PATTERN_STRINGS[0], // e.g. /criteria/(.*)
                            "/criteria/MAIN/projectA/taskB",
                            "MAIN/projectA/taskB"
                    },
                    {
                            PATTERN_STRINGS[1], // e.g. /acceptance/(.*)/item/.*/accept
                            "/acceptance/MAIN/projectA/taskB/item/itemABC/accept",
                            "MAIN/projectA/taskB"
                    },
                    {
                            PATTERN_STRINGS[2], // e.g. /acceptance/(.*)
                            "/acceptance/MAIN/projectA/taskB",
                            "MAIN/projectA/taskB"
                    },
                    {
                            PATTERN_STRINGS[3], // e.g. /whitelist-items/item/(.*)
                            "/whitelist-items/item/31cf88f2-2f2f-49c4-bd2d-0f1b9ead983e",
                            "31cf88f2-2f2f-49c4-bd2d-0f1b9ead983e"
                    },
                    {
                            PATTERN_STRINGS[4], // e.g. /whitelist-items/(.*)
                            "/whitelist-items/MAIN/projectA/taskB",
                            "MAIN/projectA/taskB"
                    },
                    {
                            PATTERN_STRINGS[6], // e.g. /admin/criteria/(.*)/accept
                            "/admin/criteria/MAIN/projectA/taskB/accept",
                            "MAIN/projectA/taskB"
                    }

    };

    /*
     * If you add an entry to the String array in URLMappingConfig and this
     * test fails, please:
     *   - increment expectedLength
     *   - add to TEST_DATA
     * */
    @Test
    void getPatternStrings_ShouldReturnArrayWithExpectedLength() {
        // given
        URLMappingConfig urlMappingConfig = new URLMappingConfig();
        int expectedLength = 7;

        // when
        String[] result = urlMappingConfig.getPatternStrings();

        // then
        assertEquals(expectedLength, result.length);
    }

    @Test
    void urlMappingConfig_ShouldMatchBranchPaths() {
        for (String[] testDatum : TEST_DATA) {
            // given
            String pattern = testDatum[0];
            String input = testDatum[1];
            String expectedResult = testDatum[2];
            Matcher matcher = Pattern.compile(pattern).matcher(input);

            // then
            assertMatch(expectedResult, matcher);
        }
    }

    private void assertMatch(String expected, Matcher matcher) {
        // when
        if (matcher.matches()) {
            // then
            assertEquals(expected, matcher.group(1));
        } else {
            fail("Should find match.");
        }
    }

    @Test
    void urlMappingConfig_ShouldNotMatchValidationRulesToBranch( ) {
        Matcher matcher = Pattern.compile(PATTERN_STRINGS[3]).matcher("/whitelist-items/validation-rules/e1622b95-aecd-4948-9bcd-81d2e4e3670f");
        assertFalse(matcher.matches());
    }

}
