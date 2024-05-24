
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class TicketAnalyzer {
    public static void main(String[] args) {
        File file = new File("tickets.json");
        List<Map<String, Object>> tickets = readTickets(file);

        if (tickets == null) {
            System.err.println("Ошибка чтения заявок из файла..");
            return;
        }

        List<Map<String, Object>> filteredTickets = tickets.stream()
                .filter(t -> t.get("origin").equals("VVO") && t.get("destination").equals("TLV"))
                .collect(Collectors.toList());

        Map<String, Long> minFlightTimes = calculateMinFlightTimes(filteredTickets);
        System.out.println("Минимальное время полета для каждого перевозчика:");
        minFlightTimes.forEach((carrier, time) -> System.out.println(carrier + ": " + time));

        double averagePrice = calculateAveragePrice(filteredTickets);
        double medianPrice = calculateMedianPrice(filteredTickets);
        double difference = Math.abs(averagePrice - medianPrice);

        System.out.println("Average price: " + averagePrice);
        System.out.println("Median price: " + medianPrice);
        System.out.println("Разница между средней и медианной ценой: " + difference);
    }

    private static List<Map<String, Object>> readTickets(File file) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(file).get("tickets");
            List<Map<String, Object>> tickets = new ArrayList<>();
            if (root != null) {
                for (JsonNode node : root) {
                    Map<String, Object> ticket = new HashMap<>();
                    ticket.put("origin", node.get("origin").asText());
                    ticket.put("destination", node.get("destination").asText());
                    ticket.put("departureTime", LocalTime.parse(node.get("departure_time").asText()));
                    ticket.put("arrivalTime", LocalTime.parse(node.get("arrival_time").asText()));
                    ticket.put("carrier", node.get("carrier").asText());
                    ticket.put("price", node.get("price").asInt());
                    tickets.add(ticket);
                }
            }
            return tickets;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Map<String, Long> calculateMinFlightTimes(List<Map<String, Object>> tickets) {
        Map<String, Long> minTimes = new HashMap<>();

        for (Map<String, Object> ticket : tickets) {
            LocalTime departureTime = (LocalTime) ticket.get("departureTime");
            LocalTime arrivalTime = (LocalTime) ticket.get("arrivalTime");
            long duration = Duration.between(departureTime, arrivalTime).toMinutes();
            String carrier = (String) ticket.get("carrier");

            minTimes.merge(carrier, duration, Math::min);
        }

        return minTimes;
    }

    private static double calculateAveragePrice(List<Map<String, Object>> tickets) {
        return tickets.stream().mapToInt(t -> (int) t.get("price")).average().orElse(0);
    }

    private static double calculateMedianPrice(List<Map<String, Object>> tickets) {
        List<Integer> prices = tickets.stream().map(t -> (int) t.get("price")).sorted().collect(Collectors.toList());
        int size = prices.size();
        if (size % 2 == 0) {
            return (prices.get(size / 2 - 1) + prices.get(size / 2)) / 2.0;
        } else {
            return prices.get(size / 2);
        }
    }
}
