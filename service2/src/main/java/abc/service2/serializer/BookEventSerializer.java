package abc.service2.serializer;

import abc.service2.model.event.BookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class BookEventSerializer implements Serializer<BookEvent> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, BookEvent bookDto) {
        try {
            if (bookDto == null) {
                return null;
            }
            return objectMapper.writeValueAsBytes(bookDto);
        } catch (Exception e) {
            throw new SerializationException("Error serializing BookEvent", e);
        }
    }

    @Override
    public void close() {
    }
}
