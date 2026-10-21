package com.matheushenrique.lumi;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Classe principal (rode esta no IntelliJ).
 * O Spring Boot cuida dos services e repositories (MongoDB);
 * o JavaFX (classe Main) cuida das telas e liga o Spring durante a animação de abertura.
 */
@SpringBootApplication
public class LumiApplication {

	public static void main(String[] args) {
		Application.launch(Main.class, args);
	}

}
