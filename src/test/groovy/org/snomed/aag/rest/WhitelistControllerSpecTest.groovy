package org.snomed.aag.rest

import org.snomed.aag.data.domain.WhitelistItem
import org.snomed.aag.data.services.ServiceRuntimeException
import spock.lang.Specification

class WhitelistControllerSpecTest extends Specification {
    def "test valid whitelist content #testName"() {
        expect:
            WhitelistItem wli = new WhitelistItem([
                    userId          : userId,
                    componentId     : componentId,
                    conceptId       : conceptId,
                    branch          : branch
            ])

        when:
            def result = (new WhitelistController(null, null)).validateSingleWhiteListItem(wli)

        then:
            result

        where:
            testName               | userId | componentId | conceptId | branch
            "All OK"               | "jco"  | "2"         | "3"       | "MAIN"
            "Component ID is text" | "jco"  | "A"         | "3"       | "MAIN"
    }

    def "test invalid whitelist content #testName"() {
        expect:
            WhitelistItem wli = new WhitelistItem([
                    userId     : userId,
                    componentId: componentId,
                    conceptId  : conceptId,
                    branch     : branch
            ])

        when:
            (new WhitelistController(null, null)).validateSingleWhiteListItem(wli)

        then:
            def error = thrown(ServiceRuntimeException)
            error.message == expectedMessage

        where:
            testName                  | userId | componentId | conceptId | branch || expectedMessage
            "All NULLS"               | null   | null        | null      | null   || "Invalid component ID: 'null'.\nInvalid concept ID: 'null'.\nBranch is mandatory.\n"
            "All Empty"               | ""     | ""          | ""        | ""     || "Invalid component ID: ''.\nInvalid concept ID: ''.\nBranch is mandatory.\n"
            "Component ID missing"    | "jco"  | null        | "3"       | "MAIN" || "Invalid component ID: 'null'.\n"
            "Concept ID missing"      | "jco"  | "2"         | null      | "MAIN" || "Invalid concept ID: 'null'.\n"
            "Branch missing"          | "jco"  | "2"         | "3"       | null   || "Branch is mandatory.\n"
            "Component ID is empty"   | "jco"  | ""         | "3"       | "MAIN" || "Invalid component ID: ''.\n"
            "Concept ID not number"   | "jco"  | "2"         | "A"       | "MAIN" || "Invalid concept ID: 'A'.\n"
    }
}
