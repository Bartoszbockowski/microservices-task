package pl.abc.service1.listenter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import pl.abc.service1.model.dto.BookDto;

import java.util.Map;

public class BookEventDeserializer implements Deserializer<BookDto> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public BookDto deserialize(String topic, byte[] data) {
        try {
            if (data == null) {
                return null;
            }
            return objectMapper.readValue(data, BookDto.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing BookEvent", e);
        }
    }

    @Override
    public void close() {
    }
}
