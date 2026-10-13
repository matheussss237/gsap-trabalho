package br.com.projeto.view.components;

import javafx.scene.layout.Region;

/**
 * Ícones vetoriais (SVG path) desenhados como Region + -fx-shape.
 * A cor vem do CSS (.icon { -fx-background-color }), então acompanham o tema.
 */
public final class Icons {

    public static final String PLUS = "M11 5h2v14h-2zM5 11h14v2H5z";
    public static final String SEND = "M2 21l21-9L2 3v7l15 2-15 2z";
    public static final String TRASH = "M6 7h12l-1 14H7L6 7zm3-4h6l1 2h4v2H4V5h4l1-2z";
    public static final String EDIT = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04a1 1 0 0 0 0-1.41"
            + "l-2.34-2.34a1 1 0 0 0-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String LOGOUT = "M10 17l1.4-1.4-2.6-2.6H20v-2H8.8l2.6-2.6L10 7l-5 5 5 5zM4 3h8v2H4v14h8v2H4"
            + "a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z";
    public static final String SUN = "M12 7a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM11 1h2v3h-2zM11 20h2v3h-2zM1 11h3v2H1z"
            + "M20 11h3v2h-3zM4.2 5.6l1.4-1.4 2.1 2.1-1.4 1.4zM16.3 17.7l1.4-1.4 2.1 2.1-1.4 1.4zM4.2 18.4l2.1-2.1"
            + " 1.4 1.4-2.1 2.1zM16.3 6.3l2.1-2.1 1.4 1.4-2.1 2.1z";
    public static final String MOON = "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z";
    public static final String SETTINGS = "M3 17h6v2H3zM3 5h10v2H3zM13 21v-2h8v-2h-8v-2h-2v6zM7 9v2H3v2h4v2h2V9z"
            + "M21 13v-2H11v2zM15 9h2V7h4V5h-4V3h-2z";
    public static final String BOOK = "M4 4.5A2.5 2.5 0 0 1 6.5 2H20v17H6.5a1.5 1.5 0 0 0 0 3H20v2H6.5A3.5 3.5 0 0 1"
            + " 3 20.5v-16zM7 6v2h9V6z";
    public static final String MENU = "M3 6h18v2H3zM3 11h18v2H3zM3 16h18v2H3z";
    public static final String COPY = "M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11"
            + "a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z";
    public static final String SEARCH = "M15.5 14h-.8l-.3-.3A6.5 6.5 0 1 0 14 15.5l.3.3v.8l5 5 1.5-1.5-5-5zm-6 0"
            + "A4.5 4.5 0 1 1 14 9.5 4.5 4.5 0 0 1 9.5 14z";
    public static final String CHAT = "M4 3h16a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H8l-5 4V5a2 2 0 0 1 2-2z";
    public static final String SPARK = "M12 2l2.2 6.8L21 11l-6.8 2.2L12 20l-2.2-6.8L3 11l6.8-2.2z";
    public static final String USER = "M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-5 0-9 2.5-9 5v3h18v-3c0-2.5-4-5-9-5z";
    public static final String CLOSE = "M19 6.4L17.6 5 12 10.6 6.4 5 5 6.4 10.6 12 5 17.6 6.4 19 12 13.4 17.6 19"
            + " 19 17.6 13.4 12z";
    public static final String CHECK = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z";
    public static final String DOC = "M6 2h8l6 6v14H4V2h2zm7 1.5V9h5.5L13 3.5z";
    public static final String ALERT = "M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z";

    private Icons() {
    }

    public static Region create(String svgPath, double size) {
        Region icon = new Region();
        icon.getStyleClass().add("icon");
        icon.setStyle("-fx-shape: \"" + svgPath + "\";");
        icon.setMinSize(size, size);
        icon.setPrefSize(size, size);
        icon.setMaxSize(size, size);
        return icon;
    }
}
