package com.hedera.cartevm;

/*-
 * ‌
 * CartEVM
 * ​
 * Copyright (C) 2021 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 */

import com.google.common.base.Stopwatch;
import com.hedera.cartevm.besu.SimpleBlockValues;
import com.hedera.cartevm.besu.SimpleWorld;
import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.hyperledger.besu.datatypes.Address;
import org.hyperledger.besu.datatypes.Hash;
import org.hyperledger.besu.datatypes.Wei;
import org.hyperledger.besu.evm.Code;
import org.hyperledger.besu.evm.EVM;
import org.hyperledger.besu.evm.Gas;
import org.hyperledger.besu.evm.MainnetEVMs;
import org.hyperledger.besu.evm.account.MutableAccount;
import org.hyperledger.besu.evm.frame.MessageFrame;
import org.hyperledger.besu.evm.gascalculator.GasCalculator;
import org.hyperledger.besu.evm.gascalculator.LondonGasCalculator;
import org.hyperledger.besu.evm.internal.EvmConfiguration;
import org.hyperledger.besu.evm.precompile.MainnetPrecompiledContracts;
import org.hyperledger.besu.evm.precompile.PrecompileContractRegistry;
import org.hyperledger.besu.evm.processor.ContractCreationProcessor;
import org.hyperledger.besu.evm.processor.MessageCallProcessor;
import org.hyperledger.besu.evm.tracing.OperationTracer;
import org.hyperledger.besu.evm.worldstate.WorldUpdater;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hedera.cartevm.Step.RETURN_CONTRACT_ADDRESS;
import static com.hedera.cartevm.Step.REVERT_CONTRACT_ADDRESS;

public class LocalRunner extends CodeGenerator {

	static final Address SENDER = Address.fromHexString("12345678");
	static final Address RECEIVER = Address.fromHexString("9abcdef0");
	static final Map<String, String> bytecodeCache = new HashMap<>();

	public LocalRunner(List<Step> steps, long gasLimit, int sizeLimit) {
		super(steps, gasLimit, sizeLimit);
	}

	public void prexistingState(WorldUpdater worldUpdater, Bytes codeBytes) {
		worldUpdater.getOrCreate(SENDER).getMutable().setBalance(Wei.of(BigInteger.TWO.pow(20)));

		MutableAccount receiver = worldUpdater.getOrCreate(RECEIVER).getMutable();
		receiver.setCode(codeBytes);
		// for sload
		receiver.setStorageValue(UInt256.fromHexString("54"), UInt256.fromHexString("99"));

		MutableAccount otherAccount =
				worldUpdater.getOrCreate(Address.fromHexString(RETURN_CONTRACT_ADDRESS)).getMutable();
		// for balance
		otherAccount.setBalance(Wei.fromHexString("0x0ba1a9ce0ba1a9ce"));
		// for extcode*, returndata*, and call*
		otherAccount.setCode(Bytes.fromHexString("0x3360005260206000f3"));

		MutableAccount revert =
				worldUpdater.getOrCreate(Address.fromHexString(REVERT_CONTRACT_ADDRESS)).getMutable();
		revert.setBalance(Wei.fromHexString("0x0ba1a9ce0ba1a9ce"));
		// for REVERT
		revert.setCode(Bytes.fromHexString("0x6055605555604360a052600160a0FD"));
	}

	public void execute(boolean verbose) {
		String yul = generate(yulTemplate);
		String bytecode = bytecodeCache.computeIfAbsent(yul, this::compileYul);
		Bytes codeBytes = Bytes.fromHexString(bytecode);

		WorldUpdater worldUpdater = new SimpleWorld();
		prexistingState(worldUpdater, codeBytes);

		// final EVM evm = MainnetEvms.london();
		GasCalculator londonGasCalculator = new LondonGasCalculator();
		final EVM evm = MainnetEVMs.constantinople(londonGasCalculator, EvmConfiguration.DEFAULT);
		final PrecompileContractRegistry precompileContractRegistry = new PrecompileContractRegistry();
		MainnetPrecompiledContracts.populateForIstanbul(
				precompileContractRegistry, londonGasCalculator);
		final Stopwatch stopwatch = Stopwatch.createUnstarted();
		final Deque<MessageFrame> messageFrameStack = new ArrayDeque<>();
		final Gas initialGas = Gas.of(gasLimit);
		MessageFrame initialMessageFrame =
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
						.code(new Code(codeBytes, Hash.hash(codeBytes)))
						.blockValues(new SimpleBlockValues())
						.depth(0)
						.completer(c -> {
						})
						.miningBeneficiary(Address.ZERO)
						.blockHashLookup(h -> null)
						.build();
		messageFrameStack.add(initialMessageFrame);

		final MessageCallProcessor mcp = new MessageCallProcessor(evm, precompileContractRegistry);
		final ContractCreationProcessor ccp =
				new ContractCreationProcessor(londonGasCalculator, evm, true, List.of(), 0);
		stopwatch.start();
		OperationTracer tracer = OperationTracer.NO_TRACING;
		while (!messageFrameStack.isEmpty()) {
			MessageFrame messageFrame = messageFrameStack.peek();
			switch (messageFrame.getType()) {
				case MESSAGE_CALL -> mcp.process(messageFrame, tracer);
				case CONTRACT_CREATION -> ccp.process(messageFrame, tracer);
			}
		}
		stopwatch.stop();
		initialMessageFrame.getRevertReason().ifPresent(b -> System.out.println("Reverted - " + b));
		long gasUsed = initialGas.minus(initialMessageFrame.getRemainingGas()).toLong();
		long timeElapsedNanos = stopwatch.elapsed(TimeUnit.NANOSECONDS);
		if (verbose) {
			System.out.printf(
					"%s\t%s\t%,d\t%,.3f\t%,.0f\t%s%n",
					getName().replace("__", "\t"),
					initialMessageFrame
							.getExceptionalHaltReason()
							.map(Object::toString)
							.orElse(initialMessageFrame.getState().toString()),
					gasUsed,
					timeElapsedNanos / 1000.0,
					gasUsed * 1_000_000_000.0 / timeElapsedNanos,
					initialMessageFrame.getRevertReason().orElse(Bytes.EMPTY).toUnprefixedHexString());
		}
	}
}
