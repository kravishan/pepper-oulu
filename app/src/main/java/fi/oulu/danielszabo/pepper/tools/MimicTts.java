package fi.oulu.danielszabo.pepper.tools;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import fi.oulu.danielszabo.pepper.R;

public class MimicTts {
    private static String SERVER_ENDPOINT = "http://100.79.68.64:59125/api/tts?voice=en_US/vctk_low#p240";
    private static Boolean serverAvailable = null;

    public static void speak(String text) {
        // Send the TTS request asynchronously
        new TTSRequestTask().execute(text);
    }

    public static boolean isAvailable() {
        if (serverAvailable == null) {
            try {
                URL url = new URL(SERVER_ENDPOINT);

                // Open a connection to the server
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set up the request method and headers
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Send a test request
                String testText = "Test request";
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(testText);
                writer.flush();
                writer.close();

                // Check if the server responds
                int responseCode = connection.getResponseCode();
                Log.d("responseCode", "responseCode: " + responseCode);
                serverAvailable = (responseCode >= 200 && responseCode < 300);

                Log.d("Link worked", "API link works: " + serverAvailable);

                Log.d("MimicTts", "Mimic3 server is available: " + serverAvailable);
            } catch (IOException e) {
                // The server is not reachable
                serverAvailable = false;

                Log.e("MimicTts", "Mimic3 server is not available: " + e.getMessage());
            }
        }

        return serverAvailable;
    }


    private static class TTSRequestTask extends AsyncTask<String, Void, byte[]> {

        @Override
        protected byte[] doInBackground(String... params) {
            String textToSend = params[0];
            try {
                URL url = new URL(SERVER_ENDPOINT);

                // Open a connection to the server
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Set up the request method and headers
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Send the text to the server
                OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream());
                writer.write(textToSend);
                writer.flush();
                writer.close();

                // Log the request being sent
                Log.d("MimicTtsController", "Request sent: " + textToSend);

                // Get the response headers from the server
                Map<String, List<String>> responseHeaders = connection.getHeaderFields();
                if (responseHeaders.containsKey("Content-Type")) {
                    List<String> contentTypeValues = responseHeaders.get("Content-Type");
                    String contentType = contentTypeValues.get(0);
                    Log.d("MimicTtsController", "Received Content-Type: " + contentType);
                }

                // Get the response from the server
                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                byte[] audioData = readInputStream(inputStream);

                // Close the connection
                connection.disconnect();

                return audioData;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(byte[] audioData) {
            // Log the received audio response
            Log.d("MimicTtsController", "Received audio data");

            // Play the received audio using Pepper's speakers
            if (audioData != null) {
                playAudio(audioData);
            }
        }

        private byte[] readInputStream(InputStream inputStream) throws IOException {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while ((bytesRead = inputStream.read(buffer, 0, bufferSize)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return outputStream.toByteArray();
        }
    }

    private static void playAudio(byte[] audioData) {
        int audioSampleRate = 18000;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
        int channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        int bufferSize = AudioTrack.getMinBufferSize(audioSampleRate, channelConfig, audioFormat);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate, channelConfig, audioFormat, bufferSize, AudioTrack.MODE_STREAM);

        audioTrack.play();
        audioTrack.write(audioData, 0, audioData.length);
        audioTrack.stop();
        audioTrack.release();
    }
}
