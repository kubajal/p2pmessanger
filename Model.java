import java.net.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.beans.PropertyChangeSupport;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Komponent Model wzorca MVC aplikacji.
 * Czesc aplikacja odpowiadajaca za nieskopoziomowe dzialanie aplikacji.
 * Obsluga polaczen internetowych, wysylanie i odbieranie wiadomosci, kolejkowanie odebranych
 * i wysylanych wiadomosci, nasluch polaczen.
 * @author Kuba Jalowiec
 */
public class Model{

	/** Obsluga istniejacego polaczenia.  */
	private Connection connection;
	
	/** Nasluch. */
	private ServerSocket listener;
	
	/** Kolejka wiadomosci do wyslania.  */
	private BlockingQueue<String> sendQueue;
	
	/** Kolejka wiadomosci do wyswietlenia. */
	private BlockingQueue<String> toViewQueue;
	
	/** Obsluga zdarzen. Wykorzystywane w Controller.  */
	public PropertyChangeSupport changeSupport;
	
	/** Log aplikacji - sluzy do komunikowania uzytkownikowi zmian stanu polaczenia, bledow itp. */
	private String log;
	
	/** Klasa do obslugi nawiazanego polaczenia. Watki wysylania i odbierania wiadomosci,
	 *  zamykanie polaczenia itp. */
	class Connection{

		private Socket socket;
		private Thread outputThread, inputThread;
		private Boolean closeConnectionFlag, outputRunFlag;
		private PrintWriter socketOutput;
		private BufferedReader socketInput;
		private String nazwa = "przychodzace";

		/** Konstruktor na podstawie istniejacego socket'a, powstalego w funkcji listen() lub newConnection() */
		Connection(Socket _s){
			
			socket = _s;
			try {
				socketInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				socketOutput = new PrintWriter(socket.getOutputStream(), true);
				closeConnectionFlag = false;
				outputRunFlag = true;
				outputThread = newOutputThread();
				outputThread.start();
				inputThread = newInputThread();
				inputThread.start();
			}
			catch(Exception e){
				updateLog("Nie powiodlo sie zapisanie polaczenia.");
				socketInput = null;
				socketOutput = null;
				closeConnectionFlag = false;
				outputRunFlag = false;
			}
		}
		
		/** Tworzy i zwraca watek odpowiedzialny za wysylanie wiadomosci. Watek pobiera wiadomosci z kolejki sendQueue
		 * i je wysyla.
		 * @return Watek wyjsciowy.
		 */
		private Thread newOutputThread(){
			
			return new Thread(
				new Runnable(){
					public synchronized void run(){
						while(outputRunFlag == true){
							
							String msg = null;
							try {
								msg = sendQueue.poll(100, TimeUnit.MILLISECONDS);
							} catch (InterruptedException e) {
								updateLog("Przerywam wysylanie wiadomosci.");
							}
							if(msg != null)
							socketOutput.println(msg);
						}
						updateLog("Zamknieto wyjscie.");
					}
				}
			);
		}

		/** Tworzy i zwraca watek odpowiedzialny za odbieranie wiadomosci. Watek nasluchuje na socket'cie i wstawia odebrane
		 * wiadomosci do kolejki toViewQueue.
		 * @return Watek wejsciowy.
		 */
		private Thread newInputThread(){
		
			return new Thread(
				
				new Runnable(){
					public synchronized void run(){
						String msg = null;
							try {
								while((msg = socketInput.readLine()) != null){
									String timeStamp = new SimpleDateFormat("HH.mm.ss").format(Calendar.getInstance().getTime());
									try {
										toViewQueue.put(timeStamp + ", " + nazwa  + ": " + msg + "\n");
									} catch (InterruptedException e) {
										updateLog("Blad podczas przesylania wiadomosci do widoku.");
									}
								}
							} catch (IOException e) {
								//nie rob nic - ewentualny wyjatek lub ewentualne pojawienie sie nulla jest obsluzone w finally
							}
							finally{
								if(closeConnectionFlag == false){
									updateLog("Rozlaczono po drugiej stronie...");
									close();
								}
							}
							updateLog("Zamknieto wejscie.");
					}
				}
			);
		}

		/** Zamyka polaczenie, tzn. konczy watki obslugi wejscia oraz wyjscia,
		 *  zamyka socket i czysci po connection.
		 */
		private synchronized void close(){
			new Thread(
				new Runnable(){
					public synchronized void run(){
						if(connection != null){
							closeConnectionFlag = true;
							try {
								socket.close();
							} catch (IOException e) {
								updateLog("Blad podczas zamykania socket'a.");
							}
							try {
								inputThread.join();
							} catch (InterruptedException e) {
								updateLog("Blad podczas oczekiwania na zamkniecie watku wejsciowego.");
							}
							outputRunFlag = false;
							try {
								outputThread.join();
							} catch (InterruptedException e) {
								updateLog("Blad podczas oczekiwania na zamkniecie watku wyjsciowego.");
							}
							updateLog("Zamknieto polaczenie.");
							Connection old = connection;
							connection = null;
							changeSupport.firePropertyChange("connection", old, connection);
							closeConnectionFlag = false;
						}
					}
				}
			).start();
		}
	}

	/** Tworzy czysty model bez zadnych polaczen oraz bez nasluchiwania.
	 */
	Model(){

		listener = null;
		connection = null;
		sendQueue = new LinkedBlockingQueue<String>();
		toViewQueue = new LinkedBlockingQueue<String>();
		changeSupport = new PropertyChangeSupport(this);
		log = new String();
	}

	/** Wyswietla w logu nowa informacje.
	 * @param _s Komunikat do dolaczenia do logu.
	 */
	public synchronized void updateLog(String _s){
		String old = log;
		log = log + _s + "\n";
		changeSupport.firePropertyChange("log", old, log);
	}

	/** Rozpoczyna nasluchiwanie na porcie 5000.
	 */
	void listen() throws IOException{
		
		new Thread(new Runnable(){
			public synchronized void run(){
				Socket tmp = null;
				try {
					listener = new ServerSocket(5000);
					updateLog("Nasluchuje na porcie " + listener.getLocalPort());
					tmp = listener.accept();
					updateLog("Polaczono do " + tmp.getInetAddress());
				} catch (IOException e) {
					updateLog("Blad podczas odbierania polaczenia.");
					stopListening();
					return;
				}
				stopListening();
				Connection old = connection;
				connection = new Connection(tmp);
				changeSupport.firePropertyChange("connection", old, connection);
			}
		}).start();
	}

	/** Konczy nasluchiwanie na porcie 5000.
	 */
	void stopListening(){
		
		updateLog("Przestaje nasluchiwac.");
		if(listener != null)
			try {
				listener.close();
				
			} catch (IOException e) {

				updateLog("Blad podczas zamykania ServerSocket.");
			};
	}

	/** Laczy z podanym adresem IP.
	 * @throws IOException 
	 */
	void newConnection(InetAddress IP) throws IOException{

		Connection old = connection;
		try {
			connection = new Connection(new Socket(IP, 5000));
		} catch (IOException e) {
			connection = null;
			updateLog("Blad podczas nawiazywania polaczenia.");
			throw e;
		}
		changeSupport.firePropertyChange("connection", old, connection);
		updateLog("Polaczono do " + IP);
	}

	/** Wysyla przez socket wiadomosc tekstowa.
	 * @param msg Wiadomosc do wyslania.
	 */
	public synchronized void sendMessage(String msg){
		
		String timeStamp = new SimpleDateFormat("HH.mm.ss").format(Calendar.getInstance().getTime());
		try {
			sendQueue.put(msg);
			toViewQueue.put(timeStamp + ", ja: " + msg + "\n");
		} catch (InterruptedException e) {
			updateLog("Blad podczas wysylania wiadomosci.");
		}
	}

	/** Zamyka polaczenie.
	 */
	public void closeConnection(){

		if(connection != null){
			connection.close();
		}
	}

	/** Zwraca referencje na log.
	 * @return Referencja na log.
	 */
	public String getConnectionLog(){
		return log;
	}

	/** Zwraca wiadomosc odebrana przez socket. Jesli kolejka jest pusta
	 * to metoda ta blokuje sie do czasu pojawienia sie nowej wiadomosci.
	 * @return Wiadomosc odebrana z zewnatrz.
	 * @throws InterruptedException Jesli pojawi sie interrupt() na watku zablokwanym na tej metodzie.
	 */
	public String getNextMessage() throws InterruptedException {
		return toViewQueue.take();
	}
}
