package io.synthesia.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.javalin.http.Context;
import io.synthesia.api.dto.SignRequestDTO;
import io.synthesia.async.MessageSigningQueue;
import io.synthesia.crypto.CryptoClient;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SignApiTest {
  private static String SIGNED_MESSAGE = "signed";

  private static SignRequestDTO VALID_SIGN_REQUEST =
      new SignRequestDTO("message", "https://url.com");

  @Mock private Context context;

  @Mock private CryptoClient cryptoClient;

  @Mock private MessageSigningQueue messageSigningQueue;

  private SignApi sut;

  @BeforeEach
  public void beforeEach() {
    MockitoAnnotations.initMocks(this);

    this.sut = new SignApi(this.cryptoClient, this.messageSigningQueue);
  }

  @ParameterizedTest
  @MethodSource("invalidRequestsArguments")
  void sign_whenInvalidBody_returns400(SignRequestDTO requestDTO) {
    givenRequestBody(requestDTO);

    this.sut.sign(this.context);

    assertBadRequest();
  }

  @Test
  void sign_whenSignSucceeds_returnsSignedMessage() {
    givenRequestBody(VALID_SIGN_REQUEST);

    whenSignSucceeds();

    this.sut.sign(this.context);

    assertSignedMessageReturned();
  }

  @Test
  void sign_whenSignThrows_throwsException() {
    givenRequestBody(VALID_SIGN_REQUEST);

    whenSignThrows();

    assertSignThrows();
  }

  @Test
  void sign_whenClientSignatureMissingAndSigningSchedulingThrows_throwsException() {
    givenRequestBody(VALID_SIGN_REQUEST);

    whenSignFails();
    whenScheduleMessageSigningThrows();

    assertSignThrows();
  }

  @Test
  void sign_whenClientSignatureMissing_schedulesMessageSigningAndReturns202() {
    givenRequestBody(VALID_SIGN_REQUEST);

    whenSignFails();

    this.sut.sign(this.context);

    assertMessageSigningScheduled();
    assertAccepted();
  }

  private void givenRequestBody(SignRequestDTO requestDTO) {
    when(this.context.queryParam(eq("message"))).thenReturn(requestDTO.getMessage());
    when(this.context.queryParam(eq("webhookUrl"))).thenReturn(requestDTO.getWebhookUrl());
  }

  private void whenSignSucceeds() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.of(SIGNED_MESSAGE));
  }

  private void whenSignFails() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.empty());
  }

  private void whenSignThrows() {
    when(this.cryptoClient.sign(any())).thenThrow(RuntimeException.class);
  }

  private void whenScheduleMessageSigningThrows() {
    doThrow(new RuntimeException()).when(this.messageSigningQueue).scheduleMessageSigning(any());
  }

  private static Stream<SignRequestDTO> invalidRequestsArguments() {
    return Stream.of(new SignRequestDTO(null, null), new SignRequestDTO("message", "not a URL"));
  }

  private void assertBadRequest() {
    verify(this.context, times(1)).status(400);
  }

  private void assertAccepted() {
    verify(this.context, times(1)).status(202);
  }

  private void assertSignedMessageReturned() {
    verify(this.context, times(1)).result(SIGNED_MESSAGE);
  }

  private void assertMessageSigningScheduled() {
    verify(this.messageSigningQueue, times(1)).scheduleMessageSigning(any());
  }

  private void assertSignThrows() {
    Assertions.assertThrows(Exception.class, () -> this.sut.sign(this.context));
  }
}
