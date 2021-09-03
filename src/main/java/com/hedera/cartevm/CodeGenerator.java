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
  final int unrolledLoopSize;
  final int outerLoopSize;
  final long gasLimit;

  public CodeGenerator(
      final List<Step> steps,
      final int unrolledLoopSize,
      final int outerLoopSize,
      final long gasLimit) {
    this.steps = steps;
    this.unrolledLoopSize = unrolledLoopSize;
    this.outerLoopSize = outerLoopSize;
    this.gasLimit = gasLimit;
  }

  String getName() {
    return steps.stream().map(Step::getName).collect(Collectors.joining("_"));
  }

  public String generate(String template) {
    StringBuffer inner = new StringBuffer("verbatim_0i_0o(hex\"");

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
        getName(),
        outerLoopSize,
        globalSetup.isEmpty() ? "" : "verbatim_0i_0o(hex\"" + globalSetup + "\")",
        inner,
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
}
