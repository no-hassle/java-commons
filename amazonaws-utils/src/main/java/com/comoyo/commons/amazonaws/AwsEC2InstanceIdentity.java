package com.comoyo.commons.amazonaws;

import com.google.common.base.Optional;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Collections;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class AwsEC2InstanceIdentity
{
    private static final String INSTANCE_DATA_URL
        = "http://169.254.169.254/latest/dynamic/instance-identity/document";
    private static final int CONNECT_TIMEOUT_MS = 250;

    private static AtomicReference<Optional<Map<String, String>>> metadataRef;

    /**
     * Find AWS EC2 instance metadata for running host.
     *
     * @return optionally, a map of [key, value] pairs describing the
     * hosting EC2 instance.
     */
    public static Optional<Map<String, String>> getMetadata()
    {
        final Optional<Map<String, String>> metadata = metadataRef.get();
        if (metadata != null) {
            return metadata;
        }
        if (hypervisorAbsent()) {
            metadataRef.set(Optional.<Map<String, String>>absent());
            return metadataRef.get();
        }

        final ObjectMapper mapper = new ObjectMapper();
        try {
            final URL url = new URL(INSTANCE_DATA_URL);
            final URLConnection connection = url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.connect();
            final Map<String, String> response
                = mapper.readValue(
                    connection.getInputStream(),
                    new TypeReference<Map<String, String>>() {});
            metadataRef.set(Optional.fromNullable(
                                response != null
                                ? Collections.unmodifiableMap(response)
                                : null));
        }
        catch (IOException e) {
            metadataRef.set(Optional.<Map<String, String>>absent());
        }
        return metadataRef.get();
    }

    /**
     * Best-effort function to determine if a hypervisor is absent.
     *
     * @return false if unable to positively determine that no
     * hypervisor is present.
     */
    private static boolean hypervisorAbsent()
    {
        try {
            final BufferedReader cpuinfo
                = Files.newBufferedReader(
                    Paths.get("/proc/cpuinfo"), StandardCharsets.UTF_8);
            String line;
            while ((line = cpuinfo.readLine()) != null) {
                if (line.startsWith("flags")) {
                    return !line.contains(" hypervisor");
                }
            }
            return false;
        }
        catch (IOException e) {
            return false;
        }
    }
}
