import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class CurrencyConverter {

    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String CONFIG_FILE_PATH = "config.properties";

    // Instancia global de HttpClient para reutilización
    private static final HttpClient httpClient = HttpClient.newBuilder()
           .version(HttpClient.Version.HTTP_2)
           .connectTimeout(Duration.ofSeconds(30))
           .build();

    // Caché para almacenar los códigos de moneda
    private static final Map<String, Set<String>> cache = new HashMap<>();
    private static long lastUpdate = System.currentTimeMillis();
    private static final long CACHE_UPDATE_INTERVAL = 60 * 1000; // 1 minuto

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("¡Bienvenido al Conversor de Moneda!");

        boolean continuar = true;
        while (continuar) {
            printOptions();
            int opcion = scanner.nextInt();
            scanner.nextLine();
            if (!isValidInput(opcion)) {
                System.out.println("Entrada inválida. Por favor, seleccione una opción válida.");
                continue;
            }
            ConversionOption option = ConversionOption.values()[opcion];

            try {
                if (option == ConversionOption.EXIT) {
                    continuar = false;
                    break;
                } else if (option == ConversionOption.CUSTOM) {
                    handleCustomOption(scanner);
                } else {
                    while (true) {
                        System.out.print("Ingrese la cantidad a convertir de " + option.getFromCurrency() + " a " + option.getToCurrency() + ": ");
                        String amountStr = scanner.nextLine();
                        if (!isNumeric(amountStr)) {
                            System.out.println("Favor de digitar una cantidad numérica");
                        } else {
                            double amount = Double.parseDouble(amountStr);
                            double convertedAmount = convertCurrency(option.getFromCurrency(), option.getToCurrency(), amount);
                            System.out.printf("%.2f %s = %.2f %s%n", amount, option.getFromCurrency(), convertedAmount, option.getToCurrency());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            System.out.println("\n¿Desea realizar otra conversión? (S/N):");
            String respuesta = scanner.nextLine().toUpperCase();
            continuar = respuesta.equals("S");
        }
        System.out.println("Programa finalizado. Gracias por usar la API!");
        scanner.close();
    }

    private static void printOptions() {
        System.out.println("Seleccione una opción:");
        for (ConversionOption option : ConversionOption.values()) {
            System.out.println(option.ordinal() + ". " + option.name());
        }
        System.out.print("Opción: ");
    }

    private static boolean isValidInput(int opcion) {
        return opcion >= 0 && opcion < ConversionOption.values().length;
    }

    private static boolean isNumeric(String str){
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static void handleCustomOption(Scanner scanner) throws Exception {
        System.out.println("Lista de todas las monedas disponibles:");
        Set<String> currencies = getAllCurrencies();
        for (String currency : currencies) {
            System.out.println(currency);
        }

        String fromCurrency;
        String toCurrency;
        while (true) {
            System.out.print("Ingrese la moneda de origen: ");
            fromCurrency = scanner.nextLine().toUpperCase();
            System.out.print("Ingrese la moneda de destino: ");
            toCurrency = scanner.nextLine().toUpperCase();
            if (!currencies.contains(fromCurrency) || !currencies.contains(toCurrency)) {
                System.out.println("Moneda no encontrada. Intente de nuevo.");
            } else {
                break;
            }
        }

        while (true) {
            System.out.print("Ingrese la cantidad a convertir: ");
            String amountStr = scanner.nextLine();
            if (!isNumeric(amountStr)) {
                System.out.println("Favor de digitar una cantidad numérica.");
            } else {
                double amount = Double.parseDouble(amountStr);
                double convertedAmount = convertCurrency(fromCurrency, toCurrency, amount);
                System.out.printf("%.2f %s = %.2f %s%n", amount, fromCurrency, convertedAmount, toCurrency);
                break;
            }
        }
    }

    public static double convertCurrency(String fromCurrency, String toCurrency, double amount) throws Exception {
        String url = BASE_URL + readApiKey() + "/latest/" + fromCurrency;
        HttpRequest request = HttpRequest.newBuilder()
               .uri(URI.create(url))
               .GET()
               .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode()!= 200) {
            throw new CustomException("Failed to get response from the API: " + response.body());
        }

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        JsonObject conversionRates = jsonResponse.getAsJsonObject("conversion_rates");

        if (!conversionRates.has(toCurrency)) {
            throw new CustomException("Invalid currency code: " + toCurrency);
        }

        double rate = conversionRates.get(toCurrency).getAsDouble();
        return amount * rate;
    }

    private static Set<String> getAllCurrencies() throws Exception {
        // Verificar si los datos están en caché y son recientes
        if (cache.containsKey("allCurrencies") && System.currentTimeMillis() - lastUpdate < CACHE_UPDATE_INTERVAL) {
            return cache.get("allCurrencies");
        }

        String url = BASE_URL + readApiKey() + "/codes";
        HttpRequest request = HttpRequest.newBuilder()
               .uri(URI.create(url))
               .GET()
               .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode()!= 200) {
            throw new CustomException("Failed to get response from the API: " + response.body());
        }

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        JsonArray supportedCodes = jsonResponse.getAsJsonArray("supported_codes");

        Set<String> currencies = new HashSet<>();
        for (JsonElement element : supportedCodes) {
            JsonArray currencyArray = element.getAsJsonArray();
            currencies.add(currencyArray.get(0).getAsString());
        }

        // Almacenar los datos en caché
        cache.put("allCurrencies", currencies);
        lastUpdate = System.currentTimeMillis();

        return currencies;
    }

    private enum ConversionOption {
        EUR_TO_USD("EUR", "USD"),
        EUR_TO_MXN("EUR", "MXN"),
        USD_TO_EUR("USD", "EUR"),
        USD_TO_MXN("USD", "MXN"),
        MXN_TO_EUR("MXN", "EUR"),
        MXN_TO_USD("MXN", "USD"),
        CUSTOM("Personalizada", "Personalizada"),
        EXIT("", "");

        private final String fromCurrency;
        private final String toCurrency;

        ConversionOption(String fromCurrency, String toCurrency) {
            this.fromCurrency = fromCurrency;
            this.toCurrency = toCurrency;
        }

        public String getFromCurrency() {
            return fromCurrency;
        }

        public String getToCurrency() {
            return toCurrency;
        }
    }

    static class CustomException extends Exception {
        public CustomException(String message) {
            super(message);
        }
    }

    private static String readApiKey() {
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE_PATH)) {
            properties.load(input);
            return properties.getProperty("api_key");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
