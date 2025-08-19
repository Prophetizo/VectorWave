package ai.prophetizo.wavelet.api;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for UI integration with wavelet selection.
 * Provides methods to get wavelets in various formats suitable for different UI frameworks.
 * 
 * <p>This class separates the internal enum representation from display strings,
 * following best practices for UI integration.</p>
 * 
 * @since 1.0
 */
public final class WaveletUIHelper {
    
    /**
     * Represents a UI choice item with display text and underlying value.
     * Useful for dropdown/combobox implementations.
     */
    public static class WaveletChoice {
        private final WaveletName value;
        private final String displayText;
        private final String description;
        private final String groupName;
        
        public WaveletChoice(WaveletName value) {
            this.value = value;
            this.displayText = formatDisplayText(value);
            this.description = value.getDescription();
            this.groupName = getFamily(value);
        }
        
        public WaveletName getValue() { return value; }
        public String getDisplayText() { return displayText; }
        public String getDescription() { return description; }
        public String getGroupName() { return groupName; }
        
        @Override
        public String toString() {
            return displayText;
        }
        
        private static String formatDisplayText(WaveletName name) {
            return String.format("%s - %s", 
                name.getCode().toUpperCase(), 
                name.getDescription());
        }
        
        private static String getFamily(WaveletName name) {
            String n = name.name();
            if (n.equals("HAAR")) return "Haar";
            if (n.startsWith("DB")) return "Daubechies";
            if (n.startsWith("SYM")) return "Symlet";
            if (n.startsWith("COIF")) return "Coiflet";
            if (name.getType() == WaveletType.CONTINUOUS) return "Continuous";
            if (name.getType() == WaveletType.COMPLEX) return "Complex";
            return "Other";
        }
    }
    
    // ============================================================
    // Methods for getting wavelets in UI-friendly formats
    // ============================================================
    
    /**
     * Get wavelets as a list of choice objects for object-oriented UIs.
     * @param transformType optional filter by transform compatibility
     * @return list of wavelet choices
     */
    public static List<WaveletChoice> getWaveletChoices(TransformType transformType) {
        List<WaveletName> wavelets = (transformType != null) 
            ? WaveletRegistry.getWaveletsForTransform(transformType)
            : List.of(WaveletName.values());
            
        return wavelets.stream()
            .map(WaveletChoice::new)
            .collect(Collectors.toList());
    }
    
    /**
     * Get wavelets grouped by family for hierarchical UIs.
     * @param transformType optional filter by transform compatibility
     * @return map of family name to list of choices
     */
    public static Map<String, List<WaveletChoice>> getWaveletChoicesGrouped(TransformType transformType) {
        return getWaveletChoices(transformType).stream()
            .collect(Collectors.groupingBy(
                WaveletChoice::getGroupName,
                LinkedHashMap::new,
                Collectors.toList()
            ));
    }
    
    /**
     * Get display strings only (for simple dropdowns).
     * @param wavelets list of wavelet names to convert
     * @return array of display strings
     */
    public static String[] getDisplayNames(List<WaveletName> wavelets) {
        return wavelets.stream()
            .map(name -> String.format("%s - %s", 
                name.getCode().toUpperCase(), 
                name.getDescription()))
            .toArray(String[]::new);
    }
    
    /**
     * Get a bidirectional mapping for lookups.
     * @param wavelets list of wavelet names
     * @return bidirectional map between display names and enum values
     */
    public static BidirectionalMap createDisplayMapping(List<WaveletName> wavelets) {
        return new BidirectionalMap(wavelets);
    }
    
    /**
     * Parse a wavelet from a display string.
     * @param displayName the display string from UI
     * @param wavelets the list of possible wavelets
     * @return the corresponding WaveletName, or null if not found
     */
    public static WaveletName parseFromDisplayName(String displayName, List<WaveletName> wavelets) {
        if (displayName == null) return null;
        
        // Try to extract the code from display format "CODE - Description"
        String code = displayName.split(" - ")[0].toLowerCase();
        
        return wavelets.stream()
            .filter(w -> w.getCode().equalsIgnoreCase(code))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Bidirectional mapping between display names and wavelet enums.
     */
    public static class BidirectionalMap {
        private final Map<String, WaveletName> displayToEnum;
        private final Map<WaveletName, String> enumToDisplay;
        
        public BidirectionalMap(List<WaveletName> wavelets) {
            displayToEnum = new LinkedHashMap<>();
            enumToDisplay = new LinkedHashMap<>();
            
            for (WaveletName name : wavelets) {
                String display = String.format("%s - %s", 
                    name.getCode().toUpperCase(), 
                    name.getDescription());
                displayToEnum.put(display, name);
                enumToDisplay.put(name, display);
            }
        }
        
        public WaveletName getEnum(String displayName) {
            return displayToEnum.get(displayName);
        }
        
        public String getDisplay(WaveletName waveletName) {
            return enumToDisplay.get(waveletName);
        }
        
        public Set<String> getDisplayNames() {
            return displayToEnum.keySet();
        }
        
        public Collection<WaveletName> getEnumValues() {
            return enumToDisplay.keySet();
        }
    }
    
    // ============================================================
    // Convenience methods for specific UI frameworks
    // ============================================================
    
    /**
     * Get wavelets formatted for JavaFX ComboBox.
     * @param transformType optional transform filter
     * @return list suitable for ObservableList
     */
    public static List<String> getForJavaFX(TransformType transformType) {
        return getWaveletChoices(transformType).stream()
            .map(WaveletChoice::getDisplayText)
            .collect(Collectors.toList());
    }
    
    /**
     * Get wavelets formatted for Swing JComboBox.
     * @param transformType optional transform filter
     * @return array suitable for JComboBox constructor
     */
    public static String[] getForSwing(TransformType transformType) {
        return getWaveletChoices(transformType).stream()
            .map(WaveletChoice::getDisplayText)
            .toArray(String[]::new);
    }
    
    /**
     * Get wavelets as JSON-friendly structure.
     * @param transformType optional transform filter
     * @return list of maps suitable for JSON serialization
     */
    public static List<Map<String, String>> getForJSON(TransformType transformType) {
        return getWaveletChoices(transformType).stream()
            .map(choice -> {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("value", choice.getValue().name());
                item.put("display", choice.getDisplayText());
                item.put("description", choice.getDescription());
                item.put("group", choice.getGroupName());
                return item;
            })
            .collect(Collectors.toList());
    }
    
    // ============================================================
    // Validation helpers
    // ============================================================
    
    /**
     * Validate that a wavelet selection is compatible with a transform.
     * @param waveletName the selected wavelet
     * @param transformType the intended transform
     * @throws IllegalArgumentException if incompatible
     */
    public static void validateSelection(WaveletName waveletName, TransformType transformType) {
        if (waveletName == null) {
            throw new IllegalArgumentException("No wavelet selected");
        }
        if (transformType == null) {
            throw new IllegalArgumentException("No transform specified");
        }
        if (!WaveletRegistry.isCompatible(waveletName, transformType)) {
            Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(waveletName);
            throw new IllegalArgumentException(String.format(
                "%s cannot be used with %s. Supported transforms: %s",
                waveletName.getDescription(),
                transformType.getDescription(),
                supported.stream()
                    .map(TransformType::getCode)
                    .collect(Collectors.joining(", "))
            ));
        }
    }
    
    private WaveletUIHelper() {
        // Utility class - prevent instantiation
    }
}