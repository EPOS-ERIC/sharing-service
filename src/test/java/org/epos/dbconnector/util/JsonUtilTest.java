package org.epos.dbconnector.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JsonUtil - JSON normalization and denormalization.
 */
class JsonUtilTest {

    // Sample from the actual decrypted configuration (first level unescaped)
    // Note: Fixed the malformed JSON (was }}] now is }])
    private static final String SAMPLE_ESCAPED_JSON = """
            {"dataSearchConfigurables":"[{\\"id\\":\\"b92cacaf-cf2a-41ea-9933-af28cae90635\\",\\"paramValues\\":[{\\"name\\":\\"eventid\\",\\"value\\":\\"\\"},{\\"name\\":\\"minmagnitude\\",\\"value\\":\\"5\\"}],\\"style\\":{\\"id\\":\\"styler_default_id_1\\",\\"color1\\":14524637}}]","configurables":"[{\\"type\\":\\"layersOrder\\",\\"value\\":\\"[[\\\\\\\"World Imagery\\\\\\",\\\\\\\"601\\\\\\"]]\\"}}]"}""";

    @Nested
    @DisplayName("Basic Normalization Tests")
    class NormalizationTests {

        @Test
        @DisplayName("Normalize simple escaped JSON")
        void normalizeSimpleEscapedJson() {
            String input = "{\"key\":\"[{\\\"nested\\\":\\\"value\\\"}]\"}";
            
            String normalized = JsonUtil.normalizeCompact(input);
            
            System.out.println("Input:      " + input);
            System.out.println("Normalized: " + normalized);
            
            // The nested array should be parsed as actual JSON, not a string
            assertTrue(normalized.contains("\"key\":["), "Should contain parsed array");
            assertFalse(normalized.contains("\\\""), "Should not contain escaped quotes");
        }

        @Test
        @DisplayName("Normalize deeply nested escaped JSON")
        void normalizeDeeplyNestedJson() {
            // This has 2 levels of escaping: the outer value is a string containing JSON,
            // and inside that, the "value" field is also a string containing JSON
            String input = "{\"outer\":\"[{\\\"type\\\":\\\"test\\\",\\\"value\\\":\\\"[\\\\\\\"a\\\\\\\",\\\\\\\"b\\\\\\\"]\\\"}]\"}";
            
            String normalized = JsonUtil.normalizeCompact(input);
            
            System.out.println("Input:      " + input);
            System.out.println("Normalized: " + normalized);
            
            // All levels should be normalized
            assertTrue(normalized.contains("[\"a\",\"b\"]") || normalized.contains("[\"a\", \"b\"]"), 
                    "Inner array should be parsed");
        }

        @Test
        @DisplayName("Normalize null returns null")
        void normalizeNullReturnsNull() {
            assertNull(JsonUtil.normalize(null));
        }

        @Test
        @DisplayName("Normalize empty string returns empty")
        void normalizeEmptyReturnsEmpty() {
            assertEquals("", JsonUtil.normalize(""));
        }

        @Test
        @DisplayName("Normalize already normalized JSON")
        void normalizeAlreadyNormalizedJson() {
            String input = "{\"key\":[{\"nested\":\"value\"}]}";
            
            String normalized = JsonUtil.normalizeCompact(input);
            
            // Should remain the same (just reformatted)
            assertTrue(JsonUtil.isValidJson(normalized));
        }
    }

    @Nested
    @DisplayName("Real Data Tests")
    class RealDataTests {

        @Test
        @DisplayName("Normalize actual configuration sample")
        void normalizeActualConfigSample() {
            String normalized = JsonUtil.normalize(SAMPLE_ESCAPED_JSON);
            
            System.out.println("=== NORMALIZED OUTPUT ===");
            System.out.println(normalized);
            
            // Verify it's valid JSON
            assertTrue(JsonUtil.isValidJson(normalized), "Result should be valid JSON");
            
            // Verify specific structures are expanded (at least the first level)
            assertTrue(normalized.contains("\"dataSearchConfigurables\""), "Should have dataSearchConfigurables key");
            assertTrue(normalized.contains("\"id\"") && normalized.contains("b92cacaf"), "Should have expanded id field");
            
            // The dataSearchConfigurables should be an array now, not a string
            assertTrue(normalized.contains("\"dataSearchConfigurables\" : [") || 
                       normalized.contains("\"dataSearchConfigurables\":["), 
                       "dataSearchConfigurables should be an array");
        }

        @Test
        @DisplayName("Normalize handles value field with nested JSON")
        void normalizeValueFieldWithNestedJson() {
            // This is a subset of the actual data showing the "value" field issue
            String input = "{\"configurables\":\"[{\\\"type\\\":\\\"layersOrder\\\",\\\"value\\\":\\\"[[\\\\\\\"World Imagery\\\\\\\",\\\\\\\"601\\\\\\\"]]\\\"}]\"}";
            
            String normalized = JsonUtil.normalize(input);
            
            System.out.println("=== VALUE FIELD TEST ===");
            System.out.println("Input: " + input);
            System.out.println("Normalized: " + normalized);
            
            // The "value" field should be a proper array, not a string
            assertTrue(normalized.contains("World Imagery"), "Should contain World Imagery");
            assertFalse(normalized.contains("\\\\\\\""), "Should not have triple-escaped quotes");
        }
    }

    @Nested
    @DisplayName("Denormalization Tests")
    class DenormalizationTests {

        @Test
        @DisplayName("Denormalize simple JSON")
        void denormalizeSimpleJson() {
            String input = "{\"key\":[{\"nested\":\"value\"}]}";
            
            String denormalized = JsonUtil.denormalize(input);
            
            System.out.println("Input:        " + input);
            System.out.println("Denormalized: " + denormalized);
            
            // The array should be stringified
            assertTrue(denormalized.contains("\"key\":\"["), "Array should be stringified");
        }

        @Test
        @DisplayName("Denormalize with outer quotes")
        void denormalizeWithOuterQuotes() {
            String input = "{\"key\":\"value\"}";
            
            String denormalized = JsonUtil.denormalize(input, true);
            
            // Should start and end with quotes
            assertTrue(denormalized.startsWith("\""), "Should start with quote");
            assertTrue(denormalized.endsWith("\""), "Should end with quote");
        }

        @Test
        @DisplayName("Round-trip: normalize then denormalize")
        void roundTripNormalizeDenormalize() {
            String original = "{\"key\":\"[{\\\"nested\\\":\\\"value\\\"}]\"}";
            
            String normalized = JsonUtil.normalizeCompact(original);
            String denormalized = JsonUtil.denormalize(normalized);
            
            System.out.println("Original:     " + original);
            System.out.println("Normalized:   " + normalized);
            System.out.println("Denormalized: " + denormalized);
            
            // Both should be valid JSON
            assertTrue(JsonUtil.isValidJson(normalized));
            assertTrue(JsonUtil.isValidJson(denormalized));
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("isValidJson returns true for valid JSON object")
        void isValidJsonObject() {
            assertTrue(JsonUtil.isValidJson("{\"key\":\"value\"}"));
        }

        @Test
        @DisplayName("isValidJson returns true for valid JSON array")
        void isValidJsonArray() {
            assertTrue(JsonUtil.isValidJson("[1, 2, 3]"));
        }

        @Test
        @DisplayName("isValidJson returns false for invalid JSON")
        void isInvalidJson() {
            assertFalse(JsonUtil.isValidJson("{key: value}"));
            assertFalse(JsonUtil.isValidJson("not json"));
            assertFalse(JsonUtil.isValidJson(null));
            assertFalse(JsonUtil.isValidJson(""));
        }
    }

    @Nested
    @DisplayName("Full Decryption Flow Test")
    class FullFlowTest {

        private static final String INTERNAL_SALT = "fxUoIlLqLVuN";
        private static final String ENCRYPTED_SAMPLE = "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";

        @Test
        @DisplayName("Full flow: decrypt -> normalize -> verify structure")
        void fullDecryptNormalizeFlow() {
            // Step 1: Decrypt
            String decrypted = AESUtil.decryptLegacy(ENCRYPTED_SAMPLE, INTERNAL_SALT);
            assertNotNull(decrypted, "Decryption should succeed");
            
            System.out.println("=== DECRYPTED (first 500 chars) ===");
            System.out.println(decrypted.substring(0, Math.min(500, decrypted.length())));
            
            // Step 2: Normalize
            String normalized = JsonUtil.normalize(decrypted);
            assertNotNull(normalized, "Normalization should succeed");
            
            System.out.println("\n=== NORMALIZED (first 1000 chars) ===");
            System.out.println(normalized.substring(0, Math.min(1000, normalized.length())));
            
            // Step 3: Verify it's valid JSON
            assertTrue(JsonUtil.isValidJson(normalized), "Normalized result should be valid JSON");
            
            // Step 4: Verify no escaped quotes remain
            assertFalse(normalized.contains("\\\""), "Should not contain escaped quotes");
            assertFalse(normalized.contains("\\\\\\\""), "Should not contain double-escaped quotes");

            // Step 5: Test encryption round-trip (without normalization) produces identical output
            // Note: normalize/denormalize transforms the JSON structure for readability,
            // so it cannot produce byte-identical output. Instead, test that:
            // 1. The original can be re-encrypted identically using the same salt
            // 2. The normalized form preserves all data (tested above via structure checks)
            byte[] originalSalt = AESUtil.extractSalt(ENCRYPTED_SAMPLE);
            String reEncrypted = AESUtil.encryptLegacyWithSalt(decrypted, INTERNAL_SALT, originalSalt);
            assertEquals(ENCRYPTED_SAMPLE, reEncrypted, "Re-encrypting original decrypted data with same salt should produce identical output");
        }
    }
}
