LABORATOR 5 - MAP
Persistenta in PostgreSQL folosind JDBC

1. SCOP

Am extins aplicatia de retea sociala din laboratoarele anterioare prin implementarea persistentei intr-o baza de date PostgreSQL.

Functionalitatile existente au ramas neschimbate: gestionarea utilizatorilor si prieteniilor, determinarea numarului de comunitati si identificarea celei mai sociabile comunitati.


2. STRUCTURA BAZEI DE DATE

Baza de date se numeste socialnetwork si contine doua tabele:

users:
- id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
- first_name VARCHAR NOT NULL
- last_name VARCHAR NOT NULL

friendships:
- id1 BIGINT
- id2 BIGINT
- friends_from TIMESTAMP NOT NULL
- PRIMARY KEY (id1, id2)
- CHECK (id1 < id2)
- FOREIGN KEY (id1) REFERENCES users(id) ON DELETE CASCADE
- FOREIGN KEY (id2) REFERENCES users(id) ON DELETE CASCADE

Am folosit ON DELETE CASCADE pentru a elimina automat prieteniile asociate unui utilizator atunci cand acesta este sters.

ID-urile unei prietenii sunt normalizate in ordine crescatoare prin FriendshipId, astfel incat (1, 2) si (2, 1) reprezinta aceeasi relatie.


3. IMPLEMENTAREA JDBC

Am implementat clasele UserDbRepository si FriendshipDbRepository, care respecta interfetele Repository existente.

Conexiunea la baza de date se realizeaza prin DriverManager.getConnection(), folosind parametrii din configuratie.

Pentru operatiile CRUD am folosit PreparedStatement si try-with-resources, astfel incat resursele JDBC sunt inchise automat.

Interogarile folosesc parametri in locul concatenarii valorilor, pentru a preveni SQL injection.

La adaugarea unui utilizator, PostgreSQL genereaza automat ID-ul. Acesta este obtinut prin INSERT ... RETURNING id, atribuit obiectului User si afisat in consola.

Contractul Repository:
- findOne(id) - Optional cu entitatea gasita sau empty.
- findAll() - colectia entitatilor.
- save(entity) - salveaza entitatea.
- update(entity) - Optional.empty() la succes, entitatea daca nu exista.
- delete(id) - Optional cu entitatea stearsa sau empty daca nu exista.

Validarea se realizeaza inainte de save si update.


4. CONFIGURARE

RepositoryFactory permite selectarea modului de persistenta din config.properties, prin proprietatea repository.type.

Modurile disponibile:
- MEMORY - stocare temporara in memorie.
- FILE - persistenta in fisiere text.
- DATABASE - persistenta in PostgreSQL.

Pentru DATABASE sunt necesare urmatoarele proprietati:

repository.type=DATABASE
db.url=jdbc:postgresql://localhost:5432/socialnetwork
db.user=postgres
db.password=PAROLA_LOCALA

Driverul PostgreSQL este inclus in Gradle:
org.postgresql:postgresql:42.7.7

Fisierul config.example.properties contine un exemplu de configurare. Fisierul config.properties este exclus din Git pentru a nu publica datele de autentificare.


5. VALIDARE SI TRATAREA ERORILOR

UserValidator verifica ID-ul si numele utilizatorilor.

Pentru FILE am adaugat o validare separata, care respinge caracterul ';' si liniile noi, deoarece acestea ar afecta formatul fisierelor.

In DATABASE, aceste caractere sunt acceptate. Valorile sunt transmise prin PreparedStatement si sunt interpretate ca date, nu ca instructiuni SQL.

Exceptiile SQLException sunt incapsulate in RepositoryException, pastrand cauza originala.

Mesajele sunt diferentiate dupa SQLState:
- 08 - eroare de conexiune.
- 23 - incalcarea unei constrangeri.
- Alte coduri - eroare de baza de date.

ConsoleUI afiseaza mesajele de eroare fara stack trace.


6. RULARE SI TESTARE

Proiectul foloseste Java 27, Gradle si JUnit 5.

Pornirea aplicatiei:
.\gradlew.bat run --console=plain

Rularea testelor:
.\gradlew.bat clean test --console=plain

Testele de integrare JDBC folosesc o baza separata, socialnetwork_test, configurata prin variabilele de mediu:
- SOCIALNETWORK_TEST_DB_URL
- SOCIALNETWORK_TEST_DB_USER
- SOCIALNETWORK_TEST_DB_PASSWORD

Au fost executate cu succes 21 de teste de integrare PostgreSQL, fara erori sau teste ignorate.

Codul sursa si metodele relevante sunt documentate prin Javadoc.