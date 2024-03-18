import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class CrptApi {
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Semaphore semaphore;
    private final AtomicInteger requestCount;
    private final long requestLimitInterval;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        httpClient = HttpClient.newHttpClient();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        semaphore = new Semaphore(requestLimit, true);
        requestCount = new AtomicInteger(0);
        requestLimitInterval = timeUnit.toMillis(1);
    }

    public void createDocument(DocumentDTO documentDTO, String signature) throws URISyntaxException, JsonProcessingException, IOException, InterruptedException {
        acquirePermit();
        try {
            String jsonObject = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentDTO);
            System.out.println(jsonObject);
            HttpResponse<String> response = sendRequest(jsonObject);
            handleResponse(response);
        } finally {
            releasePermit();
        }
    }

    private void acquirePermit() throws InterruptedException {
        semaphore.acquire();
        if (requestCount.incrementAndGet() > semaphore.availablePermits()) {
            semaphore.release();
            Thread.sleep(requestLimitInterval);
            requestCount.set(0);
            acquirePermit();
        }
    }

    private void releasePermit() {
        semaphore.release();
    }

    private HttpResponse<String> sendRequest(String jsonObject) throws URISyntaxException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void handleResponse(HttpResponse<String> response) {
        System.out.println(response.statusCode());
        // Обработка ответа
    }

    // Геттеры реализованы для адекватной работы Jackson.
    class DocumentDTO {
        private Description description;
        private String docID;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private LocalDate productionDate;
        private String productionType;
        private List<Product> products;
        private LocalDate regDate;
        private String regNumber;

        public Description getDescription() {
            return description;
        }

        public String getDocID() {
            return docID;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public boolean isImportRequest() {
            return importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public String getProductionType() {
            return productionType;
        }

        public List<Product> getProducts() {
            return products;
        }

        public LocalDate getRegDate() {
            return regDate;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public DocumentDTO(
                Description description,
                String docID,
                String docStatus,
                String docType,
                boolean importRequest,
                String ownerInn,
                String participantInn,
                String producerInn,
                LocalDate productionDate,
                String productionType,
                List<Product> products,
                LocalDate regDate,
                String regNumber
        ) {
            this.description = description;
            this.docID = docID;
            this.docStatus = docStatus;
            this.docType = docType;
            this.importRequest = importRequest;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.products = products;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }
    }

    class Description {
        private String participantInn;

        public String getParticipantInn() {
            return participantInn;
        }

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    class Product {
        private String certificateDocument;
        private LocalDate certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private LocalDate productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        public String getCertificateDocument() {
            return certificateDocument;
        }

        public LocalDate getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public LocalDate getProductionDate() {
            return productionDate;
        }

        public String getTnvedCode() {
            return tnvedCode;
        }

        public String getUitCode() {
            return uitCode;
        }

        public String getUituCode() {
            return uituCode;
        }

        public Product(
                String certificateDocument,
                LocalDate certificateDocumentDate,
                String certificateDocumentNumber,
                String ownerInn,
                String producerInn,
                LocalDate productionDate,
                String tnvedCode,
                String uitCode,
                String uituCode
        ) {
            this.certificateDocument = certificateDocument;
            this.certificateDocumentDate = certificateDocumentDate;
            this.certificateDocumentNumber = certificateDocumentNumber;
            this.ownerInn = ownerInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.tnvedCode = tnvedCode;
            this.uitCode = uitCode;
            this.uituCode = uituCode;
        }
    }

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {
        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5);

        Description description = api.new Description("123456789012");

        List<Product> products = new ArrayList<>();
        products.add(api.new Product(
                "CertificateDocument1",
                LocalDate.of(2022, 5, 15),
                "CertDocNum1234",
                "987654321098",
                "765432109876",
                LocalDate.of(2022, 3, 20),
                "1234567890",
                "UITCODE1",
                "UITUCODE1"
        ));
        products.add(api.new Product(
                "CertificateDocument2",
                LocalDate.of(2021, 11, 1),
                "CertDocNum5678",
                "543216789012",
                "321098765432",
                LocalDate.of(2021, 9, 25),
                "9876543210",
                "UITCODE2",
                "UITUCODE2"
        ));

        DocumentDTO documentDTO = api.new DocumentDTO(
                description,
                "DOC123",
                "Registered",
                "ImportDocument",
                true,
                "123456789012",
                "987654321098",
                "765432109876",
                LocalDate.of(2022, 4, 10),
                "ImportType",
                products,
                LocalDate.of(2022, 4, 15),
                "RegNum456789"
        );

        String signature = "13151345314513";

        api.createDocument(documentDTO, signature);
    }
}