import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.swing.*;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Komponent Controller wzorca MVC aplikacji.
 * Czesc aplikacja odpowiadajaca za logike aplikacji. Przesylanie odebranych przez Model wiadomosci
 * do View oraz wpisanych przez uzytkownika w wiadomosci w View do Modelu, generalna obsluga zdarzen.
 * @author Kuba Jalowiec
 */
public class Controller {
	
	/** Referencja na View. */
	private View view;
	
	/** Referencja na Model. */
	private Model model;

	/** Watek wyswietlajacy w View wiadomosci odebrane przez socket w Modelu. */
	private Thread receivedMessagesThread;

	/** Tworzy Controller polaczony z podanym Modelem i podanym View.
	 * @param _model Referencja na Model, ktory ma byc kontrolowany przez Controller.
	 * @param _view Referencja na View, ktore ma byc kontrolowane przez Controller. */
	Controller(Model _model, View _view){
		
		view = _view;
		model = _model;
		setListeners();
		view.getFrame().setVisible(true);
		receivedMessagesThread = newReceivedMessagesThread();
		receivedMessagesThread.start();
		view.getLogTextField().setText(model.getConnectionLog());
	}

	/** 
	 * Nowy watek przesylania do View wiadomosci odebranych przez socket w Modelu.
	 * @return Nowy watek przesylania do View wiadomosci odebranych przez socket w Modelu.
	 * */
	private Thread newReceivedMessagesThread() {
		
		return new Thread(
				new Runnable(){
					public synchronized void run(){
						while(true){
							try {
								view.printReceivedMessage(model.getNextMessage());
							}
						catch(Exception e){
							model.updateLog("Koncze przekazywanie wiadomosci z Modelu do Widoku.");
						}
					}
				}
			}
		);
	}
	/** 
	 * Ustawianie sluchaczy zdarzen.
	 * */
	private void setListeners(){
		
		final JFrame frame = view.getFrame();
		final JMenuItem disconnectButton = view.getDisconnectButton();
		final JMenuItem connectButton = view.getConnectButton();
		final JMenuItem startListeningButton = view.getStartListeningButton();
		final JMenuItem stopListeningButton = view.getStopListeningButton();
		final JButton applyButton = view.getApplyButton();
		final JTextArea logTextField = view.getLogTextField();
		final JTextArea inputTextField = view.getInputTextField();
		
		/** 
		 * Obsluga zamkniecia aplikacji przez nacisniecie x w prawym gornym rogu.
		 * */
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		        if (JOptionPane.showConfirmDialog(frame, 
		            "Are you sure to close this window?", "Really Closing?", 
		            JOptionPane.YES_NO_OPTION,
		            JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION){
		        	model.closeConnection();
		            System.exit(0);
		        }
		    }
		});

		/** 
		 * Adaptacja widoku do nowego polaczenia.
		 * */
		model.changeSupport.addPropertyChangeListener("connection",
				new PropertyChangeListener(){
					public void propertyChange(PropertyChangeEvent event){
						if(event.getOldValue() == null){
							disconnectButton.setEnabled(true);
							connectButton.setEnabled(false);
							startListeningButton.setEnabled(false);
							stopListeningButton.setEnabled(false);
							applyButton.setEnabled(true);
						}
					}
				}
		);

		/** 
		 * Odswiezanie loga.
		 * */
		model.changeSupport.addPropertyChangeListener("log",
			new PropertyChangeListener(){
				public void propertyChange(PropertyChangeEvent event){
					logTextField.setText(model.getConnectionLog());
				}
			}
		);

		/** 
		 * Adaptacja widoku do zamkniecia polaczenia.
		 * */
		model.changeSupport.addPropertyChangeListener("connection",
				new PropertyChangeListener(){
					public void propertyChange(PropertyChangeEvent event){
						if(event.getNewValue() == null){
							disconnectButton.setEnabled(false);
							connectButton.setEnabled(true);
							startListeningButton.setEnabled(true);
							stopListeningButton.setEnabled(false);
							applyButton.setEnabled(false);
						}
					}
				}
			);

		/** 
		 * Adaptacja widoku rozlaczenia.
		 * */
		disconnectButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0){
				model.closeConnection();
				disconnectButton.setEnabled(false);
				connectButton.setEnabled(true);
				startListeningButton.setEnabled(true);
				stopListeningButton.setEnabled(false);
			}
		});

		/** 
		 * Adaptacja widoku do proby polaczenia - prosba o podanie adresu IP.
		 * */
		connectButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				String IP = JOptionPane.showInputDialog("Podaj adres");
				try {
					if(IP != null){
						model.newConnection(InetAddress.getByName(IP));
						disconnectButton.setEnabled(true);
						connectButton.setEnabled(false);
						startListeningButton.setEnabled(false);
						stopListeningButton.setEnabled(false);
					}
				}
				catch (IOException e) {
					JOptionPane.showMessageDialog(frame, "Nie udalo sie polaczyc do podanego adresu: " + IP);
					//e.printStackTrace();
				}
			}
		});

		/** 
		 * Adaptacja widoku do nasluchiwania na ServerSocket.
		 * */
		startListeningButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent arg0) {
				JOptionPane.showMessageDialog(frame, "Przechodze w tryb nasluchiwania polaczen.");
				disconnectButton.setEnabled(false);
				startListeningButton.setEnabled(false);
				stopListeningButton.setEnabled(true);
				connectButton.setEnabled(false);
				try {
					model.listen();
					stopListeningButton.setEnabled(true);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(null, "Wystapil blad podczas nasluchiwania.");
				}
			}
		});

		/** 
		 * Adaptacja widoku do zaprzestania nasluchiwania
		 * */
		stopListeningButton.addActionListener(new ActionListener(){
			public synchronized void actionPerformed(ActionEvent arg0) {
				model.stopListening();
				startListeningButton.setEnabled(true);
				connectButton.setEnabled(true);
				disconnectButton.setEnabled(false);
				stopListeningButton.setEnabled(false);
			}

		});

		/** 
		 * Obsluga z poziomu View wyslania wiadomosci - przekazanie jej do Modelu.
		 * */
		applyButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent arg0) {
				String message = inputTextField.getText();
				model.sendMessage(message);
				inputTextField.setText("");
			}
		});
	}
}