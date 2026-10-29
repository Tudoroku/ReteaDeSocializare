RETEA DE SOCIALIZARE - LABORATORUL 3
Metode Avansate de Programare | An universitar 2026-2027
======================================================================

1. SCOPUL APLICATIEI
--------------------

Aplicatia extinde proiectul realizat in cadrul laboratorului 2, pastrand
urmatoarele functionalitati:

  - Adaugarea si stergerea utilizatorilor;
  - Adaugarea si stergerea prieteniilor;
  - Determinarea numarului de comunitati;
  - Identificarea celei mai sociabile comunitati.

O comunitate reprezinta o componenta conexa a grafului retelei sociale.
Utilizatorii fara prieteni formeaza comunitati individuale. Cea mai
sociabila comunitate este cea care contine cel mai lung drum simplu,
masurat in numar de muchii.

Obiectivul laboratorului 3 este introducerea persistentei datelor in
fisiere text si pregatirea arhitecturii pentru trecerea la o baza de date
in laboratorul 4, fara modificarea logicii de business sau a interfetei
cu utilizatorul.


2. PORNIREA APLICATIEI SI RULAREA TESTELOR
------------------------------------------

Proiectul se deschide in IntelliJ IDEA, utilizand un JDK compatibil cu
configuratia acestuia.

Aplicatia se porneste prin executarea metodei main din clasa Main.

Directorul de lucru (working directory) trebuie configurat la radacina
proiectului, deoarece fisierele de configurare si de date sunt accesate
prin cai relative.

Testele unitare JUnit sunt organizate in directorul test si pot fi
executate direct din IntelliJ IDEA.

Documentatia Javadoc poate fi consultata prin deschiderea fisierului
index.html din directorul in care a fost generata. Interfetele si
metodele publice sunt documentate in limba engleza, inclusiv contractele
si exceptiile relevante.


3. CONFIGURAREA REPOSITORY-URILOR
---------------------------------

Configuratia aplicatiei se afla in:

  src/main/resources/config.properties

Proprietatea repository.type determina mecanismul de persistenta utilizat:

  MEMORY   - Datele sunt pastrate exclusiv in memorie si se pierd la
             inchiderea aplicatiei.
  FILE     - Datele sunt incarcate din fisiere text si salvate dupa
             fiecare modificare reusita.
  DATABASE - Sunt utilizate clasele schelet ale repository-urilor pentru
             baza de date, pregatite pentru laboratorul 4.

Valorile sunt definite prin enum-ul RepositoryType si sunt sensibile la
litere mari si mici.

Pentru modul FILE se utilizeaza urmatoarele proprietati:

  users.file=data/utilizatori.txt
  friendships.file=data/prietenii.txt

Pentru modul DATABASE sunt necesare:

  db.url
  db.user
  db.password

URL-ul si utilizatorul bazei de date trebuie sa fie definite si sa
contina valori negoale. Proprietatea db.password trebuie sa existe, dar
poate avea o valoare goala.

Configuratia este citita la construirea RepositoryFactory, utilizand
java.util.Properties. Orice modificare a fisierului config.properties
necesita repornirea aplicatiei pentru a fi aplicata.

Proprietatile obligatorii lipsa sau goale si tipurile de repository
necunoscute sunt semnalate prin exceptii cu mesaje explicite. Fabrica nu
verifica daca o conexiune la baza de date poate fi stabilita.


4. REPOSITORY FACTORY SI PREGATIREA LABORATORULUI 4
---------------------------------------------------

Clasa RepositoryFactory implementeaza sablonul de proiectare Simple
Factory si centralizeaza construirea repository-urilor.

Aceasta citeste configuratia si creeaza repository-urile pentru
utilizatori si prietenii, asigurand utilizarea aceluiasi mecanism de
persistenta pentru ambele tipuri de entitati.

Main obtine repository-urile prin intermediul fabricii, fara a instantia
direct implementarile concrete.

Serviciile depind de interfetele UserRepository si FriendshipRepository,
care extind interfata generica Repository. Astfel, logica aplicatiei
ramane independenta de mecanismul de stocare.

Repository-ul utilizatorilor este creat inaintea validatorului
prieteniilor, deoarece validarea unei prietenii presupune verificarea
existentei ambilor utilizatori.

Pentru pregatirea laboratorului 4 au fost introduse clasele:

  - UserDbRepository;
  - FriendshipDbRepository.

Aceste clase primesc prin constructor validatorul si parametrii
conexiunii la baza de date, fara a deschide efectiv o conexiune.

Toate cele cinci operatii definite de interfata Repository arunca
momentan UnsupportedOperationException.

La laboratorul 4 va fi necesara implementarea operatiilor JDBC si
schimbarea proprietatii repository.type in DATABASE.

Arhitectura actuala permite aceasta tranzitie fara modificarea claselor
Main, service sau UI.


5. FORMATUL FISIERELOR SI PERSISTENTA DATELOR
---------------------------------------------

Datele sunt stocate in fisiere text UTF-8, cate o entitate pe linie,
folosind caracterul punct si virgula (;) drept separator.

Fisierul data/utilizatori.txt

  Format:  id;prenume;nume
  Exemplu: 1;Ana;Popescu
           2;Tudor;Ionescu

Fisierul data/prietenii.txt

  Format:  idUtilizator1;idUtilizator2;dataPrieteniei
  Exemplu: 1;2;2026-10-19T10:30

Data prieteniei este reprezentata prin LocalDateTime, in format ISO,
fara informatii despre fusul orar.

Identificatorii prieteniilor sunt normalizati astfel incat ID-ul mai
mic sa fie primul. Prin urmare, prieteniile (1, 2) si (2, 1)
reprezinta aceeasi relatie.

Mecanismul de persistenta

Clasa generica AbstractFileRepository centralizeaza operatiile comune
de incarcare si salvare a datelor.

Subclasele UserFileRepository si FriendshipFileRepository definesc
conversiile specifice si declanseaza incarcarea initiala, reutilizand
mecanismul comun.

In timpul incarcarii se utilizeaza super.save, evitand apelarea metodei
suprascrise care ar declansa rescrierea fisierului in timpul citirii.

Dupa fiecare operatie save, delete sau update finalizata cu succes,
continutul fisierului este rescris integral.

Accesul la fisiere se realizeaza prin API-ul NIO.2, utilizand Path,
Files.newBufferedReader si Files.newBufferedWriter.

Resursele sunt gestionate prin try-with-resources, asigurand inchiderea
lor corespunzatoare.


6. VALIDARE SI TRATAREA EXCEPTIILOR
-----------------------------------

Validarea datelor este realizata prin componente dedicate.

UserValidator verifica urmatoarele conditii:

  - Entitatea nu este null;
  - Identificatorul este pozitiv;
  - Prenumele si numele nu sunt null sau formate exclusiv din spatii;
  - Numele nu contin separatorul utilizat in fisiere sau caractere de
    sfarsit de linie.

FriendshipId verifica validitatea perechii de identificatori si
normalizeaza ordinea acestora.

FriendshipValidator verifica existenta ambilor utilizatori implicati
in prietenie.

Validarea se aplica atat la incarcarea datelor din fisiere, cat si
inaintea operatiilor de salvare si actualizare din repository-urile FILE.

UserService valideaza utilizatorii inainte de inserare, in timp ce
repository-urile MEMORY nu aplica automat validatorii de domeniu.

Aceasta abordare protejeaza datele provenite din surse externe si
operatiile aplicatiei. In cazul inserarii unui utilizator prin
UserService si un repository FILE, validarea se executa de doua ori,
pentru a mentine verificarea si la nivelul persistentei.

Gestionarea exceptiilor

Aplicatia utilizeaza urmatoarele categorii de exceptii:

  ValidationException        - Semnaleaza incalcarea regulilor de validare.
  IllegalArgumentException   - Semnaleaza, printre altele, identificatori
                               duplicati, entitati inexistente sau
                               configuratii invalide.
  RepositoryException        - Semnaleaza erori de persistenta si pastreaza
                               cauza originala prin exception chaining.
  UnsupportedOperationException
                             - Semnaleaza operatiile neimplementate in
                               repository-urile DATABASE.

Exceptiile cunoscute sunt tratate la nivelul UI, unde utilizatorului ii
este afisat un mesaj relevant, iar aplicatia poate continua executia.

Erorile aparute la initializare pot fi propagate inainte de activarea
mecanismului de tratare din meniul aplicatiei.


7. GESTIONAREA FISIERELOR LIPSA SI A DATELOR CORUPTE
----------------------------------------------------

Aplicatia implementeaza o politica de incarcare toleranta la
inregistrari invalide.

Fisier inexistent

Daca fisierul configurat nu exista, repository-ul este initializat gol.
Fisierul este creat la prima operatie de salvare reusita, impreuna cu
directoarele necesare.

Linii invalide sau corupte

La incarcarea datelor:

  - Liniile goale sunt ignorate;
  - Liniile cu format incorect sunt raportate;
  - Datele calendaristice invalide sunt respinse;
  - Identificatorii duplicati sunt respinsi;
  - Inregistrarile invalide sunt ignorate, iar incarcarea continua.

Pentru fiecare linie respinsa se afiseaza in fluxul standard de eroare
(stderr) numarul liniei si motivul respingerii.

Justificarea alegerii

Aceasta politica permite utilizarea inregistrarilor valide chiar daca
fisierul contine date corupte.

Oprirea intregii aplicatii din cauza unei singure inregistrari invalide
ar reduce disponibilitatea datelor care pot fi recuperate.

Liniile ignorate nu sunt corectate automat. La urmatoarea rescriere a
fisierului sunt persistate doar entitatile incarcate si acceptate.


8. CONSISTENTA DATELOR SI LIMITARI
----------------------------------

Pentru mentinerea integritatii referentiale, stergerea unui utilizator
presupune eliminarea prealabila a tuturor prieteniilor asociate acestuia.

Astfel, aplicatia evita pastrarea unor prietenii care refera utilizatori
inexistenti dupa finalizarea cu succes a operatiei.

In cazul unei erori de scriere semnalate prin RepositoryException,
repository-ul restaureaza starea anterioara din memorie.

Totusi, aceasta restaurare nu reprezinta o tranzactie completa asupra
fisierelor.

Exista doua limitari importante:

  - O scriere intrerupta poate lasa fisierul trunchiat sau partial
    modificat, fara restaurarea automata a continutului anterior.
  - Stergerea unui utilizator si a prieteniilor sale presupune operatii
    succesive asupra a doua repository-uri, fara o tranzactie comuna.

Prin urmare, consistenta este asigurata pentru operatiile finalizate cu
succes, dar nu este garantata atomicitatea modificarilor in cazul
erorilor de persistenta.

Aceste limitari pot fi abordate ulterior prin utilizarea tranzactiilor
oferite de o baza de date relationala.


9. SABLOANE SI PRINCIPII DE PROIECTARE
--------------------------------------

9.1. Sabloane de proiectare

Factory (Simple Factory)
  RepositoryFactory centralizeaza selectarea si construirea
  implementarilor concrete, in functie de configuratia aplicatiei.

Template Method
  AbstractFileRepository defineste algoritmul comun de incarcare si
  persistenta a entitatilor. Subclasele furnizeaza implementarile
  specifice pentru conversia dintre entitati si reprezentarea lor text,
  eliminand duplicarea codului de acces la fisiere.

Strategy
  Interfata Validator defineste un contract comun pentru politicile de
  validare. UserValidator si FriendshipValidator reprezinta implementari
  concrete, care pot fi furnizate repository-urilor prin constructor.

Decorator
  BufferedReader si BufferedWriter adauga functionalitatea de buffering
  peste mecanismele de citire si scriere a caracterelor. In implementarea
  actuala, aceste componente sunt obtinute prin metodele API-ului NIO.2.

9.2. Principii SOLID

SRP - Single Responsibility Principle
  Responsabilitatile sunt separate intre componente:
    - Entitatile modeleaza datele;
    - Validatorii verifica regulile de domeniu;
    - Repository-urile gestioneaza persistenta;
    - Serviciile coordoneaza operatiile aplicatiei;
    - UI-ul gestioneaza interactiunea cu utilizatorul.

DIP - Dependency Inversion Principle
  Serviciile depind de abstractiile UserRepository si FriendshipRepository,
  nu de implementarile concrete. Construirea dependentelor este delegata
  catre RepositoryFactory.

OCP - Open/Closed Principle
  Noile implementari de repository pot fi introduse fara modificarea
  serviciilor existente. Totusi, adaugarea unui nou mecanism de persistenta
  necesita extinderea enum-ului RepositoryType si modificarea
  instructiunilor switch din Simple Factory. Prin urmare, respectarea
  OCP este partiala la nivelul mecanismului de construire.

LSP - Liskov Substitution Principle
  Implementarile functionale ale interfetei Repository trebuie sa
  respecte contractele definite de aceasta, inclusiv comportamentul in
  cazul argumentelor invalide. Clasele DATABASE reprezinta momentan o
  limitare intentionata: operatiile lor arunca
  UnsupportedOperationException, motiv pentru care nu pot substitui
  functional implementarile MEMORY si FILE. Aceasta limitare va fi
  eliminata prin implementarea operatiilor JDBC in laboratorul 4.

9.3. Simple Factory vs. Abstract Factory

Simple Factory a fost ales datorita structurii reduse si a numarului
limitat de mecanisme de persistenta.

Avantajele acestei abordari sunt simplitatea, lizibilitatea si
centralizarea configurarii.

Principalul dezavantaj este necesitatea modificarii fabricii la
introducerea unui nou tip de repository, ceea ce limiteaza respectarea
principiului OCP.

O alternativa ar fi Abstract Factory, prin definirea unei interfete
comune si a cate unei fabrici concrete pentru fiecare mecanism de
persistenta.

Aceasta solutie separa constructia familiilor de repository-uri si poate
reduce modificarile in fabricile existente. Totusi, adaugarea unei
fabrici concrete poate necesita in continuare actualizarea selectiei
sau configurarii centrale. In plus, ar introduce interfete si clase
suplimentare.

Pentru dimensiunea si cerintele actuale ale proiectului, Simple Factory
reprezinta o alegere suficienta si usor de intretinut.


10. DEMONSTRATIE SI PREDARE
---------------------------

La demonstratie se verifica urmatoarele scenarii:

  - Adaugarea si stergerea utilizatorilor si prieteniilor in modul FILE;
  - Inchiderea si repornirea aplicatiei, cu pastrarea datelor;
  - Stergerea automata a prieteniilor asociate unui utilizator eliminat;
  - Pornirea aplicatiei in absenta fisierelor de date;
  - Raportarea si ignorarea liniilor corupte introduse manual;
  - Functionarea modului MEMORY fara incarcarea sau modificarea fisierelor;
  - Selectarea modului DATABASE si semnalarea operatiilor neimplementate.

Livrabilele proiectului includ:

  - Codul sursa al aplicatiei;
  - Fisierul config.properties;
  - Directorul data/ si fisierele aferente;
  - Un fisier demonstrativ care contine inregistrari corupte;
  - Documentatia Javadoc;
  - Prezentul fisier readme.txt.