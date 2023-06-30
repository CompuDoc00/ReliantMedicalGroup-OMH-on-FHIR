package org.gtri.hdap.mdata.jackson;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
//import com.fasterxml.jackson.core.json.UTF8JsonGenerator;
//import org.apache.catalina.connector.CoyoteOutputStream;
//import org.apache.catalina.connector.OutputBuffer;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Created by es130 on 7/20/2018.
 */
public class HapiStu3Serializer extends StdSerializer<IBaseResource> {

    Logger logger = LoggerFactory.getLogger(HapiStu3Serializer.class);

    public HapiStu3Serializer(Class<IBaseResource> t) {
        super(t);
    }

    @Override
    public void serialize(IBaseResource iBaseResource, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        IParser jsonParser = FhirContext.forDstu3().newJsonParser();
        String json = jsonParser.encodeResourceToString(iBaseResource);
        logger.debug("Parsed HAPI DSTU3 Object");
        logger.debug(json);
        int length=json.length();
        String hex=Integer.toHexString(length);
        logger.debug("Bundle length is {} characters which is {} in hexadecimal",length,hex);
        //json = hex + "\r\n" + json + "\r\n" + "0" + "\r\n\r\n";
        //logger.debug("And now here is the Chunked Transfer Encoded bundle:");
        //logger.debug(json);
        jsonGenerator.writeRaw(json);
        //((UTF8JsonGenerator) jsonGenerator)._outputStream();          //*LDG This sends the last chunk, presumably with CRLF (i.e. \r\n) afterwards
        //JsonGenerator gen = jsonGenerator; //*LDG We are supposed to send a chunk with with 0 length, so let's try flush
        //char[] spot = ((UTF8JsonGenerator) jsonGenerator)._charBuffer;
        //CoyoteOutputStream. (gen, 0, 0)

    }
}
