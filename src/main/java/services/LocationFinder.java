package services;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.errors.ApiError;
import exceptions.ApiKeyException;
import utils.ConfigLoader;

public class LocationFinder {
    /**
     * Wrapper class around the location from Google Maps Service API
     * */
    public static class Location {
        private String country;
        private String city;

        public Location(String country, String city) {
            this.country = country;
            this.city = city;
        }

        @Override
        public String toString() {
            return String.format("%s, %s", city, country);
        }

        public String country() {
            return country;
        }

        public String city() {
            return city;
        }
    }

    /**
     * Checks if the given node contains the specified type in its "types" array.
     *
     * @param node the node to check
     * @param type the type to search for
     * @return true if the "types" array of the node contains the specified type, false otherwise
     */
    private static boolean containsType(JsonNode node, String type) {
        JsonNode typesNode = node.path("types");
        if (typesNode.isArray()) {
            return StreamSupport.stream(typesNode.spliterator(), false)
                    .anyMatch(n -> n.asText().equals(type));
        }
        return false;
    }

    /**
     * Retrieves the city and country associated with the given latitude and longitude using the Google Maps API.
     *
     * @param latitude the latitude of the location to retrieve
     * @param longitude the longitude of the location to retrieve
     * @return a Location record containing the country and city name associated with the given latitude and longitude, or null if the
     *         city and country names could not be found
     * @throws IOException if an error occurs while connecting to the Google Maps API
     * @throws ApiKeyException if Google Maps Service Api Key was not provided in config.properties
     */
    public static Location getLocation(double latitude, double longitude) throws IOException {
        String apiKey = ConfigLoader.getProperty(ConfigLoader.ConfigProperty.GOOGLE_MAPS_API_KEY);
        if (apiKey.isEmpty()) {
            throw new ApiKeyException(ConfigLoader.ConfigProperty.GOOGLE_MAPS_API_KEY
                    + " was not provided in config!");
        }
        // Construct the URL for the reverse geocoding API request
        String url = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                + latitude + "," + longitude
                + "&key=" + apiKey;

        // Connect to the Google Maps API and retrieve the response as a string
        URLConnection connection = new URL(url).openConnection();
        connection.connect();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(connection.getInputStream());

        String resultCity = "N/A";
        String resultCountry = "N/A";
        // Iterate over all results to find the first one that contains a locality and country
        JsonNode resultsNode = rootNode.path("results");
        for (JsonNode resultNode : resultsNode) {
            JsonNode addressComponentsNode = resultNode.path("address_components");
            String city = StreamSupport.stream(addressComponentsNode.spliterator(), false)
                    .filter(n -> containsType(n, "locality"))
                    .findFirst()
                    .map(n -> n.path("long_name").asText())
                    .orElse("");
            resultCity = !city.isEmpty() ? city : resultCity;
            String country = StreamSupport.stream(addressComponentsNode.spliterator(), false)
                    .filter(n -> containsType(n, "country"))
                    .findFirst()
                    .map(n -> n.path("long_name").asText())
                    .orElse("");
            resultCountry = !country.isEmpty() ? country : resultCountry;
        }

        // Return null if the city and country names could not be found
        return new Location(resultCountry, resultCity);
    }
}
