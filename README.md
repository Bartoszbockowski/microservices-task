### Pozwoliłem sobie utworzyć serwisy w architekrurze monorepo dla prostszej obsługi.


### Stworzenie plków JAR (wymagany uruchomiony docker dla kontenerów testowych)
```
mvn clean install
```

## Serwis 1
#### Lista książek
```http
  GET http://localhost:8080/api/v1/book
```
#### Dodanie książki
```http
  POST http://localhost:8080/api/v1/book
```
#### Wypożyczenie książki
```http
  PUT http://localhost:8080/api/v1/book?clientName=Jan&isbn=12345678910
```
## Serwis 2
#### Lista wypożyczonych książek
```http
  GET http://localhost:8081/api/v1/book
```