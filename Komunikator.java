/**
 * Klasa opakowujaca komponenty Model, View i Controller.
 * @author Kuba Jalowiec
 */

public class Komunikator{
	
	/**
	 * Tworzy nowy Model, View i Controller. Uruchamia aplikacje.
	 * @param args Argumenty wywolania (ignorowane).
	 */
	public static void main(String[] args) {
		
		Model model = new Model();
		View widok = new View();
		new Controller(model, widok);
	}
}