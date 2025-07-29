package ai.prophetizo.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Factory interface and its default methods.
 * 
 * This test class validates the contract and behavior of the Factory interface,
 * including default method implementations and expected interface behavior.
 */
class FactoryTest {
    
    /**
     * Test implementation of Factory for testing purposes.
     */
    private static class TestFactory implements Factory<String> {
        private boolean shouldThrow = false;
        
        public TestFactory(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }
        
        @Override
        public String create() {
            if (shouldThrow) {
                throw new IllegalStateException("Test exception");
            }
            return "test-product";
        }
        
        @Override
        public String getDescription() {
            return "Test factory description";
        }
        
        @Override
        public Class<String> getProductType() {
            return String.class;
        }
    }
    
    /**
     * Another test factory for registry testing.
     */
    private static class AnotherTestFactory implements Factory<Integer> {
        @Override
        public Integer create() {
            return 42;
        }
        
        @Override
        public Class<Integer> getProductType() {
            return Integer.class;
        }
    }
    
    private TestFactory testFactory;
    
    @BeforeEach
    void setUp() {
        testFactory = new TestFactory(false);
    }
    
    @Nested
    @DisplayName("Basic Factory Interface Tests")
    class BasicFactoryTests {
        
        @Test
        @DisplayName("Factory should create products successfully")
        void shouldCreateProductsSuccessfully() {
            String product = testFactory.create();
            assertNotNull(product);
            assertEquals("test-product", product);
        }
        
        @Test
        @DisplayName("Factory should handle creation exceptions")
        void shouldHandleCreationExceptions() {
            TestFactory throwingFactory = new TestFactory(true);
            assertThrows(IllegalStateException.class, throwingFactory::create);
        }
        
        @Test
        @DisplayName("Factory should provide meaningful description")
        void shouldProvideMeaningfulDescription() {
            String description = testFactory.getDescription();
            assertNotNull(description);
            assertFalse(description.trim().isEmpty());
            assertEquals("Test factory description", description);
        }
        
        @Test
        @DisplayName("Factory should return correct product type")
        void shouldReturnCorrectProductType() {
            Class<String> productType = testFactory.getProductType();
            assertNotNull(productType);
            assertEquals(String.class, productType);
        }
        
        @Test
        @DisplayName("Default description should be reasonable")
        void defaultDescriptionShouldBeReasonable() {
            Factory<String> factoryWithDefaultDescription = new Factory<String>() {
                @Override
                public String create() {
                    return "test";
                }
            };
            
            String description = factoryWithDefaultDescription.getDescription();
            assertNotNull(description);
            assertTrue(description.contains("Factory"));
        }
        
        @Test
        @DisplayName("Default product type should return Object.class")
        void defaultProductTypeShouldReturnObjectClass() {
            Factory<String> factoryWithDefaultType = new Factory<String>() {
                @Override
                public String create() {
                    return "test";
                }
            };
            
            Class<?> productType = factoryWithDefaultType.getProductType();
            assertNotNull(productType);
            assertEquals(Object.class, productType);
        }
    }
    
    @Nested
    @DisplayName("Factory Registry Tests")
    class FactoryRegistryTests {
        
        @BeforeEach
        void setUp() {
            // Reset registry to clean state for each test
            FactoryRegistry.reset();
        }
        
        @Test
        @DisplayName("Registry should have built-in factories after reset")
        void registryShouldHaveBuiltInFactoriesAfterReset() {
            Set<String> factoryNames = FactoryRegistry.getFactoryNames();
            
            assertTrue(factoryNames.contains("WaveletTransform"));
            assertTrue(factoryNames.contains("CWT"));
            assertTrue(factoryNames.contains("WaveletOps"));
            assertTrue(factoryNames.contains("StreamingDenoiser"));
            assertTrue(FactoryRegistry.getFactoryCount() >= 4);
        }
        
        @Test
        @DisplayName("Registry should allow custom factory registration")
        void registryShouldAllowCustomFactoryRegistration() {
            String factoryName = "TestFactory";
            TestFactory factory = new TestFactory(false);
            
            FactoryRegistry.registerFactory(factoryName, factory);
            
            assertTrue(FactoryRegistry.isRegistered(factoryName));
            Optional<Factory<?>> retrieved = FactoryRegistry.getFactory(factoryName);
            assertTrue(retrieved.isPresent());
            assertSame(factory, retrieved.get());
        }
        
        @Test
        @DisplayName("Registry should handle factory name validation")
        void registryShouldHandleFactoryNameValidation() {
            TestFactory factory = new TestFactory(false);
            
            assertThrows(NullPointerException.class, 
                () -> FactoryRegistry.registerFactory(null, factory));
            assertThrows(IllegalArgumentException.class,
                () -> FactoryRegistry.registerFactory("", factory));
            assertThrows(IllegalArgumentException.class,
                () -> FactoryRegistry.registerFactory("   ", factory));
            assertThrows(NullPointerException.class,
                () -> FactoryRegistry.registerFactory("test", null));
        }
        
        @Test
        @DisplayName("Registry should handle factory retrieval by type")
        void registryShouldHandleFactoryRetrievalByType() {
            // Register test factories
            FactoryRegistry.registerFactory("StringFactory", new TestFactory(false));
            FactoryRegistry.registerFactory("IntegerFactory", new AnotherTestFactory());
            
            // Get factories by type
            List<Factory<String>> stringFactories = FactoryRegistry.getFactoriesForType(String.class);
            List<Factory<Integer>> intFactories = FactoryRegistry.getFactoriesForType(Integer.class);
            
            assertEquals(1, stringFactories.size());
            assertEquals(1, intFactories.size());
            
            assertTrue(stringFactories.get(0) instanceof TestFactory);
            assertTrue(intFactories.get(0) instanceof AnotherTestFactory);
        }
        
        @Test
        @DisplayName("Registry should handle typed factory retrieval")
        void registryShouldHandleTypedFactoryRetrieval() {
            TestFactory factory = new TestFactory(false);
            FactoryRegistry.registerFactory("TestFactory", factory);
            
            // Should succeed with correct type
            Optional<Factory<String>> stringFactory = 
                FactoryRegistry.getFactory("TestFactory", String.class);
            assertTrue(stringFactory.isPresent());
            assertSame(factory, stringFactory.get());
            
            // Should fail with incorrect type
            Optional<Factory<Integer>> intFactory = 
                FactoryRegistry.getFactory("TestFactory", Integer.class);
            assertFalse(intFactory.isPresent());
        }
        
        @Test
        @DisplayName("Registry should handle factory unregistration")
        void registryShouldHandleFactoryUnregistration() {
            String factoryName = "TestFactory";
            FactoryRegistry.registerFactory(factoryName, new TestFactory(false));
            
            assertTrue(FactoryRegistry.isRegistered(factoryName));
            assertTrue(FactoryRegistry.unregisterFactory(factoryName));
            assertFalse(FactoryRegistry.isRegistered(factoryName));
            
            // Second unregistration should return false
            assertFalse(FactoryRegistry.unregisterFactory(factoryName));
        }
        
        @Test
        @DisplayName("Registry should handle clearing operations")
        void registryShouldHandleClearingOperations() {
            // Add custom factories
            FactoryRegistry.registerFactory("Custom1", new TestFactory(false));
            FactoryRegistry.registerFactory("Custom2", new AnotherTestFactory());
            
            int initialCount = FactoryRegistry.getFactoryCount();
            assertTrue(initialCount >= 6); // 4 built-in + 2 custom
            
            // Clear custom factories only
            FactoryRegistry.clearCustomFactories();
            assertEquals(4, FactoryRegistry.getFactoryCount()); // Only built-ins remain
            assertTrue(FactoryRegistry.isRegistered("WaveletTransform"));
            assertFalse(FactoryRegistry.isRegistered("Custom1"));
            
            // Clear all factories
            FactoryRegistry.clearAllFactories();
            assertEquals(0, FactoryRegistry.getFactoryCount());
        }
        
        @Test
        @DisplayName("Registry should handle null parameter validation")
        void registryShouldHandleNullParameterValidation() {
            assertThrows(NullPointerException.class, 
                () -> FactoryRegistry.getFactory(null));
            assertThrows(NullPointerException.class,
                () -> FactoryRegistry.getFactory(null, String.class));
            assertThrows(NullPointerException.class,
                () -> FactoryRegistry.getFactory("test", null));
            assertThrows(NullPointerException.class,
                () -> FactoryRegistry.getFactoriesForType(null));
            assertThrows(NullPointerException.class,
                () -> FactoryRegistry.isRegistered(null));
            assertThrows(NullPointerException.class,
                () -> FactoryRegistry.unregisterFactory(null));
        }
        
        @Test
        @DisplayName("Registry should be thread-safe")
        void registryShouldBeThreadSafe() throws InterruptedException {
            // This is a basic test for thread safety
            final int NUM_THREADS = 10;
            final int OPERATIONS_PER_THREAD = 100;
            
            Thread[] threads = new Thread[NUM_THREADS];
            
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String factoryName = "Thread" + threadId + "Factory" + j;
                        FactoryRegistry.registerFactory(factoryName, new TestFactory(false));
                        assertTrue(FactoryRegistry.isRegistered(factoryName));
                        FactoryRegistry.unregisterFactory(factoryName);
                    }
                });
            }
            
            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }
            
            // Wait for all threads to complete
            for (Thread thread : threads) {
                thread.join();
            }
            
            // Registry should still be in a consistent state
            assertTrue(FactoryRegistry.getFactoryCount() >= 4); // Built-in factories
        }
        
        @Test
        @DisplayName("Registry should provide comprehensive access methods")
        void registryShouldProvideComprehensiveAccessMethods() {
            // Test all access methods
            Collection<Factory<?>> allFactories = FactoryRegistry.getAllFactories();
            Set<String> factoryNames = FactoryRegistry.getFactoryNames();
            int factoryCount = FactoryRegistry.getFactoryCount();
            
            assertNotNull(allFactories);
            assertNotNull(factoryNames);
            assertEquals(allFactories.size(), factoryNames.size());
            assertEquals(allFactories.size(), factoryCount);
            
            // Collections should be unmodifiable
            assertThrows(UnsupportedOperationException.class,
                () -> allFactories.clear());
            assertThrows(UnsupportedOperationException.class,
                () -> factoryNames.clear());
        }
    }
}