package abc.service2.deserializer;

import abc.service2.model.event.BookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class BookEventDeserializer implements Deserializer<BookEvent> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public BookEvent deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(data, BookEvent.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing BookEvent", e);
        }
    }

    @Override
    public void close() {
    }
}
