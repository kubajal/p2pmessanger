import javax.swing.*;
import java.awt.*;

/**
 * Komponent View wzorca MVC aplikacji.
 * Czesc aplikacja odpowiadajaca za interfejs graficzny aplikacji.
 * Wprowadzanie danych przez uzytkownika, wyswietlanie odbieranych wiadomosci oraz
 * komunikowanie bledow uzytkownikowi.
 * @author Kuba Jalowiec
 */
public class View{

	private JTextArea inputTextField, outputTextField, logTextField;
	private JSplitPane downUp, upLeftRight, downLeftRight;
	private JPanel upLeft, upRight, downLeft, downRight;
	private JMenuBar menuBar;
	private JFrame frame;
	private JMenu listenerMenu, menu;
	private JMenuItem startListeningButton, stopListeningButton, connectButton, disconnectButton;
	private JButton applyButton;

	/** 
	 * Tworzy kompletny interfejs graficzny.
	 */
	View(){
		
		try{
			SwingUtilities.invokeAndWait(new Runnable(){
				public void run(){
					frame = new JFrame();
					frame.setTitle("Komunikator");
					frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					frame.setMinimumSize(new Dimension(400, 500));
					
					menu = new JMenu("Plik");
					listenerMenu = new JMenu("Nasluchiwanie");
					startListeningButton = new JMenuItem("Nasluchuj");
					startListeningButton.setEnabled(true);
					stopListeningButton = new JMenuItem("Przestan nasluchiwac");
					stopListeningButton.setEnabled(false);
					connectButton = new JMenuItem("Polacz");
					connectButton.setEnabled(true);
					disconnectButton = new JMenuItem("Rozlacz");
					disconnectButton.setEnabled(false);
	
					menu.add(connectButton);
					menu.add(disconnectButton);
					
					menu.add(listenerMenu);
					listenerMenu.add(startListeningButton);
					listenerMenu.add(stopListeningButton);
					
					menuBar = new JMenuBar();
					menuBar.add(menu);
					frame.setJMenuBar(menuBar);
				
					inputTextField = new JTextArea();
					inputTextField.setLineWrap(true);
					outputTextField = new JTextArea();
					outputTextField.setLineWrap(true);
					logTextField = new JTextArea();
					logTextField.setLineWrap(true);
					
					upLeft = new JPanel(new BorderLayout());
					upRight = new JPanel(new BorderLayout());
					downLeft = new JPanel(new BorderLayout());
					downRight = new JPanel(new FlowLayout());
					upRight.add(outputTextField);
					outputTextField.setEditable(false);
					
					downLeft.add(inputTextField);
					applyButton = new JButton("Wyslij");
					applyButton.setEnabled(false);
					downRight.add(applyButton);
					
					upLeft.setMinimumSize(new Dimension(220, 100));
					upRight.setMinimumSize(new Dimension(300, 100));
					downLeft.setMinimumSize(new Dimension(300, 75));
					downRight.setMinimumSize(new Dimension(50, 75));
				
					upLeft.setPreferredSize(new Dimension(220, 300));
					upRight.setPreferredSize(new Dimension(300, 300));
					downLeft.setPreferredSize(new Dimension(300, 75));
					downRight.setPreferredSize(new Dimension(50, 50)); 
				
					downLeftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, downLeft, downRight);
					upLeftRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, upLeft, upRight);
					downUp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upLeftRight, downLeftRight);
				
					logTextField.setEnabled(false);
					upLeft.add(logTextField);
					
					downUp.setEnabled(false);
					frame.getContentPane().add(downUp);
					frame.pack();
				}
			});
		} catch(Exception e){
			JOptionPane.showMessageDialog(null, "Nie udalo sie utworzyc interfejsu dla uzytkownika. Sprobuj uruchomic aplikacje ponownie.");
		}
	}

	/** 
	 * Wyswietlenie wiadomosci w View.
	 * @param msg Wiadomosc do wyswietlenia w widoku.
	 */
	public void printReceivedMessage(String msg){
		
		outputTextField.append(msg);
	}
	
	/** 
	 * Zwraca referencje na glowna ramke programu.
	 * @return Glowna ramka programu.
	 */
	public JFrame getFrame(){
		return frame;
	}
	
	/** 
	 * Zwraca referencje pole tekstowe wyswietlajace log aplikacji.
	 * @return Log.
	 */
	public JTextArea getLogTextField(){
		return logTextField;
	}
	
	/** 
	 * Zwraca referencje na pole tekstowe sluzace uzytkownikowi do wpisywania wiadomosci do wyslania.
	 * @return Pole tekstowe sluzace za wejscie uzytkownika.
	 */
	public JTextArea getInputTextField(){
		return inputTextField;
	}

	/** 
	 * Zwraca element menu odpowiedzialny za rozlaczenia.
	 * @return Przycisk rozlaczenia.
	 */
	public JMenuItem getDisconnectButton(){
		return disconnectButton;
	}

	/** 
	 * Zwraca element menu odpowiedzialny za utworzenie nowego polaczenia.
	 * @return Przycisk polaczenia.
	 */
	public JMenuItem getConnectButton(){
		return connectButton;
	}

	/** 
	 * Zwraca element menu odpowiedzialny za rozpoczecie nasluchiwania na nadchodzce polaczenia.
	 * @return Przycisk rozpoczecia nasluchiwania.
	 */
	public JMenuItem getStartListeningButton(){
		return startListeningButton;
	}

	/** 
	 * Zwraca element menu odpowiedzialny za przestanie nasluchiwania.
	 * @return Przycisk przestania nasluchiwania.
	 */
	public JMenuItem getStopListeningButton(){
		return stopListeningButton;
	}
	
	/** 
	 * Zwraca przycisk odpowiedzialny za wyslanie wiadomosci.
	 * @return Przycisk wyslania wiadomosci.
	 */
	public JButton getApplyButton(){
		return applyButton;
	}
}