package com.hedera.cartevm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CodeGenerator {

  public static long HARNESS_OVERHEAD_ONE_TIME = 32;
  public static long HARNESS_OVERHEAD_EACH_LOOP = 51;

  static final String yulTemplate =
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
  final long gasLimit;
  final int sizeLimit;

  public CodeGenerator(final List<Step> steps, final long gasLimit, final int sizeLimit) {
    this.steps = steps;
    this.gasLimit = gasLimit;
    this.sizeLimit = sizeLimit;
  }

  String getName() {
    return steps.stream().map(Step::getName).collect(Collectors.joining("_"));
  }

  public String generate(String template) {
    StringBuffer inner = new StringBuffer();

    List<Step> backwardsSteps = new ArrayList<>(steps);
    Collections.reverse(backwardsSteps);
    int overheadSize =
        200
            + steps.stream()
                .mapToInt(s -> s.globalSetupCode.length() + s.globalCleanupCode.length())
                .sum();
    int iterationSize =
        steps.stream()
                .mapToInt(
                    s ->
                        s.localSetupCode.length()
                            + s.executionCode.length()
                            + s.globalCleanupCode.length())
                .sum()
            * 2;
    int iterationCount = iterationSize == 0 ? 1 : (sizeLimit - overheadSize) / iterationSize;

    long setupGas = HARNESS_OVERHEAD_ONE_TIME + steps.stream().mapToLong(s -> s.gasCostFirst).sum();
    long gasForLoops = gasLimit - setupGas;
    long gasPerIteration = (steps.stream().mapToLong(s -> s.gasCost).sum());

    long iterationGas = HARNESS_OVERHEAD_EACH_LOOP + gasPerIteration * iterationCount;
    while (iterationGas > gasForLoops && iterationCount > 1) {
      iterationCount /= 2;
      iterationGas = gasPerIteration * iterationCount;
    }
    long totalLoops = gasForLoops / iterationGas;
    System.out.printf(
        "unroll: %d\ttotal loops: %d\tgasLoop: %d\tleftoverGas: %d\tgas/iter: %d\tgas for loop: %d\t",
        iterationCount,
        totalLoops,
        iterationGas,
        gasLimit - setupGas - iterationGas,
        gasPerIteration,
        gasForLoops);

    for (int i = 0; i < iterationCount; i++) {
      // weaving setup doesn't work well with dup and swap
      // switch (i % 2) {
      //   case 0 ->
      steps.forEach(
          step -> {
            inner.append(step.localSetupCode);
            inner.append(step.executionCode);
            inner.append(step.localCleanupCode);
          });
      //   case 1 -> {
      //     backwardsSteps.forEach(step -> inner.append(step.localSetupCode));
      //     steps.forEach(
      //         step -> {
      //           inner.append(step.executionCode);
      //           inner.append(step.localCleanupCode);
      //         });
      //     }
      // }
    }

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
        getName(),
        totalLoops,
        globalSetup.isEmpty() ? "" : "verbatim_0i_0o(hex\"" + globalSetup + "\")",
        inner.isEmpty() ? "" : "verbatim_0i_0o(hex\"" + inner + "\")",
        globalCleanup.isEmpty() ? "" : "verbatim_0i_0o(hex\"" + globalCleanup + "\")",
        gasLimit);
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
        }
        // no output found :(
        // display error output
        try (var br = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
          for (String line; (line = br.readLine()) != null; ) {
            System.out.println(line);
          }
        }
        return null;
      } finally {
        p.destroy();
      }
    } catch (IOException ioe) {
      return "FE"; // invalid opcode
    }
  }
}
