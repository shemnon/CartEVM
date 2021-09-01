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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes;
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
    this.name = "Cartesian_" + steps.stream().map(Step::getName).collect(Collectors.joining("_"));
  }

  public String generateYul() {
    StringBuffer inner = new StringBuffer("verbatim_0i_0o(hex\"");
    steps.forEach(step -> inner.append(step.globalSetupCode));

    List<Step> backwardsSteps = new ArrayList<>(steps);
    Collections.reverse(backwardsSteps);
    for (int i = 0; i < unrolledLoopSize; i++) {
      switch (i % 2) {
        case 0 -> steps.forEach(
            step -> {
              inner.append(step.localSetupCode);
              inner.append(step.executionCode);
              inner.append(step.localCleanupCode);
            });
        case 1 -> {
          backwardsSteps.forEach(step -> inner.append(step.localSetupCode));
          steps.forEach(
              step -> {
                inner.append(step.executionCode);
                inner.append(step.localCleanupCode);
              });
        }
      }
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

  public String compileYul(String yulSource) throws IOException {
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
  }

  public void execute() throws IOException {
    String yul = generateYul();
    String bytecode = compileYul(yul);
    Bytes codeBytes = Bytes.fromHexString(bytecode);

    Address sender = Address.fromHexString("12345678");
    Address receiver = Address.fromHexString("9abcdef0");

    WorldUpdater worldUpdater = new SimpleWorld();
    worldUpdater.getOrCreate(sender).getMutable().setBalance(Wei.of(BigInteger.TWO.pow(20)));
    worldUpdater.getOrCreate(receiver).getMutable().setCode(codeBytes);

    final EVM evm = MainnetEvms.berlin();
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
            .address(receiver)
            .originator(sender)
            .sender(sender)
            .gasPrice(Wei.ZERO)
            .inputData(Bytes.EMPTY)
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
    long gasUsed = initialGas.minus(messageFrame.getRemainingGas()).toLong();
    long timeElapsedNanos = stopwatch.elapsed(TimeUnit.NANOSECONDS);
    System.out.printf(
        "%s\t%,d\t%,.3f\t%,d%n",
        name, gasUsed, timeElapsedNanos / 1000.0, gasUsed * 1_000_000_000 / timeElapsedNanos);
  }
}
