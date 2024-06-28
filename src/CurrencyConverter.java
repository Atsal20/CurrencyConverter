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
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

public class CurrencyConverter {

    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private static final String COUNTRIES_API_URL = "https://restcountries.com/v3.1";
    private static final String CONFIG_FILE_PATH = "config.properties";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("¡Bienvenido al Conversor de Moneda!");

        boolean continuar = true;
        while (continuar) {
            printOptions();
            int opcion = scanner.nextInt();
            scanner.nextLine();  // Consume the newline
            if (!isValidInput(opcion)) {
                System.out.println("Entrada inválida. Por favor, seleccione una opción válida.");
                continue;
            }
            ConversionOption option = ConversionOption.values()[opcion];

            try {
                if (option == ConversionOption.EXIT) {
                    continuar = false;
                    System.out.println("Gracias por usar el Conversor de Moneda. ¡Adiós!");
                    break;
                } else if (option == ConversionOption.CUSTOM) {
                    handleCustomOption(scanner);
                } else {
                    System.out.print("Ingrese la cantidad a convertir de " + option.getFromCurrency() + " a " + option.getToCurrency() + ": ");
                    double amount = Double.parseDouble(scanner.nextLine());
                    double convertedAmount = convertCurrency(option.getFromCurrency(), option.getToCurrency(), amount);
                    System.out.printf("%.2f %s = %.2f %s%n", amount, option.getFromCurrency(), convertedAmount, option.getToCurrency());
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
            if (option != ConversionOption.EXIT) {
                System.out.println(option.ordinal() + ". " + option.name());
            }
        }
        System.out.print("Opción: ");
    }

    private static boolean isValidInput(int opcion) {
        return opcion >= 0 && opcion < ConversionOption.values().length;
    }

    private static void handleCustomOption(Scanner scanner) throws Exception {
        System.out.println("Lista de todas las monedas disponibles:");
        Set<String> currencies = getAllCurrencies();
        for (String currency : currencies) {
            System.out.println(currency);
        }

        System.out.print("Ingrese la moneda de origen: ");
        String fromCurrency = scanner.nextLine().toUpperCase();
        System.out.print("Ingrese la moneda de destino: ");
        String toCurrency = scanner.nextLine().toUpperCase();
        System.out.print("Ingrese la cantidad a convertir: ");
        double amount = Double.parseDouble(scanner.nextLine());
        double convertedAmount = convertCurrency(fromCurrency, toCurrency, amount);
        System.out.printf("%.2f %s = %.2f %s%n", amount, fromCurrency, convertedAmount, toCurrency);
    }

    public static double convertCurrency(String fromCurrency, String toCurrency, double amount) throws Exception {
        String url = BASE_URL + readApiKey() + "/latest/" + fromCurrency;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
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

    private static String getCurrencyCodeFromAPI(String countryCode) throws Exception {
        String url = COUNTRIES_API_URL + "/name/" + countryCode;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new CustomException("Failed to get response from the API: " + response.body());
        }

        Gson gson = new Gson();
        JsonElement jsonResponse = gson.fromJson(response.body(), JsonElement.class);

        if (jsonResponse.isJsonArray()) {
            JsonArray jsonArray = jsonResponse.getAsJsonArray();
            if (jsonArray.size() == 0) {
                throw new CustomException("No se encontró información para el país: " + countryCode);
            }
            JsonObject firstResult = jsonArray.get(0).getAsJsonObject();
            JsonObject currencyObj = firstResult.getAsJsonObject("currencies");
            String currencyCode = currencyObj.keySet().iterator().next();
            return currencyCode;
        } else {
            throw new CustomException("Respuesta inesperada de la API: " + jsonResponse);
        }
    }

    private static Set<String> getAllCurrencies() throws Exception {
        String url = BASE_URL + readApiKey() + "/codes";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new CustomException("Failed to get response from the API: " + response.body());
        }

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        JsonArray supportedCodes = jsonResponse.getAsJsonArray("supported_codes");

        Set<String> currencies = new java.util.HashSet<>();
        for (JsonElement element : supportedCodes) {
            JsonArray currencyArray = element.getAsJsonArray();
            currencies.add(currencyArray.get(0).getAsString());
        }
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
        EXIT("Salir", "");

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
