package org.snomed.aag.config;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class URLMappingConfigTest {
    private static final String[] PATTERN_STRINGS = new URLMappingConfig().getPatternStrings();
    private static final String[][][] TEST_DATA = {
            {
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
                            PATTERN_STRINGS[3], // e.g. /whitelist-items/(.*)
                            "/whitelist-items/MAIN/projectA/taskB",
                            "MAIN/projectA/taskB"
                    },
                    {
                            PATTERN_STRINGS[3], // e.g. /admin/criteria/(.*)/accept
                            "/admin/criteria/MAIN/projectA/taskB/accept",
                            "MAIN/projectA/taskB"
                    }
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
        int expectedLength = 5;

        // when
        String[] result = urlMappingConfig.getPatternStrings();

        // then
        assertEquals(expectedLength, result.length);
    }

    @Test
    void urlMappingConfig_ShouldMatchBranchPaths() {
        for (int i = 0; i < TEST_DATA[0].length; i++) {
            // given
            String pattern = TEST_DATA[0][0][0];
            String input = TEST_DATA[0][0][1];
            String expectedResult = TEST_DATA[0][0][2];
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
}
