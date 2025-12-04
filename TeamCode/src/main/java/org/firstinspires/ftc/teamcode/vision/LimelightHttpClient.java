package org.firstinspires.ftc.teamcode.vision;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for communicating with Limelight via JSON API.
 * Used when Limelight is connected to a laptop or other network-accessible device
 * instead of directly to the Robot Controller Hub.
 *
 * API Endpoint: http://limelight.local:5807/results
 */
public class LimelightHttpClient {

    private static final String DEFAULT_HOST = "limelight.local";
    private static final int DEFAULT_PORT = 5807;
    private static final int TIMEOUT_MS = 2000;

    private final String baseUrl;

    /**
     * Creates HTTP client with default Limelight address (limelight.local:5807)
     */
    public LimelightHttpClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
     * Creates HTTP client with custom address
     *
     * @param host Limelight hostname or IP
     * @param port Limelight JSON API port (usually 5807)
     */
    public LimelightHttpClient(String host, int port) {
        this.baseUrl = "http://" + host + ":" + port;
    }

    /**
     * Result container for Limelight detections
     */
    public static class LimelightResult {
        public boolean valid;
        public List<FiducialDetection> fiducials;
        public List<ColorDetection> colors;
        public int currentPipeline;

        public LimelightResult() {
            this.valid = false;
            this.fiducials = new ArrayList<>();
            this.colors = new ArrayList<>();
            this.currentPipeline = 0;
        }
    }

    /**
     * AprilTag/Fiducial detection data
     */
    public static class FiducialDetection {
        public int fiducialId;
        public double tx;  // Horizontal offset from crosshair (degrees)
        public double ty;  // Vertical offset from crosshair (degrees)
        public double ta;  // Target area (0-100%)

        public FiducialDetection(int id, double tx, double ty, double ta) {
            this.fiducialId = id;
            this.tx = tx;
            this.ty = ty;
            this.ta = ta;
        }
    }

    /**
     * Color detection data
     */
    public static class ColorDetection {
        public double tx;
        public double ty;
        public double ta;

        public ColorDetection(double tx, double ty, double ta) {
            this.tx = tx;
            this.ty = ty;
            this.ta = ta;
        }
    }

    /**
     * Fetches latest Limelight results via HTTP
     *
     * @return LimelightResult with detection data, or invalid result if connection fails
     */
    public LimelightResult getResults() {
        LimelightResult result = new LimelightResult();

        try {
            URL url = new URL(baseUrl + "/results");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                return result;  // Invalid result
            }

            // Read response
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON
            JSONObject json = new JSONObject(response.toString());
            result.valid = json.optBoolean("v", false);

            // Get current pipeline
            result.currentPipeline = json.optInt("pID", 0);

            // Parse fiducial results (AprilTags)
            if (json.has("Fiducial")) {
                JSONArray fiducials = json.getJSONArray("Fiducial");
                for (int i = 0; i < fiducials.length(); i++) {
                    JSONObject fid = fiducials.getJSONObject(i);
                    int id = fid.optInt("fID", -1);
                    double tx = fid.optDouble("tx", 0.0);
                    double ty = fid.optDouble("ty", 0.0);
                    double ta = fid.optDouble("ta", 0.0);

                    if (id >= 0) {
                        result.fiducials.add(new FiducialDetection(id, tx, ty, ta));
                    }
                }
            }

            // Parse color detector results
            if (json.has("Detector")) {
                JSONArray detectors = json.getJSONArray("Detector");
                for (int i = 0; i < detectors.length(); i++) {
                    JSONObject det = detectors.getJSONObject(i);
                    double tx = det.optDouble("tx", 0.0);
                    double ty = det.optDouble("ty", 0.0);
                    double ta = det.optDouble("ta", 0.0);

                    result.colors.add(new ColorDetection(tx, ty, ta));
                }
            }

        } catch (Exception e) {
            // Connection failed - return invalid result
            result.valid = false;
        }

        return result;
    }

    /**
     * Switches to a specific pipeline
     *
     * @param pipelineIndex Pipeline number (0-9)
     * @return true if switch successful, false otherwise
     */
    public boolean switchPipeline(int pipelineIndex) {
        try {
            URL url = new URL(baseUrl + "/settings");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            // Send pipeline switch request
            JSONObject payload = new JSONObject();
            payload.put("pipeline", pipelineIndex);

            OutputStream os = conn.getOutputStream();
            os.write(payload.toString().getBytes());
            os.flush();
            os.close();

            int responseCode = conn.getResponseCode();
            return responseCode == 200;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tests if Limelight is reachable via HTTP
     *
     * @return true if connection successful, false otherwise
     */
    public boolean testConnection() {
        try {
            URL url = new URL(baseUrl + "/results");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            int responseCode = conn.getResponseCode();
            conn.disconnect();

            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
