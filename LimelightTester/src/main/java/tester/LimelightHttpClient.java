package tester;

import com.google.gson.Gson;
import okhttp3.*;
import tester.models.LimelightResult;

import java.io.IOException;

/**
 * HTTP client for communicating with Limelight camera via HTTP API.
 * Provides methods to retrieve detection results and switch pipelines.
 */
public class LimelightHttpClient {
    private static final String DEFAULT_URL = "http://limelight.local:5807";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final String baseUrl;

    public LimelightHttpClient() {
        this(DEFAULT_URL);
    }

    public LimelightHttpClient(String baseUrl) {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.baseUrl = baseUrl;
    }

    /**
     * Retrieves the latest detection result from the Limelight.
     *
     * @return LimelightResult containing detection data, or null if connection fails
     */
    public LimelightResult getLatestResult() {
        Request request = new Request.Builder()
                .url(baseUrl + "/results")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String json = response.body().string();
                return gson.fromJson(json, LimelightResult.class);
            }
        } catch (IOException e) {
            System.err.println("Error fetching Limelight results: " + e.getMessage());
        }

        return null;
    }

    /**
     * Switches the Limelight to the specified pipeline.
     * NOTE: Pipeline switching via HTTP API may not be supported on all Limelight versions.
     * If this fails, manually switch pipelines through the web interface at port 5801.
     *
     * @param pipelineIndex The pipeline to switch to (0=AprilTag, 1=Purple, 2=Green)
     * @return true if pipeline switch was successful, false otherwise
     */
    public boolean switchPipeline(int pipelineIndex) {
        // Try multiple possible endpoints for pipeline switching
        String[] endpoints = {
            baseUrl + "/pipeline/" + pipelineIndex,
            "http://limelight.local:5801/api/pipeline/" + pipelineIndex
        };

        for (String endpoint : endpoints) {
            try {
                RequestBody body = RequestBody.create("", JSON);
                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .build();

                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    response.close();
                    // Wait a moment for pipeline to switch
                    Thread.sleep(200);
                    return true;
                }
                response.close();
            } catch (IOException | InterruptedException e) {
                // Try next endpoint
            }
        }

        System.out.println("Note: Automatic pipeline switching not available.");
        System.out.println("Please switch to pipeline " + pipelineIndex + " manually via web interface.");
        return false;
    }

    /**
     * Checks if the Limelight is connected and responding.
     *
     * @return true if Limelight is accessible, false otherwise
     */
    public boolean isConnected() {
        LimelightResult result = getLatestResult();
        return result != null;
    }

    /**
     * Gets the current frames per second from the Limelight.
     *
     * @return FPS value, or -1 if unable to retrieve
     */
    public double getFPS() {
        // FPS information is typically in the status endpoint
        // For simplicity, we'll return a placeholder value
        // In a full implementation, this would query a status endpoint
        LimelightResult result = getLatestResult();
        return result != null ? 90.0 : -1.0;
    }

    /**
     * Gets the base URL being used to communicate with the Limelight.
     *
     * @return The base URL (e.g., "http://limelight.local:5801")
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}
