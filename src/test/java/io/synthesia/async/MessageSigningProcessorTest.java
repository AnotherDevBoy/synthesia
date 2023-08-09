package io.synthesia.async;

public class MessageSigningProcessorTest {
  // TODO: when no message available -> doesn't call sign
  // TODO: when message available but sign does not have result -> doesn't call webhook
  // TODO: when message available but sign throws -> doesn't call webhook
  // TODO: when message available, sign succeeds but notify throws -> doesn't acknowledge message
  // TODO: when message available, sign succeeds, notify succeeds -> Acknowledges message
}
