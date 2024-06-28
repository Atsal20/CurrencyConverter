import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class CurrencyConverter {
    private static final String API_KEY ="b85f6567c00dcdfcc6e3ab32";
    private static final String BASE_URL ="https://v6.exchangerate-api.com/v6/";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("¡Bienvenido al Conversor de Moneda!");
        System.out.println("Este programa le permitirá convertir monedas entre diferentes países.");

        boolean continuar = true;
        while (continuar) {
            System.out.println("Seleccione una opción:");
            System.out.println("1. EUR a USD");
            System.out.println("2. EUR a MXN");
            System.out.println("3. USD a EUR");
            System.out.println("4. USD a MXN");
            System.out.println("5. MXN a EUR");
            System.out.println("6. MXN a USD");
            System.out.println("7. Otro");
            System.out.println("0. Salir");
            System.out.print("Opción: ");
            int opcion = scanner.nextInt();
            String fromCurrency = "";
            String toCurrency = "";
            double amount;
            switch (opcion) {
                case 1:
                    fromCurrency = "EUR";
                    toCurrency = "USD";
                    break;
                case 2:
                    fromCurrency = "EUR";
                    toCurrency = "MXN";
                    break;
                case 3:
                    fromCurrency = "USD";
                    toCurrency = "EUR";
                    break;
                case 4:
                    fromCurrency = "USD";
                    toCurrency = "MXN";
                    break;
                case 5:
                    fromCurrency = "MXN";
                    toCurrency = "EUR";
                    break;
                case 6:
                    fromCurrency = "MXN";
                    toCurrency = "USD";
                    break;
                case 7:
                    System.out.println("Ingrese el nombre del primer país:");
                    String fromCountry = scanner.next();
                    System.out.println("Ingrese el nombre del segundo país:");
                    String toCountry = scanner.next();
                    try {
                        fromCurrency = getCurrencyCodeFromAPI(fromCountry);
                        toCurrency = getCurrencyCodeFromAPI(toCountry);
                    } catch (Exception e) {
                        System.out.println("Error al obtener la moneda: " + e.getMessage());
                        continue;
                    }
                    break;
                case 0:
                    System.out.println("Gracias por usar nuestra API\n");
                    continuar = false;
                    break;
                default:
                    System.out.println("Opción no válida.");
                    continue;

            }
            System.out.println("Ingrese la cantidad a convertir:");
            amount = scanner.nextDouble();

            try {
                double convertedAmount = convertCurrency(fromCurrency, toCurrency, amount);
                String formattedResult = String.format("%.2f", convertedAmount);
                System.out.println(amount + " " + fromCurrency + " = " + formattedResult + " " + toCurrency);
            } catch (Exception e) {
                System.out.println("Error al realizar la conversión: " + e.getMessage());
            }

            System.out.println("\n¿Desea realizar otra conversión? (S/N):");
            String respuesta = scanner.nextLine().toUpperCase();
            continuar = respuesta.equals("S");
        }
        System.out.println("Programa finalizado. Gracias por usar la API!");
        scanner.close();

    }
    public static double convertCurrency(String fromCurrency, String toCurrency, double amount) throws Exception {
        String url = BASE_URL + API_KEY + "/latest/" + fromCurrency;

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get response from the API: " + response.body());
        }

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        JsonObject conversionRates = jsonResponse.getAsJsonObject("conversion_rates");

        if (!conversionRates.has(toCurrency)) {
            throw new RuntimeException("Invalid currency code: " + toCurrency);
        }

        double rate = conversionRates.get(toCurrency).getAsDouble();
        return amount * rate;
    }
    private static String getCurrencyCodeFromAPI(String countryName) throws Exception {
        String url = "https://restcountries.com/v3.1/name/" + countryName;
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get response from the API: " + response.body());
        }
        Gson gson = new Gson();
        JsonElement jsonResponse = gson.fromJson(response.body(), JsonElement.class);
        if (jsonResponse.isJsonArray()) {
            JsonArray jsonArray = jsonResponse.getAsJsonArray();
            if (jsonArray.size() == 0) {
                throw new RuntimeException("No se encontró información para el país: " + countryName);
            }
            JsonObject firstResult = jsonArray.get(0).getAsJsonObject();
            JsonObject currencyObj = firstResult.getAsJsonObject("currencies");
            String currencyCode = currencyObj.keySet().iterator().next();
            return currencyCode;
        } else {
            throw new RuntimeException("Respuesta inesperada de la API: " + jsonResponse);
        }
    }

}
