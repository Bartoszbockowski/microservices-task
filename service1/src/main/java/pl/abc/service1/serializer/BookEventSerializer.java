package pl.abc.service1.serializer;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import pl.abc.service1.model.event.BookEvent;

import java.util.Map;

public class BookEventSerializer implements Serializer<BookEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, BookEvent bookEvent) {
        try {
            if (bookEvent == null) {
                return null;
            }
            return objectMapper.writeValueAsBytes(bookEvent);
        } catch (Exception e) {
            throw new SerializationException("Error serializing BookEvent", e);
        }
    }

    @Override
    public void close() {
    }
}
