package io.synthesia.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
  public void sign_whenInvalidBody_returns400(SignRequestDTO requestDTO) {
    givenRequestBody(requestDTO);

    this.sut.sign(this.context);

    assertBadRequest();
  }

  @Test
  public void sign_whenSignSucceeds_returnsSignedMessage() {
    givenRequestBody(new SignRequestDTO("message", "url"));

    whenSignSucceeds();

    this.sut.sign(this.context);

    assertSignedMessageReturned();
  }

  @Test
  public void sign_whenSignThrows_throwsException() {
    givenRequestBody(new SignRequestDTO("message", "url"));

    whenSignThrows();

    assertSignThrows();
  }

  @Test
  public void sign_whenClientSignatureMissingAndSigningSchedulingThrows_throwsException() {
    givenRequestBody(new SignRequestDTO("message", "url"));

    whenSignFails();
    whenScheduleMessageSigningThrows();

    assertSignThrows();
  }

  @Test
  public void sign_whenClientSignatureMissing_schedulesMessageSigningAndReturns202() {
    givenRequestBody(new SignRequestDTO("message", "url"));

    whenSignFails();

    this.sut.sign(this.context);

    assertMessageSigningScheduled();
    assertAccepted();
  }

  private void givenRequestBody(SignRequestDTO requestDTO) {
    when(this.context.bodyAsClass(any())).thenReturn(requestDTO);
  }

  private void whenSignSucceeds() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.of(SIGNED_MESSAGE));
  }

  private void whenSignFails() {
    when(this.cryptoClient.sign(any())).thenReturn(Optional.empty());
  }

  private void whenSignThrows() {
    when(this.cryptoClient.sign(any())).thenThrow(new RuntimeException());
  }

  private void whenScheduleMessageSigningThrows() {
    doThrow(new RuntimeException()).when(this.messageSigningQueue).scheduleMessageSigning(any());
  }

  private static Stream<SignRequestDTO> invalidRequestsArguments() {
    return Stream.of(null, new SignRequestDTO(null, null));
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
