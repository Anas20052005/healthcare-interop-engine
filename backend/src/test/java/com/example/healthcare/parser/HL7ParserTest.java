package com.example.healthcare.parser;

import com.example.healthcare.dto.hl7.ParsedEncounter;
import com.example.healthcare.dto.hl7.ParsedHL7Data;
import com.example.healthcare.dto.hl7.ParsedObservation;
import com.example.healthcare.dto.hl7.ParsedPatient;
import com.example.healthcare.exception.InvalidHL7Exception;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HL7ParserTest {

    private HL7Parser parser;

    @BeforeEach
    void setUp() {
        parser = new DefaultHL7Parser();
    }

    @Test
    @DisplayName("1. Should parse a realistic complete synthetic ADT^A01 message")
    void testRealisticCompleteAdtA01Sample() {
        String message = "MSH|^~\\&|HIS|HOSPITAL|FHIR_ENGINE|HOSPITAL|202610080900||ADT^A01|MSG00001|P|2.5\r\n" +
                "PID|1||P1001||Sharma^Rahul||19950510|M\r\n" +
                "PV1|1|I|WARD1||||DOC001|||||||||||||||||||||||||||||||||||||202610080900|202610151200\r\n" +
                "OBX|1|NM|GLUCOSE||120|mg/dL|70-100||||F\r\n" +
                "OBX|2|NM|HEMOGLOBIN||13.5|g/dL|12-16||||N";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data);
        assertEquals("ADT^A01", data.getMessageType());
        assertEquals("MSG00001", data.getMessageControlId());
        assertEquals("2.5", data.getVersion());

        assertNotNull(data.getPatient());
        assertEquals("P1001", data.getPatient().getPatientId());
        assertEquals("Sharma", data.getPatient().getFamilyName());
        assertEquals("Rahul", data.getPatient().getGivenName());
        assertEquals("19950510", data.getPatient().getBirthDate());
        assertEquals("M", data.getPatient().getGender());

        assertNotNull(data.getEncounter());
        assertEquals("I", data.getEncounter().getEncounterClass());
        assertEquals("WARD1", data.getEncounter().getLocation());
        assertEquals("202610080900", data.getEncounter().getStart());
        assertEquals("202610151200", data.getEncounter().getEnd());

        assertEquals(2, data.getObservations().size());

        ParsedObservation obs1 = data.getObservations().get(0);
        assertEquals("NM", obs1.getValueType());
        assertEquals("GLUCOSE", obs1.getObservationIdentifier());
        assertEquals("120", obs1.getObservationValue());
        assertEquals("mg/dL", obs1.getUnits());
        assertEquals("70-100", obs1.getReferenceRange());
        assertEquals("F", obs1.getStatus());

        ParsedObservation obs2 = data.getObservations().get(1);
        assertEquals("NM", obs2.getValueType());
        assertEquals("HEMOGLOBIN", obs2.getObservationIdentifier());
        assertEquals("13.5", obs2.getObservationValue());
        assertEquals("g/dL", obs2.getUnits());
        assertEquals("12-16", obs2.getReferenceRange());
        assertEquals("N", obs2.getStatus());
    }

    @Test
    @DisplayName("2. Should extract MSH fields accurately")
    void testMshExtraction() {
        String message = "MSH|^~\\&|APP_A|FAC_A|APP_B|FAC_B|202610081200||ADT^A01|CTRL-998877|P|2.5\r\n" +
                "PID|1||P2002||Gupta^Anjali||19900101|F";

        ParsedHL7Data data = parser.parse(message);

        assertEquals("ADT^A01", data.getMessageType());
        assertEquals("CTRL-998877", data.getMessageControlId());
        assertEquals("2.5", data.getVersion());
    }

    @Test
    @DisplayName("3. Should extract PID patient identifier")
    void testPidExtraction() {
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG101|P|2.5\r\n" +
                "PID|1||MRN-554433||Patel^Vikram||19851120|M";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data.getPatient());
        assertEquals("MRN-554433", data.getPatient().getPatientId());
    }

    @Test
    @DisplayName("4. Should extract PID name components: family and given name")
    void testPidNameComponentExtraction() {
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG102|P|2.5\r\n" +
                "PID|1||P3003||Deshmukh^Rohan^K^Dr||19820412|M";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data.getPatient());
        assertEquals("Deshmukh", data.getPatient().getFamilyName());
        assertEquals("Rohan", data.getPatient().getGivenName());
    }

    @Test
    @DisplayName("5. Should extract PID birth date as raw structured value")
    void testPidBirthDateExtraction() {
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG103|P|2.5\r\n" +
                "PID|1||P4004||Iyer^Meera||20010725|F";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data.getPatient());
        assertEquals("20010725", data.getPatient().getBirthDate());
    }

    @Test
    @DisplayName("6. Should extract PID gender as raw supplied string without conversion")
    void testPidGenderExtraction() {
        String messageMale = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG104|P|2.5\r\n" +
                "PID|1||P5001||Khan^Zaid||19980315|M";
        ParsedHL7Data dataMale = parser.parse(messageMale);
        assertEquals("M", dataMale.getPatient().getGender());

        String messageFemale = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG105|P|2.5\r\n" +
                "PID|1||P5002||Khan^Sara||19990812|F";
        ParsedHL7Data dataFemale = parser.parse(messageFemale);
        assertEquals("F", dataFemale.getPatient().getGender());
    }

    @Test
    @DisplayName("7. Should extract PV1 encounter fields accurately")
    void testPv1Extraction() {
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG106|P|2.5\r\n" +
                "PID|1||P6001||Nair^Suresh||19751210|M\r\n" +
                "PV1|1|E|EMERGENCY_ROOM|||||||||||||||||||||||||||||||||||||||||202610081030|202610081430";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data.getEncounter());
        assertEquals("E", data.getEncounter().getEncounterClass());
        assertEquals("EMERGENCY_ROOM", data.getEncounter().getLocation());
        assertEquals("202610081030", data.getEncounter().getStart());
        assertEquals("202610081430", data.getEncounter().getEnd());
    }

    @Test
    @DisplayName("8. Should support multiple OBX segments")
    void testMultipleObxSegments() {
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG107|P|2.5\r\n" +
                "PID|1||P7001||Bose^Sunita||19890614|F\r\n" +
                "OBX|1|NM|WBC||6.5|10*3/uL|4.0-11.0||||F\r\n" +
                "OBX|2|NM|RBC||4.8|10*6/uL|4.2-5.4||||F\r\n" +
                "OBX|3|ST|BLOOD_GROUP||O_POSITIVE||||||F";

        ParsedHL7Data data = parser.parse(message);

        assertEquals(3, data.getObservations().size());
        assertEquals("WBC", data.getObservations().get(0).getObservationIdentifier());
        assertEquals("RBC", data.getObservations().get(1).getObservationIdentifier());
        assertEquals("BLOOD_GROUP", data.getObservations().get(2).getObservationIdentifier());
        assertEquals("O_POSITIVE", data.getObservations().get(2).getObservationValue());
    }

    @Test
    @DisplayName("9. Should extract all approved OBX fields")
    void testObxFieldExtraction() {
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG108|P|2.5\r\n" +
                "PID|1||P8001||Chopra^Karan||19920202|M\r\n" +
                "OBX|1|NM|CHOLESTEROL||185|mg/dL|<200||||F";

        ParsedHL7Data data = parser.parse(message);

        assertEquals(1, data.getObservations().size());
        ParsedObservation obs = data.getObservations().get(0);
        assertEquals("NM", obs.getValueType());
        assertEquals("CHOLESTEROL", obs.getObservationIdentifier());
        assertEquals("185", obs.getObservationValue());
        assertEquals("mg/dL", obs.getUnits());
        assertEquals("<200", obs.getReferenceRange());
        assertEquals("F", obs.getStatus());
    }

    @Test
    @DisplayName("10. Should tolerate missing optional fields and segments without crashing")
    void testOptionalFieldsAbsent() {
        // Message with no PV1 end time, no PID name given, no OBX segments
        String message = "MSH|^~\\&|APP|FAC|RCV|FAC|202610081200||ADT^A01|MSG109|P|2.5\r\n" +
                "PID|1||P9001||Anonymous|||\r\n" +
                "PV1|1|O|CLINIC_1";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data);
        assertEquals("P9001", data.getPatient().getPatientId());
        assertEquals("Anonymous", data.getPatient().getFamilyName());
        assertNull(data.getPatient().getGivenName());
        assertNull(data.getPatient().getBirthDate());
        assertNull(data.getPatient().getGender());

        assertNotNull(data.getEncounter());
        assertEquals("O", data.getEncounter().getEncounterClass());
        assertEquals("CLINIC_1", data.getEncounter().getLocation());
        assertNull(data.getEncounter().getStart());
        assertNull(data.getEncounter().getEnd());

        assertTrue(data.getObservations().isEmpty());
    }

    @Test
    @DisplayName("11. Should throw InvalidHL7Exception on null or empty input")
    void testNullAndEmptyInputHandling() {
        assertThrows(InvalidHL7Exception.class, () -> parser.parse(null));
        assertThrows(InvalidHL7Exception.class, () -> parser.parse(""));
        assertThrows(InvalidHL7Exception.class, () -> parser.parse("   \r\n  "));
        assertThrows(InvalidHL7Exception.class, () -> parser.parse("INVALID_SEGMENT|1|2|3"));
    }

    @Test
    @DisplayName("12. Should correctly handle HL7 field indexing without off-by-one errors")
    void testHl7FieldIndexing() {
        // Explicitly verifies MSH-1 delimiter offset and standard segment index alignment
        String message = "MSH|^~\\&|SEND_APP|SEND_FAC|RECV_APP|RECV_FAC|202610080000||ADT^A01|CTRL-IDX-01|P|2.5\r\n" +
                "PID|1||ID-FIELD-3||NAME-FIELD-5||DOB-FIELD-7|GENDER-FIELD-8\r\n" +
                "PV1|1|CLASS-FIELD-2|LOC-FIELD-3\r\n" +
                "OBX|1|TYPE-FIELD-2|CODE-FIELD-3||VAL-FIELD-5|UNIT-FIELD-6|RANGE-FIELD-7||||STATUS-FIELD-11";

        ParsedHL7Data data = parser.parse(message);

        assertEquals("ADT^A01", data.getMessageType(), "MSH-9 must match message type");
        assertEquals("CTRL-IDX-01", data.getMessageControlId(), "MSH-10 must match message control ID");
        assertEquals("2.5", data.getVersion(), "MSH-12 must match version ID");

        assertEquals("ID-FIELD-3", data.getPatient().getPatientId(), "PID-3 must match patientId");
        assertEquals("NAME-FIELD-5", data.getPatient().getFamilyName(), "PID-5 component 0 must match familyName");
        assertEquals("DOB-FIELD-7", data.getPatient().getBirthDate(), "PID-7 must match birthDate");
        assertEquals("GENDER-FIELD-8", data.getPatient().getGender(), "PID-8 must match gender");

        assertEquals("CLASS-FIELD-2", data.getEncounter().getEncounterClass(), "PV1-2 must match encounterClass");
        assertEquals("LOC-FIELD-3", data.getEncounter().getLocation(), "PV1-3 must match location");

        ParsedObservation obs = data.getObservations().get(0);
        assertEquals("TYPE-FIELD-2", obs.getValueType(), "OBX-2 must match valueType");
        assertEquals("CODE-FIELD-3", obs.getObservationIdentifier(), "OBX-3 must match observationIdentifier");
        assertEquals("VAL-FIELD-5", obs.getObservationValue(), "OBX-5 must match observationValue");
        assertEquals("UNIT-FIELD-6", obs.getUnits(), "OBX-6 must match units");
        assertEquals("RANGE-FIELD-7", obs.getReferenceRange(), "OBX-7 must match referenceRange");
        assertEquals("STATUS-FIELD-11", obs.getStatus(), "OBX-11 must match status");
    }

    @Test
    @DisplayName("13. Should tolerate additional unsupported segments safely")
    void testTolerateAdditionalIgnoredSegments() {
        String message = "MSH|^~\\&|HIS|HOSPITAL|FHIR_ENGINE|HOSPITAL|202610080900||ADT^A01|MSG00001|P|2.5\r\n" +
                "EVN|A01|202610080900\r\n" +
                "PID|1||P1001||Sharma^Rahul||19950510|M\r\n" +
                "NK1|1|Sharma^Pooja|SPO||||NOK\r\n" +
                "PV1|1|I|WARD1\r\n" +
                "DG1|1||ICD10^E11.9|||A\r\n" +
                "OBX|1|NM|GLUCOSE||120|mg/dL|70-100||||F";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data);
        assertEquals("P1001", data.getPatient().getPatientId());
        assertEquals("I", data.getEncounter().getEncounterClass());
        assertEquals(1, data.getObservations().size());
    }

    @Test
    @DisplayName("14. Should handle different line terminators: CR, LF, and CRLF")
    void testDifferentLineEndings() {
        String crMessage = "MSH|^~\\&|HIS|HOSPITAL|RCV|FAC|202610080900||ADT^A01|MSG-CR|P|2.5\r" +
                "PID|1||P-CR||Test^CR||19900101|M";
        ParsedHL7Data crData = parser.parse(crMessage);
        assertEquals("MSG-CR", crData.getMessageControlId());
        assertEquals("P-CR", crData.getPatient().getPatientId());

        String lfMessage = "MSH|^~\\&|HIS|HOSPITAL|RCV|FAC|202610080900||ADT^A01|MSG-LF|P|2.5\n" +
                "PID|1||P-LF||Test^LF||19900101|F";
        ParsedHL7Data lfData = parser.parse(lfMessage);
        assertEquals("MSG-LF", lfData.getMessageControlId());
        assertEquals("P-LF", lfData.getPatient().getPatientId());
    }

    @Test
    @DisplayName("15. Should support sample message with truncated trailing OBX status")
    void testSampleMessageWithShortObx() {
        String message = "MSH|^~\\&|HIS|HOSPITAL|FHIR_ENGINE|HOSPITAL|202610080900||ADT^A01|MSG00001|P|2.5\r\n" +
                "PID|1||P1001||Sharma^Rahul||19950510|M\r\n" +
                "PV1|1|I|WARD1||||DOC001\r\n" +
                "OBX|1|NM|GLUCOSE||120|mg/dL|70-100||H\r\n" +
                "OBX|2|NM|HEMOGLOBIN||13.5|g/dL|12-16||N";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data);
        assertEquals(2, data.getObservations().size());
        assertEquals("H", data.getObservations().get(0).getStatus());
        assertEquals("N", data.getObservations().get(1).getStatus());
    }

    @Test
    @DisplayName("16. Should parse numeric OBX segment accurately")
    void testNumericObxParsing() {
        String message = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG-NM|P|2.5\r" +
                "PID|1||P-NM||Test^Numeric||19900101|M\r" +
                "OBX|1|NM|TEMP^Body Temperature||98.6|degF|97.0-99.0||||F";

        ParsedHL7Data data = parser.parse(message);

        assertEquals(1, data.getObservations().size());
        ParsedObservation obs = data.getObservations().get(0);
        assertEquals("NM", obs.getValueType());
        assertEquals("TEMP^Body Temperature", obs.getObservationIdentifier());
        assertEquals("98.6", obs.getObservationValue());
        assertEquals("degF", obs.getUnits());
        assertEquals("97.0-99.0", obs.getReferenceRange());
        assertEquals("F", obs.getStatus());
    }

    @Test
    @DisplayName("17. Should parse string OBX segment accurately")
    void testStringObxParsing() {
        String message = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG-ST|P|2.5\r" +
                "PID|1||P-ST||Test^String||19900101|F\r" +
                "OBX|1|ST|NOTE^Clinical Note||Patient is alert and stable||||||F";

        ParsedHL7Data data = parser.parse(message);

        assertEquals(1, data.getObservations().size());
        ParsedObservation obs = data.getObservations().get(0);
        assertEquals("ST", obs.getValueType());
        assertEquals("NOTE^Clinical Note", obs.getObservationIdentifier());
        assertEquals("Patient is alert and stable", obs.getObservationValue());
        assertEquals("F", obs.getStatus());
    }

    @Test
    @DisplayName("18. Should successfully parse message when optional PV1 and OBX are absent")
    void testMissingOptionalSegments() {
        String message = "MSH|^~\\&|HIS|HOSPITAL|FHIR|SERVER|202610080900||ADT^A01|MSG-NOPV1|P|2.5\r" +
                "PID|1||P-ONLY||Solo^Patient||19850615|M";

        ParsedHL7Data data = parser.parse(message);

        assertNotNull(data);
        assertEquals("P-ONLY", data.getPatient().getPatientId());
        assertNull(data.getEncounter(), "Encounter should be null when PV1 is omitted");
        assertTrue(data.getObservations().isEmpty(), "Observations should be empty when OBX is omitted");
    }

    @Test
    @DisplayName("19. Should throw InvalidHL7Exception on malformed HL7 messages")
    void testMalformedHl7InputScenarios() {
        // Missing MSH prefix
        assertThrows(InvalidHL7Exception.class, () -> parser.parse("PID|1||P1001||Smith^John"));

        // Malformed MSH delimiter length
        assertThrows(InvalidHL7Exception.class, () -> parser.parse("MSH"));
        assertThrows(InvalidHL7Exception.class, () -> parser.parse("MSH|"));

        // Blank or whitespace segments only
        assertThrows(InvalidHL7Exception.class, () -> parser.parse("   \r\n   "));
    }
}
