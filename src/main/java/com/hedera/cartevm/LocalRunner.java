package com.hedera.cartevm;

import com.google.common.base.Stopwatch;
import com.hedera.cartevm.besu.SimpleBlockHeader;
import com.hedera.cartevm.besu.SimpleWorld;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.Code;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.Gas;
import org.hyperledger.besu.evm.MainnetEvms;
import org.hyperledger.besu.evm.MainnetPrecompiledContracts;
import org.hyperledger.besu.evm.MessageCallProcessor;
import org.hyperledger.besu.evm.MessageFrame;
import org.hyperledger.besu.evm.OperationTracer;
import org.hyperledger.besu.evm.PrecompileContractRegistry;
import org.hyperledger.besu.evm.WorldUpdater;

public class LocalRunner {

  static final Address SENDER = Address.fromHexString("12345678");
  static final Address RECEIVER = Address.fromHexString("9abcdef0");
  static final Map<String, String> bytecodeCache = new HashMap<>();
  private static final String template =
      """
        {
          // %1$s
          %3$s
          for { let i := 0 } lt(i, %2$s) { i := add(i, 1) } {
            %4$s
          }
          %5$s
        }
        """;

  final List<Step> steps;
  final int unrolledLoopSize;
  final int outerLoopSize;
  final long gasLimit;
  private final String name;

  public LocalRunner(List<Step> steps, int unrolledLoopSize, int outerLoopSize, long gasLimit) {
    this.steps = steps;
    this.unrolledLoopSize = unrolledLoopSize;
    this.outerLoopSize = outerLoopSize;
    this.gasLimit = gasLimit;
    this.name = steps.stream().map(Step::getName).collect(Collectors.joining("_"));
  }

  public String generateYul() {
    StringBuffer inner = new StringBuffer("verbatim_0i_0o(hex\"");
    List<Step> backwardsSteps = new ArrayList<>(steps);
    Collections.reverse(backwardsSteps);
    for (int i = 0; i < unrolledLoopSize; i++) {
      //      switch (i % 2) {
      //        case 0 ->
      steps.forEach(
          step -> {
            inner.append(step.localSetupCode);
            inner.append(step.executionCode);
            inner.append(step.localCleanupCode);
          });
      //        case 1 -> {
      //          backwardsSteps.forEach(step -> inner.append(step.localSetupCode));
      //          steps.forEach(
      //              step -> {
      //                inner.append(step.executionCode);
      //                inner.append(step.localCleanupCode);
      //              });
      //        }
      //      }
    }
    inner.append("\")");

    String globalSetup =
        steps.stream()
            .map(Step::getGlobalSetupCode)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining());
    String globalCleanup =
        steps.stream()
            .map(Step::getGlobalCleanupCode)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining());
    return template.formatted(
        name,
        outerLoopSize,
        globalSetup.isEmpty() ? "" : "verbatim_0i_0o(hex\"" + globalSetup + "\")",
        inner,
        globalCleanup.isEmpty() ? "" : "verbatim_0i_0o(hex\"" + globalCleanup + "\")");
  }

  public String compileYul(String yulSource) {
    try {
      ProcessBuilder pb = new ProcessBuilder().command("solc", "--assemble", "-");
      Process p = pb.start();
      try {
        p.getOutputStream().write(yulSource.getBytes(StandardCharsets.UTF_8));
        p.getOutputStream().close();

        try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
          for (String line; (line = br.readLine()) != null; ) {
            if ("Binary representation:".equals(line)) {
              return br.readLine();
            }
          }
          // no output found :(
          return null;
        }
      } finally {
        p.destroy();
      }
    } catch (IOException ioe) {
      return "FE"; // invalid opcode
    }
  }

  public void prexistingState(WorldUpdater worldUpdater, Bytes codeBytes) {
    worldUpdater.getOrCreate(SENDER).getMutable().setBalance(Wei.of(BigInteger.TWO.pow(20)));
    worldUpdater.getOrCreate(RECEIVER).getMutable().setCode(codeBytes);

    // for balance
    worldUpdater
        .getOrCreate(Address.fromHexString("0xa94f5374fce5edbc8e2a8697c15331677e6ebf0b"))
        .getMutable()
        .setBalance(Wei.fromHexString("0x0ba1a9ce0ba1a9ce"));
    // for extcode*
    worldUpdater
        .getOrCreate(Address.fromHexString("0xe713449c212d891357cc2966816b1d528cfb59e0"))
        .getMutable()
        .setCode(Bytes.fromHexString("0x600c606355600b606355600a60635500"));
    // for returndata
    worldUpdater
        .getOrCreate(Address.fromHexString("0xa94f5374fce5edbc8e2a8697c15331677e6ebf0b"))
        .getMutable()
        .setCode(Bytes.fromHexString("0x3360005260206000f3"));
    // for sload
    worldUpdater
        .getOrCreate(RECEIVER)
        .getMutable()
        .setStorageValue(UInt256.fromHexString("54"), UInt256.fromHexString("99"));
  }

  public void execute(boolean verbose) {
    String yul = generateYul();
    String bytecode = bytecodeCache.computeIfAbsent(yul, this::compileYul);
    Bytes codeBytes = Bytes.fromHexString(bytecode);

    WorldUpdater worldUpdater = new SimpleWorld();
    prexistingState(worldUpdater, codeBytes);

    //    final EVM evm = MainnetEvms.berlin();
    final EVM evm = MainnetEvms.london();
    final PrecompileContractRegistry precompileContractRegistry = new PrecompileContractRegistry();
    MainnetPrecompiledContracts.populateForIstanbul(
        precompileContractRegistry, evm.getGasCalculator());
    final Stopwatch stopwatch = Stopwatch.createUnstarted();
    final Deque<MessageFrame> messageFrameStack = new ArrayDeque<>();
    final Gas initialGas = Gas.of(gasLimit);
    MessageFrame messageFrame =
        MessageFrame.builder()
            .type(MessageFrame.Type.MESSAGE_CALL)
            .messageFrameStack(messageFrameStack)
            .worldUpdater(worldUpdater.updater())
            .initialGas(initialGas)
            .contract(Address.ZERO)
            .address(RECEIVER)
            .originator(SENDER)
            .sender(SENDER)
            .gasPrice(Wei.ZERO)
            .inputData(
                Bytes.fromHexString(
                    "a9059cbb"
                        + "0000000000000000000000004bbeeb066ed09b7aed07bf39eee0460dfa261520"
                        + "00000000000000000000000000000000000000000000000002a34892d36d6c74"))
            .value(Wei.ZERO)
            .apparentValue(Wei.ZERO)
            .code(new Code(codeBytes))
            .blockHeader(new SimpleBlockHeader())
            .depth(0)
            .completer(c -> {})
            .miningBeneficiary(Address.ZERO)
            .blockHashLookup(h -> null)
            .build();
    messageFrameStack.add(messageFrame);

    final MessageCallProcessor mcp = new MessageCallProcessor(evm, precompileContractRegistry);
    stopwatch.start();
    OperationTracer tracer = OperationTracer.NO_TRACING;
    while (!messageFrameStack.isEmpty()) {
      mcp.process(messageFrameStack.peek(), tracer);
    }
    stopwatch.stop();
    messageFrame.getRevertReason().ifPresent(b -> System.out.println("Reverted - " + b));
    long gasUsed = initialGas.minus(messageFrame.getRemainingGas()).toLong();
    long timeElapsedNanos = stopwatch.elapsed(TimeUnit.NANOSECONDS);
    if (verbose) {
      System.out.printf(
          "%s\t%s\t%,d\t%,.3f\t%,.0f\t%s%n",
          name,
          messageFrame
              .getExceptionalHaltReason()
              .map(Enum::toString)
              .orElse(messageFrame.getState().toString()),
          gasUsed,
          timeElapsedNanos / 1000.0,
          gasUsed * 1_000_000_000.0 / timeElapsedNanos,
          messageFrame.getRevertReason().orElse(Bytes.EMPTY).toUnprefixedHexString());
    }
  }
}
