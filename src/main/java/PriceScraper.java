public interface PriceScraper {
    /**
     * Haalt de prijs op van de gegeven URL.
     * @return De geformatteerde prijs (bijv. "€ 10,00") of "Niet leverbaar".
     */
    String fetchPrice(String url);

    /**
     * Controleert of deze scraper geschikt is voor de domeinnaam in de URL.
     */
    boolean supports(String url);
}