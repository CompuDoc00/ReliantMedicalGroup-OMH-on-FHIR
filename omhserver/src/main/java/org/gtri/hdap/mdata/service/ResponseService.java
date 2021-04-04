package org.gtri.hdap.mdata.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUser;
import org.gtri.hdap.mdata.jpa.entity.ApplicationUserId;
import org.gtri.hdap.mdata.jpa.entity.ShimmerData;
import org.gtri.hdap.mdata.jpa.repository.ApplicationUserRepository;
import org.gtri.hdap.mdata.jpa.repository.ShimmerDataRepository;
import org.gtri.hdap.mdata.util.ShimmerUtil;
import org.hl7.fhir.dstu3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.ZonedDateTime;

/**
 * Created by es130 on 9/14/2018.
 */
@Service
public class ResponseService {

    /*========================================================================*/
    /* Constants */
    /*========================================================================*/


    /*========================================================================*/
    /* Constructors */
    /*========================================================================*/
    public ResponseService() {
    }

    /*========================================================================*/
    /* Variables */
    /*========================================================================*/
    @Autowired
    private ApplicationUserRepository applicationUserRepository;
    @Autowired
    private ShimmerDataRepository shimmerDataRepository;
    private final Logger logger = LoggerFactory.getLogger(ResponseService.class);
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private SimpleDateFormat sdfNoMilli = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private SimpleDateFormat sdfColon = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private SimpleDateFormat sdfNoMilliColon = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");  //*LDG For format like 2020-08-01T09:44:55-04:00
    private static final Integer timeWithoutNanoseconds = 23;     //*LDG This is how many characters the timestamp is when it just has milliseconds. Longer than this has nanoseconds which we need to truncate...

    /*========================================================================*/
    /* Public Methods */
    /*========================================================================*/
    public DocumentReference generateDocumentReference(String documentId, String shimKey){
        DocumentReference documentReference = null;
        if(!documentId.isEmpty()) {
            String title = "OMH " + shimKey + " data";
            String binaryRef = "Binary/" + documentId;
            Date creationDate = new Date();
            //create a DocumentReference to return
            documentReference = new DocumentReference();
            documentReference.setStatus(Enumerations.DocumentReferenceStatus.CURRENT);
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.setText(title);
            documentReference.setType(codeableConcept);
            documentReference.setIndexed(creationDate);
            DocumentReference.DocumentReferenceContentComponent documentReferenceContentComponent = new DocumentReference.DocumentReferenceContentComponent();
            //create an Attachment that has a URL for the data
            Attachment attachment = new Attachment();
            attachment.setContentType("application/json");
            attachment.setUrl(binaryRef);
            attachment.setTitle(title);
            attachment.setCreation(creationDate);
            documentReferenceContentComponent.setAttachment(attachment);
            //add attachment to DocumentReference
            List<DocumentReference.DocumentReferenceContentComponent> documentContent = new ArrayList<DocumentReference.DocumentReferenceContentComponent>();
            documentContent.add(documentReferenceContentComponent);
            documentReference.setContent(documentContent);
        }
        return documentReference;
    }

    public List<Resource> generateObservationList(String shimKey, String jsonResponse, String dataType) throws IOException {
        //parse JSON response. It is of format
        //sample response
        //{
        //    "shim": "googlefit",
        //    "timeStamp": 1534251049,
        //    "body": [
        //    {
        //        "header": {
        //            "id": "3b9b68a2-e0fd-4bdd-ba85-4127a4e8bcee",
        //            "creation_date_time": "2018-08-14T12:50:49.383Z",
        //            "acquisition_provenance": {
        //                "source_name": "some application",
        //                "source_origin_id": "some device"
        //            },
        //            "schema_id": {
        //                "namespace": "omh",
        //                "name": "step-count",
        //                "version": "2.0"
        //            }
        //        },
        //        "body": {
        //            "effective_time_frame": {
        //                "time_interval": {
        //                    "start_date_time": "2018-08-14T00:00:17.805Z",
        //                    "end_date_time": "2018-08-14T00:01:17.805Z"
        //                }
        //            },
        //            "step_count": 7
        //        }
        //    },
        // ...
        //    ]
        //}
        //   FOR BODY_WEIGHT, HERE IS THE DIFFERENCE:
        //        "body":{
        //            "effective_time_frame":{
        //                "date_time":"2020-07-25T13:47:01.326Z"
        //            },
        //            "body_weight":{
        //                "unit":"kg",
        //                "value":68.49239349365234
        //            }
        //        }
        //   FOR BLOOD_PRESSURE, HERE IS THE DIFFERENCE:
        //        "body":{
        //            "effective_time_frame":{
        //                "date_time":"2020-07-25T13:47:01.326Z"
        //            },
        //            "systolic_blood_pressure":{
        //                "unit":"mmHg",
        //                "value":120
        //            }
        //            "diastolic_blood_pressure":{
        //                "unit":"mmHg",
        //                "value":70
        //            }
        //        }
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        List<Resource> observationList = new ArrayList<Resource>();
        Observation observation;

        int observations = 0;

        logger.debug("Looking through JSON nodes");
        long timestamp = jsonNode.get("timeStamp").asLong();
        logger.debug("timestamp: " + timestamp);

        for( JsonNode omhStepCount : jsonNode.get("body")){
            logger.debug("Looking at json node");
            logger.debug(omhStepCount.toString());
            observation = generateObservation(shimKey, timestamp, omhStepCount, dataType);
            observationList.add(observation);
            ++observations;
        }
        logger.debug("Done building observationList of '{}' observations",observations);
        return observationList;
    }

    public Observation generateObservation(String shimKey, long timestamp, JsonNode omhStepCount, String dataType){
        logger.debug("Generating Observation");
        JsonNode omhStepCountBody = omhStepCount.get("body");
        logger.debug("got body");

        JsonNode omhStepCountHeader = omhStepCount.get("header");
        logger.debug("got header");

        String identifier = getJsonNodeText(omhStepCountHeader.get("id"));
        logger.debug("identifier: " + identifier);

        String deviceSource = getJsonNodeText(omhStepCountHeader.get("acquisition_provenance").get("source_name"));
        logger.debug("deviceSource: " + deviceSource);

        String deviceOrigin = getJsonNodeText(omhStepCountHeader.get("acquisition_provenance").get("source_origin_id"));
        logger.debug("deviceOrigin: " + deviceOrigin);

        List<String> deviceInfoList = new ArrayList<String>();
        deviceInfoList.add(deviceSource);
        deviceInfoList.add(deviceOrigin);
        deviceInfoList.add(Long.toString(timestamp));
        String deviceInfo = String.join(",", deviceInfoList);

        Observation observation = new Observation();
        //set Id
        observation.setId(UUID.randomUUID().toString());

        //set patient
        Patient patient = generatePatient(shimKey);
        List<Resource> containedResources = new ArrayList<Resource>();
        containedResources.add(patient);
        observation.setContained(containedResources);

        //set identifier
        List<Identifier> idList = createSingleIdentifier(identifier);
        observation.setIdentifier(idList);

        //set status
        observation.setStatus(Observation.ObservationStatus.UNKNOWN);

        //set category
        List<CodeableConcept> categories = createCategory(dataType);
        observation.setCategory(categories);

        //set code
        CodeableConcept code = createCodeableConcept(ShimmerUtil.OBSERVATION_CODE_SYSTEM, ShimmerUtil.CODE_CODE_MAP.get(dataType), ShimmerUtil.CODE_DISPLAY_MAP.get(dataType));
        observation.setCode(code);

        //set subject
        Reference subjectRef = createSubjectReference(patient.getId());
        observation.setSubject(subjectRef);

        //set Issued
        observation.setIssued(new Date());          //*LDG Changed from Date(timestamp) since timestamp is not defined

        //set device
        Reference deviceRef = createDeviceReference(deviceInfo);
        observation.setDevice(deviceRef);

        Double value = 0d;
        String units = "";
        String startDateStr = "";
        String endDateStr = "";
        int dotSpot = 0;
        int zero = -1;
        logger.debug("dataType=" + dataType + "=");
        logger.debug("dataType == step_count =" + (dataType.equals("step_count")) + "=");
        Integer DTLength = 0;
        Integer lessThan = -1;

        if (dataType.equals("step_count")) {
            startDateStr = getJsonNodeText(omhStepCountBody.get("effective_time_frame").get("time_interval").get("start_date_time"));
            DTLength = timeWithoutNanoseconds.compareTo(startDateStr.length());
            if (DTLength.equals(lessThan)) {
                dotSpot = startDateStr.indexOf(".");
                if (dotSpot != zero) {
                    startDateStr = startDateStr.substring(0, dotSpot + 4) + "Z";
                }
            }
            logger.debug("startDateStr: [" + startDateStr + "]");

            endDateStr = getJsonNodeText(omhStepCountBody.get("effective_time_frame").get("time_interval").get("end_date_time"));
            DTLength = timeWithoutNanoseconds.compareTo(endDateStr.length());
            if (DTLength.equals(lessThan)) {
                dotSpot = endDateStr.indexOf(".");
                if (dotSpot != zero) {
                    endDateStr = endDateStr.substring(0, dotSpot + 4) + "Z";
                }
            }
            logger.debug("endDateStr: [" + endDateStr + "]");

            //set effective period
            try {
                Period effectivePeriod = createEffectiveDateTime(startDateStr, endDateStr);
                observation.setEffective(effectivePeriod);
            }
            catch(ParseException pe){
                logger.error("Could not parse Shimmer dates");
                pe.printStackTrace();
            }

            value = omhStepCountBody.get(dataType).asDouble();
            units = "steps";
            logger.debug(dataType + ": " + value + " " + units);

            //set steps
            List<Observation.ObservationComponentComponent> componentList = new ArrayList<Observation.ObservationComponentComponent>();
            Observation.ObservationComponentComponent occ = createObservationComponent(value, units, dataType);
            componentList.add(occ);
            observation.setComponent(componentList);
        }

        else {
            startDateStr = getJsonNodeText(omhStepCountBody.get("effective_time_frame").get("date_time"));
            logger.debug("startDateStr: [" + startDateStr + "]");
            DTLength = timeWithoutNanoseconds.compareTo(startDateStr.length());
            if (DTLength.equals(lessThan)) {
                dotSpot = startDateStr.indexOf(".");
                if (dotSpot != zero) {
                    startDateStr = startDateStr.substring(0, dotSpot + 4) + "Z";
                }
            }
            logger.debug("DateStr: [" + startDateStr + "]");
            endDateStr = startDateStr;

            //set effective period
            try {
                Period effectivePeriod = createEffectiveDateTime(startDateStr, endDateStr);
                observation.setEffective(effectivePeriod);
            }
            catch(ParseException pe){
                logger.error("Could not parse Shimmer dates");
                pe.printStackTrace();
            }


            if (dataType.equals("blood_pressure")) {            //*LDG For Blood Pressure, we have to send both systolic and diastolic blood_pressure!!!!!!
                String Sunits = getJsonNodeText(omhStepCountBody.get("systolic_blood_pressure").get("unit"));
                Double Svalue = Double.parseDouble(getJsonNodeText(omhStepCountBody.get("systolic_blood_pressure").get("value")));
                logger.debug("systolic_blood_pressure: " + Svalue + " " + Sunits);
                String Dunits = getJsonNodeText(omhStepCountBody.get("diastolic_blood_pressure").get("unit"));
                Double Dvalue = Double.parseDouble(getJsonNodeText(omhStepCountBody.get("diastolic_blood_pressure").get("value")));
                logger.debug("diastolic_blood_pressure: " + Dvalue + " " + Dunits);

                //set Component
                List<Observation.ObservationComponentComponent> componentList = new ArrayList<Observation.ObservationComponentComponent>();
                Observation.ObservationComponentComponent occ = createObservationComponent(Svalue, Sunits, "systolic_blood_pressure");
                componentList.add(occ);
                occ = createObservationComponent(Dvalue, Dunits, "diastolic_blood_pressure");
                componentList.add(occ);
                observation.setComponent(componentList);
            }
            else {
                units = getJsonNodeText(omhStepCountBody.get(dataType).get("unit"));
                value = Double.parseDouble(getJsonNodeText(omhStepCountBody.get(dataType).get("value")));
                logger.debug(dataType + ": " + value + " " + units);

                //set Component
                List<Observation.ObservationComponentComponent> componentList = new ArrayList<Observation.ObservationComponentComponent>();
                Observation.ObservationComponentComponent occ = createObservationComponent(value, units, dataType);
                componentList.add(occ);
                observation.setComponent(componentList);
            }


        }


        return observation;
    }

    public Patient generatePatient(String shimKey){
        Patient patient = new Patient();
        //set id
        patient.setId(ShimmerUtil.PATIENT_RESOURCE_ID);
        //set identifier
        List<Identifier> idList = createSingleIdentifier(shimKey);
        patient.setIdentifier(idList);
        return patient;
    }

    public Reference createSubjectReference(String refId){
        return createReference(refId, null);
    }
    public Reference createDeviceReference(String display){
        return createReference(null, display);
    }

    public Reference createReference(String refId, String display){
        Reference reference = new Reference();
        if( refId != null ){
            reference.setReference(refId);
        }
        if(display != null){
            reference.setDisplay(display);
        }
        return reference;
    }

    public Identifier createIdentifier(String id){
        Identifier identifier = new Identifier();
        identifier.setSystem(ShimmerUtil.PATIENT_IDENTIFIER_SYSTEM);
        identifier.setValue(id);
        return identifier;
    }

    public List<Identifier> createSingleIdentifier(String id){
        Identifier identifier = createIdentifier(id);
        List<Identifier> idList = new ArrayList<Identifier>();
        idList.add(identifier);
        return idList;
    }

    public List<CodeableConcept> createCategory(String dataType){
        CodeableConcept codeableConcept = createCodeableConcept(ShimmerUtil.OBSERVATION_CATEGORY_SYSTEM, ShimmerUtil.CATEGORY_CODE_MAP.get(dataType), ShimmerUtil.CATEGORY_DISPLAY_MAP.get(dataType));
        List<CodeableConcept> codeableConceptList = new ArrayList<>();
        codeableConceptList.add(codeableConcept);
        return codeableConceptList;
    }

    public CodeableConcept createCodeableConcept(String system, String code, String display){
        return createCodeableConcept(system, code, display, null);
    }
    public CodeableConcept createCodeableConcept(String system, String code, String display, String text){
        CodeableConcept codeableConcept = new CodeableConcept();
        List<Coding> codingList = new ArrayList<>();
        Coding coding = new Coding();
        coding.setSystem(system);
        coding.setCode(code);
        coding.setDisplay(display);
        codingList.add(coding);
        codeableConcept.setCoding(codingList);
        if(text!= null){
            codeableConcept.setText(text);
        }
        return  codeableConcept;
    }

    public Period createEffectiveDateTime(String startDateStr, String endDateStr) throws ParseException{
        Period effectivePeriod = new Period();
        Date startDate = parseDate(startDateStr);
        Date endDate = parseDate(endDateStr);
        effectivePeriod.setStart(startDate);
        effectivePeriod.setEnd(endDate);
        return effectivePeriod;
    }

    public Date parseDate(String dateStr) throws ParseException{
        Date date;
        try{
            date =sdf.parse(dateStr);
        }
        catch(ParseException pe){
            try{
                date =sdfNoMilli.parse(dateStr);
            }
            catch(ParseException pe2){
                try{
                    date =sdfColon.parse(dateStr);
                }
                catch(ParseException pe3){
                        date =sdfNoMilliColon.parse(dateStr);
                }

            }

        }
        return date;
    }

    public Observation.ObservationComponentComponent createObservationComponent(Double value, String units, String dataType){
        Observation.ObservationComponentComponent occ = new Observation.ObservationComponentComponent();
        //set code
        CodeableConcept codeableConcept = createCodeableConcept(ShimmerUtil.COMPONENT_CODE_SYSTEM_MAP.get(dataType), ShimmerUtil.COMPONENT_CODE_CODE_MAP.get(dataType), ShimmerUtil.COMPONENT_CODE_DISPLAY_MAP.get(dataType), ShimmerUtil.COMPONENT_CODE_TEXT_MAP.get(dataType));
        occ.setCode(codeableConcept);

        //set valueQuantity
        Quantity quantity = new Quantity();
        quantity.setValue(value);
        quantity.setUnit(ShimmerUtil.VALUE_CODE_UNIT_MAP.get(dataType).replace("<units>",units));
        quantity.setSystem(ShimmerUtil.OBSERVATION_COMPONENT_VALUE_CODE_SYSTEM);
        quantity.setCode(ShimmerUtil.VALUE_CODE_CODE_MAP.get(dataType).replace("<units>",units));
        occ.setValue(quantity);

        return occ;
    }

    public byte[] makeByteArrayForDocument(String documentId){
        logger.debug("Making Bundle for Document " + documentId);
        //get fitbit data for binary resource
        ShimmerData shimmerData = shimmerDataRepository.findByDocumentId(documentId);
        //get shimmer data
        String jsonData = "";
        if(shimmerData != null) {
            jsonData = shimmerData.getJsonData();
            logger.debug("Got JSON data " + jsonData);
            //Delete the stored user data because it has been retrieved. We do not want to hold on to data longer than needed.
            shimmerDataRepository.delete(shimmerData);
        }
        return jsonData.getBytes();
    }

    public Bundle makeBundleForDocument(String documentId){

        byte[] jsonData = makeByteArrayForDocument(documentId);

        //convert to base64
        byte[] base64EncodedData = Base64.getEncoder().encode(jsonData);

        //make the Binary object
        Binary binary = makeBinary(base64EncodedData);

        //convert to a bundle
        Bundle bundle = makeBundleWithSingleEntry(binary);

        //return the bundle
        return bundle;
    }

    public Binary makeBinary(byte[] base64EncodedData){
        Binary binary = new Binary();
        binary.setContentType("application/json");
        binary.setContent(base64EncodedData);
        return binary;
    }

    public Bundle makeBundleWithSingleEntry(Resource fhirResource){
        List<Resource> fhirResources = new ArrayList<Resource>();
        if( fhirResource != null ) {
            fhirResources.add(fhirResource);
        }
        return makeBundle(fhirResources);
    }
    public Bundle makeBundle(List<Resource> fhirResources){
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(fhirResources.size());
        logger.debug("Now we're creating a bundle of fhirResources of size: " + fhirResources.size());

        for( Resource fhirResource : fhirResources){
            Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent();
            bundleEntryComponent.setResource(fhirResource);
            bundle.addEntry(bundleEntryComponent);
        }

        return bundle;
    }

    public String getShimmerId(String ehrId, String shimkey){
        logger.debug("Checking User EHR ID: [" + ehrId + "] ShimKey: [" + shimkey + "]");
        ApplicationUserId applicationUserId = new ApplicationUserId(ehrId, shimkey);
        //debug info
        logger.debug("Find by ID " + applicationUserRepository.findById(applicationUserId).isPresent());
        ApplicationUser user;
        Optional<ApplicationUser> applicationUserOptional = applicationUserRepository.findById(applicationUserId);
        if(applicationUserOptional.isPresent()){
            logger.debug("Found the user");
            user = applicationUserOptional.get();
        }
        else{
            logger.debug("Did not find user. Creating new one");
            user = createNewApplicationUser(applicationUserId);
        }
        String shimmerId = user.getShimmerId();
        logger.debug("Returning shimmer id: " + shimmerId);
        return shimmerId;
    }

    public ApplicationUser createNewApplicationUser(ApplicationUserId applicationUserId){
        logger.debug("User does not exist, creating");
        String shimmerId = UUID.randomUUID().toString();
        ApplicationUser newUser = new ApplicationUser(applicationUserId, shimmerId);
        applicationUserRepository.save(newUser);
        //force a flush
        applicationUserRepository.flush();
        logger.debug("finished creating user");
        return newUser;
    }


    /**
     * Returns the text contained in a JsonNode object or an empty string if the Node is null
     * @param jsonNode
     * @return
     */
    private String getJsonNodeText(JsonNode jsonNode){
        String nodeText = "";
        if(jsonNode != null){
            nodeText = jsonNode.asText();
        }
        return nodeText;
    }
}
