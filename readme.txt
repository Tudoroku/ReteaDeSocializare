SCOPUL APLICATIEI

Laboratorul 4 refactorizeaza implementarea din Lab 3 folosind interfete
functionale, expresii lambda, referinte la metode si Stream API.
Aplicatia pastreaza functionalitatile si formatul fisierelor de date:

Adaugarea si stergerea utilizatorilor

Adaugarea si stergerea prieteniilor

Determinarea numarului de comunitati

Identificarea celei mai sociabile comunitati

O comunitate este o componenta conexa a grafului retelei sociale.
Un utilizator fara prieteni formeaza o comunitate individuala.
Cea mai sociabila comunitate este cea care contine cel mai lung
drum simplu, masurat in numar de muchii.

PORNIREA APLICATIEI SI RULAREA TESTELOR

Deschide proiectul in IntelliJ IDEA si ruleaza metoda main din clasa
Main. Proiectul a fost verificat cu JDK 27.

Configureaza directorul de lucru la radacina proiectului. Aplicatia
foloseste cai relative pentru configurare si fisierele de date.

Testele JUnit din directorul test pot fi rulate direct din IntelliJ.
Pe versiunea verificata a laboratorului au trecut toate cele 68 de teste.
Metodele si interfetele noi sunt documentate prin Javadoc in engleza.

CONFIGURAREA REPOSITORY-URILOR

Configuratia se afla in src/main/resources/config.properties.
Proprietatea repository.type accepta urmatoarele valori:

MEMORY   - Datele sunt pastrate in memorie si se pierd la inchidere.
FILE     - Datele sunt incarcate si salvate in fisiere text.
DATABASE - Sunt create clasele schelet pentru persistenta in baza de date.

Pentru FILE se folosesc proprietatile:

users.file=data/utilizatori.txt
friendships.file=data/prietenii.txt

Pentru DATABASE, proprietatile db.url si db.user trebuie sa fie
prezente si necompletate cu valori goale. Proprietatea db.password
trebuie sa existe, dar poate avea valoare goala.

In Lab 4, UserDbRepository si FriendshipDbRepository raman clase schelet.
Operatiile lor arunca UnsupportedOperationException. Persistenta JDBC
va fi implementata in Lab 5.

RepositoryFactory citeste configuratia la initializare. Pentru a aplica
modificarile din config.properties, reporneste aplicatia.

REPOSITORY FACTORY

RepositoryFactory centralizeaza alegerea implementarilor pentru
UserRepository si FriendshipRepository. Pentru fiecare dintre cele doua
interfete, fabrica asociaza fiecarui RepositoryType un Supplier care
creeaza implementarea corespunzatoare.

Fabrica selecteaza supplier-ul prin factories.get(type) si il apeleaza
prin get(). Astfel, doar repository-ul selectat este construit si doar
setarile specifice acelui mod sunt verificate. De exemplu, modul MEMORY
poate fi folosit fara proprietatile necesare pentru FILE sau DATABASE.

Serviciile lucreaza cu interfetele repository-urilor. Adaugarea unui nou
tip de persistenta ar presupune extinderea enum-ului RepositoryType si
actualizarea maparilor din fabrica.

FORMATUL SI PERSISTENTA FISIERELOR

Datele sunt stocate in fisiere text UTF-8, cate o entitate pe linie.
Campurile sunt separate prin punct si virgula.

data/utilizatori.txt
Format:  id;prenume;nume
Exemplu: 1;Ana;Popescu

data/prietenii.txt
Format:  idUtilizator1;idUtilizator2;dataPrieteniei
Exemplu: 1;2;2026-10-19T10:30

Data prieteniei este un LocalDateTime in format ISO. ID-urile unei
prietenii sunt normalizate in ordine crescatoare, astfel incat perechile
(1, 2) si (2, 1) reprezinta aceeasi relatie.

AbstractFileRepository citeste datele cu Files.lines, intr-un bloc
try-with-resources. Liniile sunt procesate in ordinea lor din fisier.
Contorul include si liniile goale, astfel incat mesajele de eroare sa
indice numarul real al liniei. Liniile goale sunt ignorate, iar
inregistrarile invalide sunt raportate la stderr si sarite.

Daca fisierul lipseste, repository-ul porneste gol. La prima salvare,
directoarele necesare si fisierul sunt create. La scriere, entitatile
sunt transformate in linii prin map si colectate intr-o lista.
Files.write rescrie fisierul in UTF-8.

Am pastrat Template Method pentru repository-urile pe fisier.
AbstractFileRepository contine pasii comuni de incarcare si salvare.
UserFileRepository si FriendshipFileRepository definesc conversia
dintre o linie de text si entitatea lor. Astfel, citirea si scrierea
sunt implementate intr-un singur loc, iar formatele din Lab 3 sunt
pastrate.

VALIDARE, OPTIONAL SI TRATAREA ERORILOR

UserValidator verifica ID-ul si numele utilizatorului.
FriendshipValidator verifica existenta ambilor utilizatori dintr-o
prietenie. Repository-urile FILE aplica validarea la incarcare,
salvare si actualizare.

Am pastrat Optional<E> ca tip de retur pentru Repository.findOne,
deoarece acesta exprima explicit cazul in care entitatea lipseste.
In AbstractFileRepository, delete si update folosesc orElseThrow
pentru a obtine entitatea originala inainte de modificare.

Erorile de citire si scriere sunt semnalate prin RepositoryException,
cu pastrarea cauzei initiale. Daca scrierea esueaza, repository-ul
restaureaza starea anterioara in memorie. Fisierul poate ramane partial
scris, deoarece rescrierea nu este atomica.

Stergerea unui utilizator elimina mai intai prieteniile asociate.
Aceste operatii sunt executate succesiv si nu formeaza o tranzactie
comuna. Daca o stergere esueaza, operatiile deja finalizate nu sunt
anulate automat.

INTERFETE FUNCTIONALE FOLOSITE

Validator<T> verifica o entitate si poate arunca ValidationException.
In demonstratia din Main, andThen compune UserValidator cu o regula
lambda care limiteaza prenumele la 50 de caractere. Validatorul
existent ruleaza primul, apoi se aplica regula suplimentara.

Predicate<E> decide daca o entitate indeplineste o conditie.
Repository.findAll(Predicate<E>) aplica predicatul prin filter.
UserCriteria.hasFirstName si hasLastName creeaza criterii care sunt
combinate in demonstratie cu and, or, negate si Predicate.not.

Supplier<T> produce o valoare fara argumente. RepositoryFactory
foloseste supplier-i pentru a amana construirea repository-ului
pana la selectarea tipului configurat.

Function<T, R> transforma o valoare in alta. In buildGraph,
map(User::getId) extrage ID-ul fiecarui utilizator. In writeToFile,
map(this::createEntityAsString) transforma entitatea intr-o linie.

Consumer<T> executa o actiune fara rezultat. In deleteUser,
forEach(friendshipRepo::delete) sterge prieteniile selectate.
In Main, forEach(System.out::println) afiseaza rezultatele.

Cele patru forme de referinte la metode sunt folosite in cod:

Metoda statica: Main::formatUser

Metoda de instanta pe un obiect anume: System.out::println

Metoda de instanta pe obiectele din stream: User::getLastName

Constructor: InMemoryUserRepository::new

In demonstratie, rezultatele sunt sortate dupa numele de familie cu
Comparator.comparing, apoi dupa prenume cu thenComparing.

BUCLE INLOCUITE CU STREAM API

Clasa / metoda

Inainte (Lab 3)

Dupa (Lab 4)

UserService.deleteUser

Bucla peste toate prieteniile

findAll(Predicate), map, forEach

CommunityService.buildGraph

Bucla pentru noduri

map, distinct, collect(toMap)

CommunityService.buildGraph

Bucla pentru muchii

map si forEach

CommunityService.dfs

Bucla peste vecinii nodului curent

filter si forEach

CommunityService.getCommunities

Bucla peste nodurile grafului

collect cu acumulator pentru componente

CommunityService.getMostSociableCommunity

Doua bucle pentru scor si selectie

mapToInt si max pentru scor, apoi map si reduce pentru selectie

AbstractFileRepository.loadFromFile

while cu BufferedReader.readLine

Files.lines si forEachOrdered

AbstractFileRepository.writeToFile

Bucla cu BufferedWriter

map, collect(toList) si Files.write

BUCLE PASTRATE

CommunityService.dfs pastreaza bucla while care controleaza stiva DFS.
La fiecare pas, algoritmul scoate un nod din stiva si adauga vecinii
nevizitati. Filtrarea vecinilor foloseste Stream API.

CommunityService.longestPathDFS pastreaza bucla peste vecini pentru
backtracking. Fiecare apel recursiv marcheaza nodul curent la intrare
si il demarcheaza la iesire. Bucla face explicita explorarea succesiva
a ramurilor si ordinea in care se modifica setul de noduri vizitate.

ConsoleUI pastreaza bucla meniului pentru a citi si executa comenzi pana
la iesire. Bucla controleaza interactiunea cu utilizatorul.

DEMONSTRATIE SI PREDARE

Pentru demonstratia Lab 4, ruleaza Main cu argumentul --demo. In IntelliJ,
argumentul se introduce in Run > Edit Configurations > Program arguments.
Demonstrarea foloseste date in memorie. Ea arata acceptarea unui
utilizator valid, respingerea unui prenume de 51 de caractere si
rezultatele filtrarii prin and, or, negate si Predicate.not.

Ruleaza Main fara argumente pentru meniul obisnuit. Cu fisierele de date
incluse in proiect si modul FILE, aplicatia afiseaza 2 comunitati.
Cea mai sociabila comunitate contine utilizatorii cu ID-urile 1, 2 si 3.

Exemple inainte/dupa pentru prezentare:

UserService.deleteUser
findAll(Predicate) selecteaza prieteniile relevante. map extrage
ID-urile, iar forEach este operatia terminala care le sterge.

CommunityService.buildGraph
map si distinct obtin ID-urile unice. collect(toMap) este operatia
terminala care construieste harta nodurilor cu seturi de vecini.

AbstractFileRepository.writeToFile
map transforma entitatile in linii. collect(toList) este operatia
terminala care construieste lista trimisa apoi catre Files.write.

Proiectul include codul sursa, testele, config.properties si fisierele
din data/, inclusiv data/utilizatori_corupti.txt pentru verificarea
incarcarii in prezenta liniilor invalide.