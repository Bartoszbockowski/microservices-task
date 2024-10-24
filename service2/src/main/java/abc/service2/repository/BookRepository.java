package abc.service2.repository;

import abc.service2.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookRepository extends JpaRepository<Book, String> {

    List<Book> findAllByPersonNotNull();
}
