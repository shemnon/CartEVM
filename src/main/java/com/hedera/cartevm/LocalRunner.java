package com.hedera.cartevm;

import com.google.common.base.Stopwatch;
import com.hedera.cartevm.besu.SimpleBlockHeader;
import com.hedera.cartevm.besu.SimpleWorld;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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

public class LocalRunner extends CodeGenerator {

  static final Address SENDER = Address.fromHexString("12345678");
  static final Address RECEIVER = Address.fromHexString("9abcdef0");
  static final Map<String, String> bytecodeCache = new HashMap<>();

  public LocalRunner(List<Step> steps, long gasLimit, int sizeLimit) {
    super(steps, gasLimit, sizeLimit);
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
    String yul = generate(yulTemplate);
    String bytecode = bytecodeCache.computeIfAbsent(yul, this::compileYul);
    Bytes codeBytes = Bytes.fromHexString(bytecode);

    WorldUpdater worldUpdater = new SimpleWorld();
    prexistingState(worldUpdater, codeBytes);

    // final EVM evm = MainnetEvms.london();
    final EVM evm = MainnetEvms.petersburg();
    final PrecompileContractRegistry precompileContractRegistry = new PrecompileContractRegistry();
    MainnetPrecompiledContracts.populateForIstanbul(
        precompileContractRegistry, evm.getGasCalculator());
    final Stopwatch stopwatch = Stopwatch.createUnstarted();
    final Deque<MessageFrame> messageFrameStack = new ArrayDeque<>();
    final Gas initialGas = Gas.of(gasLimit * 2);
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
          getName(),
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
