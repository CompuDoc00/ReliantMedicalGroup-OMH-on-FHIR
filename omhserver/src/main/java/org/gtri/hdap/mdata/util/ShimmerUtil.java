package org.gtri.hdap.mdata.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by es130 on 8/6/2018.
 */
public class ShimmerUtil {

    /*========================================================================*/
    /* Constants */
    /*========================================================================*/
    public static String OMH_ON_FHIR_CALLBACK_ENV = "OMH_ON_FHIR_CALLBACK";
    public static String OMH_ON_FHIR_LOGIN_ENV = "OMH_ON_FHIR_LOGIN";
    public static String PATIENT_RESOURCE_ID = "p";
    public static String PATIENT_IDENTIFIER_SYSTEM = "https://omh.org/shimmer/patient_ids";
    public static String OBSERVATION_CATEGORY_SYSTEM = "https://snomed.info.sct";
    //public static String OBSERVATION_CATEGORY_CODE = "68130003";                                              //*LDG Other than for TEST, these are replaced below
    //public static String OBSERVATION_CATEGORY_DISPLAY = "Physical activity (observable entity)";              //*LDG Other than for TEST, these are replaced below
    public static String OBSERVATION_CODE_SYSTEM = "http://loinc.org";
    //public static String OBSERVATION_CODE_CODE = "55423-8";                                                   //*LDG Other than for TEST, these are replaced below
    //public static String OBSERVATION_CODE_DISPLAY = "Number of steps in unspecified time Pedometer";          //*LDG Other than for TEST, these are replaced below
    //public static String OBSERVATION_COMPONENT_CODE_SYSTEM = "http://hl7.org/fhir/observation-statistics";
    //public static String OBSERVATION_COMPONENT_CODE_CODE = "maximum";
    //public static String OBSERVATION_COMPONENT_CODE_DISPLAY = "Maximum";
    //public static String OBSERVATION_COMPONENT_CODE_TEXT = "Maximum";
    //public static String OBSERVATION_COMPONENT_VALUE_CODE_UNIT = "/{tot}";
    public static String OBSERVATION_COMPONENT_VALUE_CODE_SYSTEM = "http://unitsofmeasure.org";
    //public static String OBSERVATION_COMPONENT_VALUE_CODE_CODE = "{steps}/{tot}";
    public static final Map<String, String> CATEGORY_CODE_MAP = new HashMap<>();
         static {
             CATEGORY_CODE_MAP.put("step_count", "68130003");    //https://www.openmhealth.org/documentation/#/schema-docs/schema-library/schemas/omh_physical-activity
             CATEGORY_CODE_MAP.put("body_weight", "363808001");  //https://www.openmhealth.org/documentation/#/schema-docs/schema-library/schemas/omh_body-weight
             CATEGORY_CODE_MAP.put("blood_pressure", "75367002");  //https://www.openmhealth.org/documentation/#/schema-docs/schema-library/schemas/omh_blood-pressure
             CATEGORY_CODE_MAP.put("heart_rate", "78564009");  //https://www.openmhealth.org/documentation/#/schema-docs/schema-library/schemas/omh_heart-rate
             CATEGORY_CODE_MAP.put("oxygen_saturation", "431314004");  //https://www.openmhealth.org/documentation/#/schema-docs/schema-library/schemas/omh_oxygen-saturation
    }
    public static final Map<String, String> CATEGORY_DISPLAY_MAP = new HashMap<>();
        static {
            CATEGORY_DISPLAY_MAP.put("step_count", "Physical activity (observable entity)");
            CATEGORY_DISPLAY_MAP.put("body_weight", "Body weight measure (observable entity)");
            CATEGORY_DISPLAY_MAP.put("blood_pressure", "Blood pressure (observable entity)");
            CATEGORY_DISPLAY_MAP.put("heart_rate", "Pulse rate (observable entity)");
            CATEGORY_DISPLAY_MAP.put("oxygen_saturation", "Oxygen saturation by pulse oximetry");
    }
    public static final Map<String, String> CODE_CODE_MAP = new HashMap<>();
        static {
            CODE_CODE_MAP.put("step_count", "55423-8");
            CODE_CODE_MAP.put("body_weight", "29463-7");   //http://hl7.org/fhir/bodyweight.html
            CODE_CODE_MAP.put("blood_pressure", "85354-9");
            CODE_CODE_MAP.put("heart_rate", "8867-4");
            CODE_CODE_MAP.put("oxygen_saturation", "59408-5");
    }
    public static final Map<String, String> CODE_DISPLAY_MAP = new HashMap<>();
        static {
            CODE_DISPLAY_MAP.put("step_count", "Number of steps in unspecified time by Pedometer");
            CODE_DISPLAY_MAP.put("body_weight", "Body weight");
            CODE_DISPLAY_MAP.put("blood_pressure", "Blood pressure");
            CODE_DISPLAY_MAP.put("heart_rate", "Heart rate");
            CODE_DISPLAY_MAP.put("oxygen_saturation", "Oxygen saturation");
    }
    public static final Map<String, String> COMPONENT_CODE_SYSTEM_MAP = new HashMap<>();
        static {
            COMPONENT_CODE_SYSTEM_MAP.put("step_count", "http://loinc.org");
            COMPONENT_CODE_SYSTEM_MAP.put("body_weight", "http://loinc.org");
            COMPONENT_CODE_SYSTEM_MAP.put("blood_pressure", "http://loinc.org");
            COMPONENT_CODE_SYSTEM_MAP.put("systolic_blood_pressure", "http://loinc.org");
            COMPONENT_CODE_SYSTEM_MAP.put("diastolic_blood_pressure", "http://loinc.org");
            COMPONENT_CODE_SYSTEM_MAP.put("heart_rate", "http://loinc.org");
            COMPONENT_CODE_SYSTEM_MAP.put("oxygen_saturation", "http://loinc.org");
    }
    public static final Map<String, String> COMPONENT_CODE_CODE_MAP = new HashMap<>();
    static {
        COMPONENT_CODE_CODE_MAP.put("step_count", "55423-8");
        COMPONENT_CODE_CODE_MAP.put("body_weight", "29463-7");
        COMPONENT_CODE_CODE_MAP.put("blood_pressure", "85354-9");
        COMPONENT_CODE_CODE_MAP.put("systolic_blood_pressure", "8480-6");
        COMPONENT_CODE_CODE_MAP.put("diastolic_blood_pressure", "8462-4");
        COMPONENT_CODE_CODE_MAP.put("heart_rate", "8867-4");
        COMPONENT_CODE_CODE_MAP.put("oxygen_saturation", "59408-5");
    }
    public static final Map<String, String> COMPONENT_CODE_DISPLAY_MAP = new HashMap<>();
    static {
        COMPONENT_CODE_DISPLAY_MAP.put("step_count", "Steps in time period");
        COMPONENT_CODE_DISPLAY_MAP.put("body_weight", "Body weight");
        COMPONENT_CODE_DISPLAY_MAP.put("blood_pressure", "Blood pressure");
        COMPONENT_CODE_DISPLAY_MAP.put("systolic_blood_pressure", "Systolic blood pressure");
        COMPONENT_CODE_DISPLAY_MAP.put("diastolic_blood_pressure", "Diastolic blood pressure");
        COMPONENT_CODE_DISPLAY_MAP.put("heart_rate", "Heart rate");
        COMPONENT_CODE_DISPLAY_MAP.put("oxygen_saturation", "Oxygen saturation");
    }
    public static final Map<String, String> COMPONENT_CODE_TEXT_MAP = new HashMap<>();
    static {
        COMPONENT_CODE_TEXT_MAP.put("step_count", "Number of steps in unspecified time by Pedometer");
        COMPONENT_CODE_TEXT_MAP.put("body_weight", "Body weight");
        COMPONENT_CODE_TEXT_MAP.put("blood_pressure", "Blood pressure");
        COMPONENT_CODE_TEXT_MAP.put("systolic_blood_pressure", "Systolic blood pressure");
        COMPONENT_CODE_TEXT_MAP.put("diastolic_blood_pressure", "Diastolic blood pressure");
        COMPONENT_CODE_TEXT_MAP.put("heart_rate", "Heart rate");
        COMPONENT_CODE_TEXT_MAP.put("oxygen_saturation", "Oxygen saturation");
    }
    public static final Map<String, String> VALUE_CODE_UNIT_MAP = new HashMap<>();
    static {
        VALUE_CODE_UNIT_MAP.put("step_count", "{steps}/{tot}");
        VALUE_CODE_UNIT_MAP.put("body_weight", "<units>");
        VALUE_CODE_UNIT_MAP.put("blood_pressure", "<units>");
        VALUE_CODE_UNIT_MAP.put("systolic_blood_pressure", "<units>");
        VALUE_CODE_UNIT_MAP.put("diastolic_blood_pressure", "<units>");
        VALUE_CODE_UNIT_MAP.put("heart_rate", "<units>");
        VALUE_CODE_UNIT_MAP.put("oxygen_saturation", "<units>");
    }
    public static final Map<String, String> VALUE_CODE_CODE_MAP = new HashMap<>();
        static {
            VALUE_CODE_CODE_MAP.put("step_count", "{steps}/{tot}");
            VALUE_CODE_CODE_MAP.put("body_weight", "<units>");
            VALUE_CODE_CODE_MAP.put("blood_pressure", "<units>");
            VALUE_CODE_CODE_MAP.put("systolic_blood_pressure", "<units>");
            VALUE_CODE_CODE_MAP.put("diastolic_blood_pressure", "<units>");
            VALUE_CODE_CODE_MAP.put("heart_rate", "<units>");
            VALUE_CODE_CODE_MAP.put("oxygen_saturation", "<units>");
    }
    public static final Map<String, String> VALUE_UNIT_TYPE_MAP = new HashMap<>();
        static {
            VALUE_UNIT_TYPE_MAP.put("step_count", "DateRange");
            VALUE_UNIT_TYPE_MAP.put("body_weight", "SingleDate");
            VALUE_UNIT_TYPE_MAP.put("blood_pressure", "SingleDate");
            VALUE_UNIT_TYPE_MAP.put("systolic_blood_pressure", "SingleDate");
            VALUE_UNIT_TYPE_MAP.put("diastolic_blood_pressure", "SingleDate");
            VALUE_UNIT_TYPE_MAP.put("heart_rate", "SingleDate");
            VALUE_UNIT_TYPE_MAP.put("oxygen_saturation", "SingleDate");
    }
}
