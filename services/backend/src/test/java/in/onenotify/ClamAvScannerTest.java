package in.onenotify;

import static org.junit.jupiter.api.Assertions.*;

import in.onenotify.documents.ClamAvMalwareScanner;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class ClamAvScannerTest {
  private boolean scanWith(String response) throws Exception {
    try (var server = new ServerSocket(0)) {
      var peer =
          CompletableFuture.runAsync(
              () -> {
                try (var client = server.accept()) {
                  var in = new DataInputStream(client.getInputStream());
                  assertEquals(
                      "zINSTREAM\0", new String(in.readNBytes(10), StandardCharsets.US_ASCII));
                  var body = new ByteArrayOutputStream();
                  for (int size; (size = in.readInt()) != 0; ) body.write(in.readNBytes(size));
                  assertEquals("sample", body.toString(StandardCharsets.US_ASCII));
                  client.getOutputStream().write(response.getBytes(StandardCharsets.US_ASCII));
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
      boolean accepted =
          new ClamAvMalwareScanner("127.0.0.1", server.getLocalPort(), 1000)
              .scan("sample".getBytes())
              .accepted();
      peer.get(3, TimeUnit.SECONDS);
      return accepted;
    }
  }

  @Test
  void acceptsOnlyCompleteCleanResponse() throws Exception {
    assertTrue(scanWith("stream: OK\0"));
  }

  @Test
  void rejectsMalware() throws Exception {
    assertFalse(scanWith("stream: Win.Test.EICAR FOUND\0"));
  }

  @Test
  void rejectsErrorsAndTruncatedResponses() throws Exception {
    assertFalse(scanWith("INSTREAM size limit exceeded. ERROR\0"));
    assertFalse(scanWith("stream: OK"));
  }

  @Test
  void failsClosedWhenUnavailable() throws Exception {
    int port;
    try (var server = new ServerSocket(0)) {
      port = server.getLocalPort();
    }
    assertFalse(
        new ClamAvMalwareScanner("127.0.0.1", port, 100).scan("sample".getBytes()).accepted());
  }

  @Test
  void deadlineAlsoBoundsBlockedWrites() throws Exception {
    try (var server = new ServerSocket(0)) {
      server.setReceiveBufferSize(1024);
      var peer =
          CompletableFuture.runAsync(
              () -> {
                try (var socket = server.accept()) {
                  Thread.sleep(700);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              });
      assertTimeoutPreemptively(
          java.time.Duration.ofSeconds(2),
          () ->
              assertFalse(
                  new ClamAvMalwareScanner("127.0.0.1", server.getLocalPort(), 100)
                      .scan(new byte[10 * 1024 * 1024])
                      .accepted()));
      peer.get(3, TimeUnit.SECONDS);
    }
  }
}
