package abc.service2.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.domain.Persistable;

@Entity
@DynamicUpdate
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Book implements Persistable<String> {

    @Id
    public String isbn;
    private String title;
    private String author;
    private String genre;
    private String person;
    private long version;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public String getId() {
        return isbn;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

}
