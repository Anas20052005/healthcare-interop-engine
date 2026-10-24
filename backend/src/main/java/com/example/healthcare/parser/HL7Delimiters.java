package com.example.healthcare.parser;

/**
 * Encapsulates HL7 v2 delimiters defined in the MSH segment.
 * Defaults to standard HL7 delimiters: field '|', component '^', repetition '~', escape '\', subcomponent '&'.
 */
public class HL7Delimiters {

    private final String fieldSeparator;
    private final String componentSeparator;
    private final String repetitionSeparator;
    private final String escapeCharacter;
    private final String subcomponentSeparator;

    public HL7Delimiters(String fieldSeparator, String componentSeparator, String repetitionSeparator,
                         String escapeCharacter, String subcomponentSeparator) {
        this.fieldSeparator = fieldSeparator;
        this.componentSeparator = componentSeparator;
        this.repetitionSeparator = repetitionSeparator;
        this.escapeCharacter = escapeCharacter;
        this.subcomponentSeparator = subcomponentSeparator;
    }

    public static HL7Delimiters defaultDelimiters() {
        return new HL7Delimiters("|", "^", "~", "\\", "&");
    }

    /**
     * Extracts delimiters from the MSH segment string.
     * MSH-1 is the character at index 3 (field separator).
     * MSH-2 is the encoding characters starting at index 4.
     */
    public static HL7Delimiters fromMsh(String mshSegment) {
        if (mshSegment == null || mshSegment.length() < 4) {
            return defaultDelimiters();
        }

        String fieldSep = String.valueOf(mshSegment.charAt(3));
        String compSep = "^";
        String repSep = "~";
        String escChar = "\\";
        String subCompSep = "&";

        if (mshSegment.length() >= 5) {
            compSep = String.valueOf(mshSegment.charAt(4));
        }
        if (mshSegment.length() >= 6) {
            repSep = String.valueOf(mshSegment.charAt(5));
        }
        if (mshSegment.length() >= 7) {
            escChar = String.valueOf(mshSegment.charAt(6));
        }
        if (mshSegment.length() >= 8) {
            subCompSep = String.valueOf(mshSegment.charAt(7));
        }

        return new HL7Delimiters(fieldSep, compSep, repSep, escChar, subCompSep);
    }

    public String getFieldSeparator() {
        return fieldSeparator;
    }

    public String getComponentSeparator() {
        return componentSeparator;
    }

    public String getRepetitionSeparator() {
        return repetitionSeparator;
    }

    public String getEscapeCharacter() {
        return escapeCharacter;
    }

    public String getSubcomponentSeparator() {
        return subcomponentSeparator;
    }
}
