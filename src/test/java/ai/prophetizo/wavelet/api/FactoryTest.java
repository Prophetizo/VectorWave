package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Factory interface default methods and contracts.
 */
class FactoryTest {
    
    @Test
    void testDefaultIsValidConfiguration() {
        TestFactory factory = new TestFactory();
        assertTrue(factory.isValidConfiguration("valid"));
        assertTrue(factory.isValidConfiguration(null));
        assertTrue(factory.isValidConfiguration("anything"));
    }
    
    @Test
    void testDefaultGetDescription() {
        TestFactory factory = new TestFactory();
        assertEquals("TestFactory", factory.getDescription());
    }
    
    @Test
    void testFactoryInterface() {
        TestFactory factory = new TestFactory();
        
        // Test create() method
        String result1 = factory.create();
        assertEquals("default", result1);
        
        // Test create(config) method
        String result2 = factory.create("custom");
        assertEquals("custom", result2);
    }
    
    @Test
    void testFactoryWithNullConfig() {
        TestFactory factory = new TestFactory();
        String result = factory.create(null);
        assertEquals("null", result);
    }
    
    @Test
    void testFactoryWithValidation() {
        ValidatingFactory factory = new ValidatingFactory();
        
        // Valid configuration
        assertTrue(factory.isValidConfiguration("valid"));
        String result = factory.create("valid");
        assertEquals("valid", result);
        
        // Invalid configuration
        assertFalse(factory.isValidConfiguration("invalid"));
        assertThrows(IllegalArgumentException.class, () -> factory.create("invalid"));
    }
    
    @Test
    void testFactoryDescription() {
        DescriptiveFactory factory = new DescriptiveFactory();
        assertEquals("A test factory for demonstration", factory.getDescription());
    }
    
    // Test implementation that implements Factory interface
    private static class TestFactory implements Factory<String, String> {
        @Override
        public String create() {
            return "default";
        }
        
        @Override
        public String create(String config) {
            return config == null ? "null" : config;
        }
    }
    
    // Test implementation with custom validation
    private static class ValidatingFactory implements Factory<String, String> {
        @Override
        public String create() {
            return "default";
        }
        
        @Override
        public String create(String config) {
            if (!isValidConfiguration(config)) {
                throw new IllegalArgumentException("Invalid configuration: " + config);
            }
            return config;
        }
        
        @Override
        public boolean isValidConfiguration(String config) {
            return "valid".equals(config);
        }
    }
    
    // Test implementation with custom description
    private static class DescriptiveFactory implements Factory<String, String> {
        @Override
        public String create() {
            return "default";
        }
        
        @Override
        public String create(String config) {
            return config;
        }
        
        @Override
        public String getDescription() {
            return "A test factory for demonstration";
        }
    }
}